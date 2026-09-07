package com.ryanshelby.linea

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.telecom.PhoneAccountManager
import com.ryanshelby.linea.telecom.RoleHelper
import com.ryanshelby.linea.telecom.SimAccountInfo
import com.ryanshelby.linea.ui.navigation.LineaNavGraph
import com.ryanshelby.linea.ui.theme.LineaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var roleHelper: RoleHelper

    @Inject
    lateinit var phoneAccountManager: PhoneAccountManager

    @Inject
    lateinit var lineaPreferences: LineaPreferences

    private var isDefaultDialerState by mutableStateOf(false)
    private var simAccountsState by mutableStateOf<List<SimAccountInfo>>(emptyList())

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        checkRoleAndAccounts()
    }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        checkRoleAndAccounts()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkRoleAndAccounts()
        requestAllPermissionsUpfront()

        setContent {
            val scope = rememberCoroutineScope()
            val reduceAnimations by lineaPreferences.reduceAnimations.collectAsState(initial = false)

            LineaTheme(reduceAnimations = reduceAnimations) {
                LineaNavGraph(
                    isDefaultDialer = isDefaultDialerState,
                    simAccounts = simAccountsState,
                    reduceAnimations = reduceAnimations,
                    onRequestDefaultDialer = { requestDefaultDialerRole() },
                    onRequestPermissions = { requestAllPermissionsUpfront() },
                    onToggleReduceAnimations = { enabled ->
                        scope.launch {
                            lineaPreferences.setReduceAnimations(enabled)
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkRoleAndAccounts()
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

        if (neededPermissions.isNotEmpty()) {
            permissionsLauncher.launch(neededPermissions.toTypedArray())
        }
    }
}
