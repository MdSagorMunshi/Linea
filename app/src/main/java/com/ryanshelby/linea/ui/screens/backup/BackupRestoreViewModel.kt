package com.ryanshelby.linea.ui.screens.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.data.backup.BackupRestoreManager
import com.ryanshelby.linea.data.backup.BackupRestoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupScreenState(
    val isExporting: Boolean = false,
    val exportedJson: String? = null,
    val isRestoring: Boolean = false,
    val restoreResult: BackupRestoreResult? = null
)

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val backupManager: BackupRestoreManager
) : ViewModel() {

    private val _state = MutableStateFlow(BackupScreenState())
    val state: StateFlow<BackupScreenState> = _state.asStateFlow()

    fun exportData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isExporting = true)
            val json = backupManager.generateBackupJson()
            _state.value = _state.value.copy(
                isExporting = false,
                exportedJson = json
            )
        }
    }

    fun restoreData(jsonText: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRestoring = true)
            val result = backupManager.restoreFromJson(jsonText)
            _state.value = _state.value.copy(
                isRestoring = false,
                restoreResult = result
            )
        }
    }

    fun clearResult() {
        _state.value = _state.value.copy(restoreResult = null)
    }
}
