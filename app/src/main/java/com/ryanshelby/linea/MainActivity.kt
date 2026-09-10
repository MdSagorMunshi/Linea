package com.ryanshelby.linea

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.permissions.PermissionCoordinator
import com.ryanshelby.linea.telecom.PhoneAccountManager
import com.ryanshelby.linea.telecom.RoleHelper
import com.ryanshelby.linea.telecom.SimAccountInfo
import com.ryanshelby.linea.ui.navigation.LineaNavGraph
import androidx.compose.material.icons.Icons
import com.ryanshelby.linea.ui.components.LiquidGlassPermissionDialog
import com.ryanshelby.linea.ui.theme.LineaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var roleHelper: RoleHelper

    @Inject
    lateinit var phoneAccountManager: PhoneAccountManager

    @Inject
    lateinit var lineaPreferences: LineaPreferences

    @Inject
    lateinit var callManager: com.ryanshelby.linea.telecom.CallManager

    @Inject
    lateinit var permissionCoordinator: PermissionCoordinator

    private var isDefaultDialerState by mutableStateOf(false)
    private var simAccountsState by mutableStateOf<List<SimAccountInfo>>(emptyList())
    private var permissionMessage by mutableStateOf<String?>(null)
    private var showOpenSettings by mutableStateOf(false)
    private var lastRequestedPermissions: List<String> = emptyList()

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        checkRoleAndAccounts()
    }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        checkRoleAndAccounts()
        handlePermissionResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkRoleAndAccounts()
        lifecycleScope.launch {
            permissionCoordinator.requests.collect { request ->
                requestPermissionsOrExplain(request.permissions)
            }
        }
        handleCallIntent(intent)

        setContent {
            val reduceAnimations by lineaPreferences.reduceAnimations.collectAsState(initial = false)
            val themePreference by lineaPreferences.themePreference.collectAsState(initial = "DARK")
            val permissionIntroShown by lineaPreferences.permissionIntroShown.collectAsState(initial = null)

            LineaTheme(theme = themePreference, reduceAnimations = reduceAnimations) {
                LineaNavGraph(
                    isDefaultDialer = isDefaultDialerState,
                    simAccounts = simAccountsState,
                    reduceAnimations = reduceAnimations,
                    phoneAccountManager = phoneAccountManager,
                    lineaPreferences = lineaPreferences,
                    callManager = callManager,
                    onRequestDefaultDialer = { requestDefaultDialerRole() },
                    onRequestPermissions = { requestAllPermissionsUpfront() }
                )

                if (permissionIntroShown == false) {
                    LiquidGlassPermissionDialog(
                        title = "Permissions required",
                        message = "Linea needs Phone and Phone State to place calls, Microphone for voice calls, " +
                            "Contacts and Call Log for caller information and history, and Notifications for call alerts. " +
                            "Calling will be blocked when required permissions are denied.",
                        actionLabel = "Continue",
                        onAction = {
                            lifecycleScope.launch { lineaPreferences.setPermissionIntroShown(true) }
                            requestAllPermissionsUpfront()
                        },
                        dismissible = false
                    )
                }

                permissionMessage?.let { message ->
                    LiquidGlassPermissionDialog(
                        title = "Permission needed",
                        message = message,
                        actionLabel = if (showOpenSettings) "Open settings" else "OK",
                        onAction = {
                            if (showOpenSettings) openAppPermissionSettings()
                            permissionMessage = null
                            showOpenSettings = false
                        },
                        dismissible = true,
                        onDismiss = {
                            permissionMessage = null
                            showOpenSettings = false
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkRoleAndAccounts()
        permissionCoordinator.resumePendingOutgoingCallIfPermitted()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCallIntent(intent)
    }

    private fun handleCallIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action == Intent.ACTION_CALL) {
            val number = intent.data?.schemeSpecificPart
            if (!number.isNullOrBlank()) {
                callManager.placeCall(number)
            }
        }
    }

    private fun checkRoleAndAccounts() {
        isDefaultDialerState = roleHelper.isDefaultDialer()
        simAccountsState = phoneAccountManager.registerPhoneAccounts()
    }

    private fun requestDefaultDialerRole() {
        val intent = roleHelper.createRequestRoleIntent()
        if (intent != null) {
            roleRequestLauncher.launch(intent)
        }
    }

    private fun requestAllPermissionsUpfront() {
        val permissions = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ANSWER_PHONE_CALLS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        requestPermissionsOrExplain(neededPermissions)
    }

    private fun requestPermissionsOrExplain(permissions: List<String>) {
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            permissionCoordinator.resumePendingOutgoingCallIfPermitted()
            return
        }
        val prefs = getSharedPreferences("linea_permission_requests", MODE_PRIVATE)
        val permanentlyDenied = missing.filter { permission ->
            prefs.getBoolean(permission, false) && !shouldShowRequestPermissionRationale(permission)
        }
        if (permanentlyDenied.isNotEmpty()) {
            showPermissionExplanation(permanentlyDenied, openSettings = true)
            return
        }
        lastRequestedPermissions = missing
        missing.forEach { prefs.edit().putBoolean(it, true).apply() }
        permissionsLauncher.launch(missing.toTypedArray())
    }

    private fun handlePermissionResult() {
        val denied = lastRequestedPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        // The queued call itself only requires the coordinator's phone/mic set; unrelated
        // permissions denied in the same first-launch batch must not make that re-check stale.
        permissionCoordinator.resumePendingOutgoingCallIfPermitted()
        if (denied.isNotEmpty()) {
            val openSettings = denied.any { !shouldShowRequestPermissionRationale(it) }
            showPermissionExplanation(denied, openSettings)
        }
    }

    private fun showPermissionExplanation(permissions: List<String>, openSettings: Boolean) {
        permissionMessage = "${permissions.joinToString { permissionLabel(it) }} ${if (openSettings) "must be enabled in Android Settings before calling can continue." else "was denied. Some features, including calling or recording, will not work correctly without it."}"
        showOpenSettings = openSettings
    }

    private fun permissionLabel(permission: String): String = when (permission) {
        Manifest.permission.CALL_PHONE -> "Phone permission"
        Manifest.permission.READ_PHONE_STATE -> "Phone state permission"
        Manifest.permission.RECORD_AUDIO -> "Microphone permission"
        Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS -> "Contacts permission"
        Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG -> "Call log permission"
        else -> "Required permission"
    }

    private fun openAppPermissionSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }
}
