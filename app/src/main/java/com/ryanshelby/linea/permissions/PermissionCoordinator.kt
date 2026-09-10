package com.ryanshelby.linea.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PermissionRequest(val permissions: List<String>)

/**
 * The only gate used before initiating a cellular call. Permission state is read from the
 * platform on every invocation so revocations from Settings are immediately honoured.
 */
@Singleton
class PermissionCoordinator @Inject constructor(@ApplicationContext private val context: Context) {
    val outgoingCallPermissions = listOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.RECORD_AUDIO
    )

    private val _requests = MutableSharedFlow<PermissionRequest>(replay = 1, extraBufferCapacity = 1)
    val requests = _requests.asSharedFlow()
    private var pendingOutgoingCall: (() -> Unit)? = null

    fun missingOutgoingCallPermissions(): List<String> = outgoingCallPermissions.filter {
        context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
    }

    fun runWhenOutgoingCallPermitted(action: () -> Unit) {
        val missing = missingOutgoingCallPermissions()
        if (missing.isEmpty()) {
            action()
        } else {
            pendingOutgoingCall = action
            _requests.tryEmit(PermissionRequest(missing))
        }
    }

    fun resumePendingOutgoingCallIfPermitted() {
        if (missingOutgoingCallPermissions().isEmpty()) {
            pendingOutgoingCall?.also { pendingOutgoingCall = null }?.invoke()
        }
    }

    fun cancelPendingOutgoingCall() {
        pendingOutgoingCall = null
    }
}
