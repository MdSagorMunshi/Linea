package com.ryanshelby.linea.nfc.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.ryanshelby.linea.qr_code.QrCodeEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates high-speed, secure, bidirectional contact and Digital Card data transfer
 * over Bluetooth Low Energy (BLE), triggered via NFC Out-of-Band (OOB) touch intent.
 *
 * Characteristics:
 * - Sub-second transfer with MTU 512 over LE 2M/1M PHY.
 * - Out-of-band ephemeral Service UUID and AES-GCM-256 session key exchange via NFC.
 * - Symmetrical bidirectional data flow: Central and Peripheral exchange cards in one tap.
 * - Immediate physical detachment: users can pull phones apart immediately after the tap.
 * - Strict timeout guards and automatic teardown.
 */
@Singleton
class BleHandoverManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BleHandoverManager"
        private const val HANDOVER_TIMEOUT_MS = 6000L
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // Sub-characteristic UUIDs derived deterministically
        val CHAR_RX_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb") // Central writes to Peripheral
        val CHAR_TX_UUID: UUID = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb") // Peripheral notifies to Central

        /**
         * Derives an ephemeral BLE Service UUID and a 256-bit AES-GCM secret key
         * deterministically from the NFC nonces exchanged during the physical tap.
         */
        fun deriveSessionKeys(nonceA: ByteArray, nonceB: ByteArray): Pair<UUID, SecretKey> {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(nonceA)
            digest.update(nonceB)
            digest.update("LiNEA-BLE-Handover-v1".toByteArray(StandardCharsets.UTF_8))
            val hash = digest.digest()

            val byteBuffer = ByteBuffer.wrap(hash)
            val mostSigBits = byteBuffer.long
            val leastSigBits = byteBuffer.long
            val serviceUuid = UUID(mostSigBits, leastSigBits)

            val secretKey = SecretKeySpec(hash, "AES")
            return Pair(serviceUuid, secretKey)
        }

        fun encryptPayload(plainText: String, secretKey: SecretKey): ByteArray {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv // 12 bytes
            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val result = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, result, 0, iv.size)
            System.arraycopy(cipherText, 0, result, iv.size, cipherText.size)
            return result
        }

        fun decryptPayload(encryptedBytes: ByteArray, secretKey: SecretKey): String? {
            if (encryptedBytes.size < 13) return null
            val iv = encryptedBytes.copyOfRange(0, 12)
            val cipherText = encryptedBytes.copyOfRange(12, encryptedBytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            val decrypted = cipher.doFinal(cipherText)
            return String(decrypted, StandardCharsets.UTF_8)
        }
    }

    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var isSessionActive = false

    // Central state
    private var activeGattClient: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null

    // Peripheral state
    private var gattServer: BluetoothGattServer? = null
    private var advertiseCallback: AdvertiseCallback? = null

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    val isBleSupported: Boolean
        get() = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE)

    /**
     * Start Central (Client) role. Initiator of the NFC bump initiates scan for the ephemeral Service UUID,
     * connects to the Peripheral, writes outbound payload, and awaits inbound payload.
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    fun startCentralTransfer(
        serviceUuid: UUID,
        secretKey: SecretKey,
        outboundPayload: QrCodeEngine.ContactPayload?,
        onSuccess: (QrCodeEngine.ContactPayload) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isSessionActive) {
            cancelActiveSession()
        }
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            onError("Bluetooth is not enabled")
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            onError("BLE Scanner unavailable")
            return
        }

        isSessionActive = true
        scheduleWatchdog("Central transfer timed out", onError)

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(serviceUuid))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val device = result?.device ?: return
                Log.d(TAG, "Central discovered peripheral: ${device.address}")
                try {
                    scanner.stopScan(this)
                } catch (_: Exception) {}
                scanCallback = null

                connectToGattServer(device, serviceUuid, secretKey, outboundPayload, onSuccess, onError)
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Central scan failed with error code: $errorCode")
                cancelActiveSession()
                onError("BLE Scan failed ($errorCode)")
            }
        }

        try {
            scanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
            Log.d(TAG, "Central scanning for ephemeral UUID: $serviceUuid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE scan", e)
            cancelActiveSession()
            onError("Failed to start BLE scan: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToGattServer(
        device: BluetoothDevice,
        serviceUuid: UUID,
        secretKey: SecretKey,
        outboundPayload: QrCodeEngine.ContactPayload?,
        onSuccess: (QrCodeEngine.ContactPayload) -> Unit,
        onError: (String) -> Unit
    ) {
        val gattCallback = object : BluetoothGattCallback() {
            private var rxChar: BluetoothGattCharacteristic? = null
            private var txChar: BluetoothGattCharacteristic? = null

            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Central connected to GATT server. Requesting MTU 512...")
                    activeGattClient = gatt
                    gatt.requestMtu(512)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Central disconnected from GATT server")
                    cancelActiveSession()
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                Log.d(TAG, "MTU configured to $mtu. Discovering services...")
                gatt.discoverServices()
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    onError("Service discovery failed")
                    cancelActiveSession()
                    return
                }

                val service = gatt.getService(serviceUuid)
                if (service == null) {
                    onError("Handover service not found on peer")
                    cancelActiveSession()
                    return
                }

                rxChar = service.getCharacteristic(CHAR_RX_UUID)
                txChar = service.getCharacteristic(CHAR_TX_UUID)

                // Enable notifications on TX characteristic
                txChar?.let { tx ->
                    gatt.setCharacteristicNotification(tx, true)
                    val descriptor = tx.getDescriptor(CCCD_UUID)
                    if (descriptor != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        } else {
                            @Suppress("DEPRECATION")
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            @Suppress("DEPRECATION")
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                }

                // Transmit outbound contact payload to Peripheral
                sendOutboundData(gatt, rxChar, outboundPayload, secretKey)
            }

            private fun sendOutboundData(
                gatt: BluetoothGatt,
                rx: BluetoothGattCharacteristic?,
                payload: QrCodeEngine.ContactPayload?,
                key: SecretKey
            ) {
                if (rx == null || payload == null) return
                try {
                    val rawUri = QrCodeEngine.serializeToUri(payload)
                    val encrypted = encryptPayload(rawUri, key)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeCharacteristic(rx, encrypted, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    } else {
                        @Suppress("DEPRECATION")
                        rx.value = encrypted
                        @Suppress("DEPRECATION")
                        rx.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        @Suppress("DEPRECATION")
                        gatt.writeCharacteristic(rx)
                    }
                    Log.d(TAG, "Central transmitted outbound payload (${encrypted.size} bytes)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write outbound characteristic", e)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                if (characteristic.uuid == CHAR_TX_UUID) {
                    @Suppress("DEPRECATION")
                    handleIncomingBytes(characteristic.value)
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                if (characteristic.uuid == CHAR_TX_UUID) {
                    handleIncomingBytes(value)
                }
            }

            private fun handleIncomingBytes(bytes: ByteArray) {
                try {
                    val decrypted = decryptPayload(bytes, secretKey)
                    if (!decrypted.isNullOrBlank()) {
                        val parsed = QrCodeEngine.parseLineaScannedText(decrypted) ?: QrCodeEngine.parseScannedText(decrypted)
                        if (parsed != null) {
                            Log.d(TAG, "Central received valid contact: ${parsed.name}")
                            triggerHapticFeedback()
                            mainHandler.post { onSuccess(parsed) }
                            // Completed mutual exchange successfully
                            mainHandler.postDelayed({ cancelActiveSession() }, 500)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Central failed to decrypt received payload", e)
                }
            }
        }

        try {
            activeGattClient = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to GATT server", e)
            cancelActiveSession()
            onError("Connection failed: ${e.message}")
        }
    }

    /**
     * Start Peripheral (Server) role. Target of the NFC bump advertises the ephemeral Service UUID,
     * hosts the GATT server, receives Central's payload, and notifies its own payload back.
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    fun startPeripheralTransfer(
        serviceUuid: UUID,
        secretKey: SecretKey,
        outboundPayload: QrCodeEngine.ContactPayload?,
        onSuccess: (QrCodeEngine.ContactPayload) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isSessionActive) {
            cancelActiveSession()
        }
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            onError("Bluetooth is not enabled")
            return
        }

        val advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            onError("BLE Advertiser unavailable on this device")
            return
        }

        isSessionActive = true
        scheduleWatchdog("Peripheral transfer timed out", onError)

        var connectedClientDevice: BluetoothDevice? = null

        // 1. Setup GATT Server
        val serverCallback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connectedClientDevice = device
                    Log.d(TAG, "Peripheral: Central connected: ${device.address}")
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Peripheral: Central disconnected")
                    cancelActiveSession()
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
                }

                if (characteristic.uuid == CHAR_RX_UUID && value != null) {
                    try {
                        val decrypted = decryptPayload(value, secretKey)
                        if (!decrypted.isNullOrBlank()) {
                            val parsed = QrCodeEngine.parseLineaScannedText(decrypted) ?: QrCodeEngine.parseScannedText(decrypted)
                            if (parsed != null) {
                                Log.d(TAG, "Peripheral received valid contact: ${parsed.name}")
                                triggerHapticFeedback()
                                mainHandler.post { onSuccess(parsed) }

                                // Send outbound payload back to Central if present
                                sendReturnPayload(device, outboundPayload, secretKey)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Peripheral failed to decrypt incoming payload", e)
                    }
                }
            }

            private fun sendReturnPayload(device: BluetoothDevice, payload: QrCodeEngine.ContactPayload?, key: SecretKey) {
                if (payload == null) {
                    // No return payload, close session after short delay
                    mainHandler.postDelayed({ cancelActiveSession() }, 600)
                    return
                }

                try {
                    val rawUri = QrCodeEngine.serializeToUri(payload)
                    val encrypted = encryptPayload(rawUri, key)
                    val txChar = gattServer?.getService(serviceUuid)?.getCharacteristic(CHAR_TX_UUID)
                    if (txChar != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gattServer?.notifyCharacteristicChanged(device, txChar, false, encrypted)
                        } else {
                            @Suppress("DEPRECATION")
                            txChar.value = encrypted
                            @Suppress("DEPRECATION")
                            gattServer?.notifyCharacteristicChanged(device, txChar, false)
                        }
                        Log.d(TAG, "Peripheral returned outbound payload (${encrypted.size} bytes)")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to notify return payload", e)
                } finally {
                    mainHandler.postDelayed({ cancelActiveSession() }, 800)
                }
            }
        }

        try {
            val server = bluetoothManager?.openGattServer(context, serverCallback)
            if (server == null) {
                onError("Failed to open GATT Server")
                cancelActiveSession()
                return
            }
            gattServer = server

            val handoverService = BluetoothGattService(serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)

            val rxChar = BluetoothGattCharacteristic(
                CHAR_RX_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )

            val txChar = BluetoothGattCharacteristic(
                CHAR_TX_UUID,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            val cccdDescriptor = BluetoothGattDescriptor(
                CCCD_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )
            txChar.addDescriptor(cccdDescriptor)

            handoverService.addCharacteristic(rxChar)
            handoverService.addCharacteristic(txChar)
            server.addService(handoverService)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup GATT Server service", e)
            cancelActiveSession()
            onError("GATT Server setup failed: ${e.message}")
            return
        }

        // 2. Start Advertising
        val advertiseSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .setTimeout(5000)
            .build()

        val advertiseData = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(serviceUuid))
            .setIncludeDeviceName(false)
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.d(TAG, "Peripheral advertising ephemeral Service UUID: $serviceUuid")
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "Peripheral advertising failed with code $errorCode")
                cancelActiveSession()
                onError("BLE Advertising failed ($errorCode)")
            }
        }

        try {
            advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE advertising", e)
            cancelActiveSession()
            onError("Advertising error: ${e.message}")
        }
    }

    private fun scheduleWatchdog(timeoutMessage: String, onError: (String) -> Unit) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isSessionActive) {
                Log.w(TAG, "BLE Handover watchdog triggered: $timeoutMessage")
                cancelActiveSession()
                onError(timeoutMessage)
            }
        }, HANDOVER_TIMEOUT_MS)
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun cancelActiveSession() {
        mainHandler.removeCallbacksAndMessages(null)
        isSessionActive = false

        // Stop scanning
        try {
            scanCallback?.let {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(it)
            }
        } catch (_: Exception) {}
        scanCallback = null

        // Disconnect GATT client
        try {
            activeGattClient?.disconnect()
            activeGattClient?.close()
        } catch (_: Exception) {}
        activeGattClient = null

        // Stop advertising
        try {
            advertiseCallback?.let {
                bluetoothAdapter?.bluetoothLeAdvertiser?.stopAdvertising(it)
            }
        } catch (_: Exception) {}
        advertiseCallback = null

        // Close GATT server
        try {
            gattServer?.clearServices()
            gattServer?.close()
        } catch (_: Exception) {}
        gattServer = null

        Log.d(TAG, "BLE Handover session cleanly terminated")
    }

    private fun triggerHapticFeedback() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 75, 40, 95), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(120)
            }
        } catch (_: Exception) {}
    }
}
