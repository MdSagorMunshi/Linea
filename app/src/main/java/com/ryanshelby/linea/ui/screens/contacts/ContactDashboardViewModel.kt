package com.ryanshelby.linea.ui.screens.contacts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.data.local.dao.CallNoteDao
import com.ryanshelby.linea.data.local.dao.CallRecordDao
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.entities.CallNoteEntity
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import com.ryanshelby.linea.telecom.CallManager
import com.ryanshelby.linea.telecom.PhoneAccountManager
import com.ryanshelby.linea.telecom.logic.AvailabilityInsight
import com.ryanshelby.linea.telecom.logic.AvailabilityInsightEngine
import com.ryanshelby.linea.data.repository.ContactSyncRepository
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ContactDashboardState(
    val contact: ContactEntity? = null,
    val numbers: List<ContactNumberEntity> = emptyList(),
    val callHistory: List<CallRecordEntity> = emptyList(),
    val preCallNote: CallNoteEntity? = null,
    val availabilityInsight: AvailabilityInsight? = null,
    val lastInteractionSummary: String = "No logged interactions",
    val totalCalls: Int = 0,
    val totalDurationSeconds: Long = 0,
    val preferredSimSlot: Int? = null,
    val isFavorite: Boolean = false,
    val isPrivate: Boolean = false,
    val alwaysRing: Boolean = false,
    val allowRestrictedHours: Boolean = false,
    val customRingtone: String = "Default Linea Ringtone",
    val isLoading: Boolean = true
)

