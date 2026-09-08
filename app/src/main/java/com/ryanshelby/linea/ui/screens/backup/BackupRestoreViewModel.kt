package com.ryanshelby.linea.ui.screens.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.data.backup.BackupRestoreManager
import com.ryanshelby.linea.data.backup.BackupRestoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

data class BackupScreenState(
    val isExporting: Boolean = false,
    val exportedJson: String? = null,
    val isRestoring: Boolean = false,
    val restoreResult: BackupRestoreResult? = null,
    val exportSuccessMessage: String? = null
)

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val backupManager: BackupRestoreManager
) : ViewModel() {

    private val _state = MutableStateFlow(BackupScreenState())
    val state: StateFlow<BackupScreenState> = _state.asStateFlow()

    fun exportToFileUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isExporting = true, exportSuccessMessage = null)
            try {
                val json = withContext(Dispatchers.IO) {
                    backupManager.generateBackupJson()
                }
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray(Charsets.UTF_8))
                        os.flush()
                    }
                }
                _state.value = _state.value.copy(
                    isExporting = false,
                    exportedJson = json,
                    exportSuccessMessage = "Backup successfully saved to file!"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isExporting = false,
                    exportSuccessMessage = "Export failed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun restoreFromFileUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRestoring = true, restoreResult = null)
            try {
                val jsonText = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                            reader.readText()
                        }
                    } ?: ""
                }
                if (jsonText.isBlank()) {
                    _state.value = _state.value.copy(
                        isRestoring = false,
                        restoreResult = BackupRestoreResult(false, 0, 0, "Selected file is empty or unreadable.")
                    )
                    return@launch
                }
                val result = withContext(Dispatchers.IO) {
                    backupManager.restoreFromJson(jsonText)
                }
                _state.value = _state.value.copy(
                    isRestoring = false,
                    restoreResult = result
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isRestoring = false,
                    restoreResult = BackupRestoreResult(false, 0, 0, "Restore failed: ${e.localizedMessage}")
                )
            }
        }
    }

    fun exportData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isExporting = true, exportSuccessMessage = null)
            val json = withContext(Dispatchers.IO) {
                backupManager.generateBackupJson()
            }
            _state.value = _state.value.copy(
                isExporting = false,
                exportedJson = json
            )
        }
    }

    fun restoreData(jsonText: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRestoring = true, restoreResult = null)
            val result = withContext(Dispatchers.IO) {
                backupManager.restoreFromJson(jsonText)
            }
            _state.value = _state.value.copy(
                isRestoring = false,
                restoreResult = result
            )
        }
    }

    fun clearResult() {
        _state.value = _state.value.copy(restoreResult = null, exportSuccessMessage = null)
    }
}