@HiltViewModel
class ContactDashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactDao: ContactDao,
    private val callRecordDao: CallRecordDao,
    private val callNoteDao: CallNoteDao,
    private val callManager: CallManager,
    private val phoneAccountManager: PhoneAccountManager,
    private val contactSyncRepository: ContactSyncRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val contactId: Long = savedStateHandle.get<Long>("contactId") ?: 0L

    private val _state = MutableStateFlow(ContactDashboardState())
    val state: StateFlow<ContactDashboardState> = _state.asStateFlow()

    init {
        loadContactData()
    }

    fun loadContactData(overrideContactId: Long? = null) {
        val targetId = if (overrideContactId != null && overrideContactId > 0) overrideContactId else contactId
        if (targetId <= 0) return

        viewModelScope.launch {
            val contact = contactDao.getContactById(targetId) ?: return@launch
            var numbers = contactDao.getNumbersForContact(targetId).first()

            if (numbers.isEmpty() && !contact.isPrivate && contact.androidContactId != null && contact.androidContactId > 0) {
                val fetchedNumbers = mutableListOf<ContactNumberEntity>()
                try {
                    val cursor = context.contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(
                            android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                            android.provider.ContactsContract.CommonDataKinds.Phone.TYPE
                        ),
                        "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contact.androidContactId.toString()),
                        null
                    )
                    cursor?.use {
                        val numIdx = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val typeIdx = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.TYPE)
                        while (it.moveToNext()) {
                            val numStr = if (numIdx >= 0) it.getString(numIdx) ?: "" else ""
                            val typeInt = if (typeIdx >= 0) it.getInt(typeIdx) else 0
                            val label = when (typeInt) {
                                android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                                android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                                else -> "Mobile"
                            }
                            if (numStr.isNotBlank() && fetchedNumbers.none { fn -> fn.number == numStr }) {
                                fetchedNumbers.add(
                                    ContactNumberEntity(
                                        contactId = targetId,
                                        number = numStr,
                                        normalizedNumber = numStr.filter { ch -> ch.isDigit() || ch == '+' },
                                        label = label,
                                        isPrimary = fetchedNumbers.isEmpty()
                                    )
                                )
                            }
                        }
                    }
                    if (fetchedNumbers.isNotEmpty()) {
                        contactDao.insertNumbers(fetchedNumbers)
                        numbers = fetchedNumbers
                    }
                } catch (_: Exception) {}
            }

            // Fetch call records for all numbers belonging to this contact
            val allRecords = mutableListOf<CallRecordEntity>()
            for (num in numbers) {
                val recordsForNum = callRecordDao.getCallRecordsForNumberOnce(num.number)
                allRecords.addAll(recordsForNum)
                val normalizedRecords = callRecordDao.getCallRecordsForNumberOnce(num.normalizedNumber)
                allRecords.addAll(normalizedRecords)
            }
            val distinctRecords = allRecords.distinctBy { it.id }.sortedByDescending { it.timestamp }

            // Pre-call note
            val primaryNum = numbers.firstOrNull()?.number ?: ""
            val activeNote = callNoteDao.getActivePreCallNote(targetId, primaryNum)

            // Local Availability Insight (100% on-device heuristic)
            val insight = AvailabilityInsightEngine.computeInsight(distinctRecords)

            // Last interaction summary
            val lastCall = distinctRecords.firstOrNull()
            val lastSummary = if (lastCall != null) {
                val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(lastCall.timestamp))
                val dir = lastCall.callType.name.lowercase().replaceFirstChar { it.uppercase() }
                "$dir call on $dateStr (${lastCall.durationSeconds}s)"
            } else {
                "No previous calls recorded"
            }

            _state.value = ContactDashboardState(
                contact = contact,
                numbers = numbers,
                callHistory = distinctRecords.take(10),
                preCallNote = activeNote,
                availabilityInsight = insight,
                lastInteractionSummary = lastSummary,
                totalCalls = distinctRecords.size,
                totalDurationSeconds = distinctRecords.sumOf { it.durationSeconds },
                preferredSimSlot = contact.preferredSimSlot,
                isFavorite = contact.isFavorite,
                isPrivate = contact.isPrivate,
                alwaysRing = contact.alwaysRing,
                allowRestrictedHours = contact.allowDuringRestrictedHours,
                customRingtone = contact.customRingtoneUri ?: "Default Linea Chime",
                isLoading = false
            )
        }
    }

    fun toggleFavorite() {
        val current = _state.value.contact ?: return
        val newFav = !current.isFavorite
        viewModelScope.launch {
            contactDao.setContactFavorite(current.id, newFav)
            _state.value = _state.value.copy(
                isFavorite = newFav,
                contact = current.copy(isFavorite = newFav)
            )
        }
    }

    fun togglePrivate() {
        val current = _state.value.contact ?: return
        val newPrivate = !current.isPrivate
        viewModelScope.launch {
            if (newPrivate) {
                val sysId = current.androidContactId
                if (sysId != null && sysId > 0) {
                    contactSyncRepository.deleteContactFromSystemOnly(sysId)
                }
                contactDao.updateContact(
                    current.copy(
                        isPrivate = true,
                        androidContactId = null,
                        lookupKey = null
                    )
                )
                val numbers = contactDao.getNumbersForContact(current.id).first()
                for (num in numbers) {
                    val norm = num.number.filter { it.isDigit() }
                    callRecordDao.markRecordsAsPrivate(num.number, norm)
                    try {
                        context.contentResolver.delete(
                            android.provider.CallLog.Calls.CONTENT_URI,
                            "${android.provider.CallLog.Calls.NUMBER} = ? OR ${android.provider.CallLog.Calls.NUMBER} = ?",
                            arrayOf(num.number, norm)
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                contactSyncRepository.loadContacts()
                _state.value = _state.value.copy(
                    isPrivate = true,
                    contact = current.copy(isPrivate = true, androidContactId = null, lookupKey = null)
                )
            } else {
                contactDao.setContactPrivate(current.id, false)
                val numbers = contactDao.getNumbersForContact(current.id).first()
                for (num in numbers) {
                    val norm = num.number.filter { it.isDigit() }
                    callRecordDao.markRecordsAsPublic(num.number, norm)
                }
                contactSyncRepository.loadContacts()
                _state.value = _state.value.copy(
                    isPrivate = false,
                    contact = current.copy(isPrivate = false)
                )
            }
        }
    }

    fun setPreferredSim(simSlot: Int?) {
        val current = _state.value.contact ?: return
        viewModelScope.launch {
            contactDao.updatePerContactSettings(
                id = current.id,
                simSlot = simSlot,
                allowRestricted = _state.value.allowRestrictedHours,
                alwaysRing = _state.value.alwaysRing,
                ringtoneUri = current.customRingtoneUri
            )
            _state.value = _state.value.copy(preferredSimSlot = simSlot)
        }
    }

    fun toggleAlwaysRing() {
        val current = _state.value.contact ?: return
        val newAlwaysRing = !_state.value.alwaysRing
        viewModelScope.launch {
            contactDao.updatePerContactSettings(
                id = current.id,
                simSlot = _state.value.preferredSimSlot,
                allowRestricted = _state.value.allowRestrictedHours,
                alwaysRing = newAlwaysRing,
                ringtoneUri = current.customRingtoneUri
            )
            _state.value = _state.value.copy(alwaysRing = newAlwaysRing)
        }
    }

    fun toggleAllowRestrictedHours() {
        val current = _state.value.contact ?: return
        val newAllow = !_state.value.allowRestrictedHours
        viewModelScope.launch {
            contactDao.updatePerContactSettings(
                id = current.id,
                simSlot = _state.value.preferredSimSlot,
                allowRestricted = newAllow,
                alwaysRing = _state.value.alwaysRing,
                ringtoneUri = current.customRingtoneUri
            )
            _state.value = _state.value.copy(allowRestrictedHours = newAllow)
        }
    }

    fun savePreCallNote(noteText: String) {
        val current = _state.value.contact ?: return
        val number = _state.value.numbers.firstOrNull()?.number ?: ""
        viewModelScope.launch {
            callNoteDao.clearPreCallNotes(current.id, number)
            if (noteText.isNotBlank()) {
                callNoteDao.insertNote(
                    CallNoteEntity(
                        contactId = current.id,
                        phoneNumber = number,
                        noteText = noteText,
                        isPreCallNote = true
                    )
                )
            }
            val updatedNote = callNoteDao.getActivePreCallNote(current.id, number)
            _state.value = _state.value.copy(preCallNote = updatedNote)
        }
    }

    fun placeCall(number: String) {
        val simSlot = _state.value.preferredSimSlot ?: 0
        val simAccounts = phoneAccountManager.registerPhoneAccounts()
        val simHandle = simAccounts.find { it.slotIndex == simSlot }?.phoneAccountHandle
            ?: simAccounts.firstOrNull()?.phoneAccountHandle
        callManager.placeCall(number, simHandle)
    }

    fun updateContactPhoto(photoUri: String?, photoBytes: ByteArray?) {
        val current = _state.value.contact ?: return
        viewModelScope.launch {
            val updated = current.copy(photoUri = photoUri)
            contactSyncRepository.updateContact(
                updated,
                photoBytes = photoBytes,
                hasPhotoChanged = true
            )
            _state.value = _state.value.copy(contact = updated)
        }
    }

    fun updateContact(
        displayName: String,
        company: String?,
        numbers: List<Pair<String, String>>,
        emails: List<String>,
        preferredSimSlot: Int?,
        notes: String?,
        photoUri: String?,
        photoBytes: ByteArray?
    ) {
        val current = _state.value.contact ?: return
        viewModelScope.launch {
            val photoChanged = (photoUri != current.photoUri) || (photoBytes != null)
            val updated = current.copy(
                displayName = displayName,
                company = company,
                preferredSimSlot = preferredSimSlot,
                notes = notes,
                photoUri = if (photoChanged) photoUri else current.photoUri
            )
            contactSyncRepository.updateContact(
                updated,
                photoBytes = photoBytes,
                hasPhotoChanged = photoChanged
            )

            val validNumbers = numbers.filter { it.first.isNotBlank() }
            if (validNumbers.isNotEmpty()) {
                contactDao.deleteNumbersForContact(current.id)
                contactDao.insertNumbers(validNumbers.mapIndexed { idx, (num, label) ->
                    ContactNumberEntity(
                        contactId = current.id,
                        number = num,
                        normalizedNumber = num.filter { it.isDigit() || it == '+' },
                        label = label,
                        isPrimary = idx == 0
                    )
                })
            }

            loadContactData(current.id)
        }
    }
}
