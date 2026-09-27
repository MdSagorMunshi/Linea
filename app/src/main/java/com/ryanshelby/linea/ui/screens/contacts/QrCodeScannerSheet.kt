package com.ryanshelby.linea.ui.screens.contacts

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.ryanshelby.linea.qr_code.QrCodeEngine
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * QR Code Scanner Sheet.
 * Real-time camera scanner powered by Google ML Kit with Photo Gallery fallback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeScannerSheet(
    onDismiss: () -> Unit,
    onSaveContact: (name: String, numbers: List<String>) -> Unit,
    onCallNumber: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val sheetLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(sheetLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        sheetLifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            sheetLifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var decodedPayload by remember { mutableStateOf<QrCodeEngine.ContactPayload?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var isAnalyzingPhoto by remember { mutableStateOf(false) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var toggleTorchAction by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    var isCameraStreaming by remember { mutableStateOf(false) }
    var cameraErrorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Photo picker launcher with ML Kit primary + ZXing dual-pass fallback
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isAnalyzingPhoto = true
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val scanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                )
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        // If there are many QR codes in the photo, only detect the LiNEA QR code:
                        val lineaBarcode = barcodes.firstOrNull { barcode ->
                            val raw = barcode.rawValue
                            !raw.isNullOrBlank() && QrCodeEngine.isLineaQrCode(raw)
                        }
                        if (lineaBarcode?.rawValue != null) {
                            val payload = QrCodeEngine.parseLineaScannedText(lineaBarcode.rawValue)
                            if (payload != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                decodedPayload = payload
                                showResult = true
                                isAnalyzingPhoto = false
                            } else {
                                isAnalyzingPhoto = false
                                Toast.makeText(context, "Invalid LiNEA QR code", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Fallback to ZXing dual-pass (normal + inverted for dark mode QR codes)
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val zxingRaw = decodeLineaQrWithZxing(context, uri)
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    isAnalyzingPhoto = false
                                    if (!zxingRaw.isNullOrBlank()) {
                                        val payload = QrCodeEngine.parseLineaScannedText(zxingRaw)
                                        if (payload != null) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            decodedPayload = payload
                                            showResult = true
                                        } else {
                                            Toast.makeText(context, "Invalid LiNEA QR code", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        if (barcodes.isNotEmpty()) {
                                            Toast.makeText(context, "Not a LiNEA QR code. Please scan a LiNEA QR code.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "No LiNEA QR code found in photo", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Fallback to ZXing dual-pass on ML Kit failure
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val zxingRaw = decodeLineaQrWithZxing(context, uri)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                isAnalyzingPhoto = false
                                if (!zxingRaw.isNullOrBlank()) {
                                    val payload = QrCodeEngine.parseLineaScannedText(zxingRaw)
                                    if (payload != null) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        decodedPayload = payload
                                        showResult = true
                                    } else {
                                        Toast.makeText(context, "Invalid LiNEA QR code", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "No LiNEA QR code found in photo", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    .addOnCompleteListener {
                        scanner.close()
                    }
            } catch (_: Exception) {
                // Fallback to ZXing if InputImage failed to load
                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val zxingRaw = decodeLineaQrWithZxing(context, uri)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isAnalyzingPhoto = false
                        if (!zxingRaw.isNullOrBlank()) {
                            val payload = QrCodeEngine.parseLineaScannedText(zxingRaw)
                            if (payload != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                decodedPayload = payload
                                showResult = true
                            } else {
                                Toast.makeText(context, "Invalid LiNEA QR code", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Failed to load photo", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LineaColors.BackgroundTop,
        contentColor = LineaColors.TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scan Contact QR",
                    style = LineaTypography.titleLarge,
                    color = LineaColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Flash toggle
                    if (hasCameraPermission && !showResult && toggleTorchAction != null) {
                        IconButton(
                            onClick = {
                                val next = !isTorchEnabled
                                isTorchEnabled = next
                                toggleTorchAction?.invoke(next)
                            }
                        ) {
                            Icon(
                                imageVector = if (isTorchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                contentDescription = "Toggle Torch",
                                tint = if (isTorchEnabled) LineaColors.TitaniumBlue else LineaColors.TextSecondary
                            )
                        }
                    }

                    // Gallery photo picker button
                    IconButton(
                        onClick = { photoPickerLauncher.launch("image/*") }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = "Scan from Photos",
                            tint = LineaColors.TitaniumBlue
                        )
                    }

                    // Close button
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = LineaColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isAnalyzingPhoto) {
                // Photo Analysis Loading State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = LineaColors.TitaniumBlue,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing photo...",
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextSecondary
                        )
                    }
                }
            } else if (showResult && decodedPayload != null) {
                // Decoded Result Card
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn() + fadeIn()
                ) {
                    DecodedContactCard(
                        payload = decodedPayload!!,
                        onCall = { number ->
                            onCallNumber(number)
                            onDismiss()
                        },
                        onSave = {
                            decodedPayload?.let { p ->
                                onSaveContact(p.name, p.numbers)
                            }
                            onDismiss()
                        },
                        onScanAgain = {
                            decodedPayload = null
                            showResult = false
                        }
                    )
                }
            } else if (hasCameraPermission) {
                // Camera Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(LineaColors.BackgroundDeep),
                    contentAlignment = Alignment.Center
                ) {
                    if (cameraErrorMessage == null) {
                        CameraPreviewWithMlKit(
                            retryTrigger = retryTrigger,
                            onCodeDetected = { payload ->
                                if (decodedPayload == null) {
                                    decodedPayload = payload
                                    showResult = true
                                }
                            },
                            onTorchReady = { toggleFn ->
                                toggleTorchAction = toggleFn
                            },
                            onCameraStreaming = { streaming ->
                                isCameraStreaming = streaming
                            },
                            onCameraError = { error ->
                                cameraErrorMessage = error
                                isCameraStreaming = false
                                toggleTorchAction = null
                                isTorchEnabled = false
                            }
                        )

                        // Loading spinner while camera hardware is starting
                        if (!isCameraStreaming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = LineaColors.TitaniumBlue,
                                strokeWidth = 3.dp
                            )
                        }

                        // Rounded Viewfinder overlay
                        Box(
                            modifier = Modifier
                                .size(230.dp)
                                .border(
                                    2.dp,
                                    LineaColors.TitaniumBlue.copy(alpha = if (isCameraStreaming) 0.85f else 0.35f),
                                    RoundedCornerShape(28.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Subtle center guide
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        LineaColors.TitaniumBlue.copy(alpha = if (isCameraStreaming) 0.6f else 0.25f),
                                        CircleShape
                                    )
                            )
                        }
                    } else {
                        // Camera Hardware Error State
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = LineaColors.TextTertiary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = cameraErrorMessage ?: "Camera unavailable",
                                style = LineaTypography.bodyMedium,
                                color = LineaColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        cameraErrorMessage = null
                                        isCameraStreaming = false
                                        retryTrigger++
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LineaColors.TitaniumBlue
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Retry Camera")
                                }
                                OutlinedButton(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Scan Photo")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (cameraErrorMessage != null) "Select a QR code from photos or retry" else "Align LiNEA QR code inside the frame",
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TextTertiary,
                    textAlign = TextAlign.Center
                )
            } else {
                // No Camera Permission
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Camera permission required to scan QR code",
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LineaColors.TitaniumBlue
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Grant Permission")
                            }
                            OutlinedButton(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Scan Photo")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Resolves the hosting Activity to ensure reliable CameraX lifecycle binding
 * across custom dialog windows and OEM skins (OneUI, HyperOS, ColorOS, etc.).
 */
private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Camera preview with real-time Google ML Kit QR code analysis.
 * Uses a resilient multi-pass binding strategy to guarantee compatibility across
 * all physical device hardware levels (LEGACY, LIMITED, FULL, LEVEL_3) and aspect ratios.
 */
@Composable
private fun CameraPreviewWithMlKit(
    retryTrigger: Int,
    onCodeDetected: (QrCodeEngine.ContactPayload) -> Unit,
    onTorchReady: ((Boolean) -> Unit) -> Unit,
    onCameraStreaming: (Boolean) -> Unit,
    onCameraError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val effectiveLifecycleOwner = remember(context, lifecycleOwner) {
        context.findActivity() ?: lifecycleOwner
    }
    val haptic = LocalHapticFeedback.current
    val cameraExecutor = remember(retryTrigger) { Executors.newSingleThreadExecutor() }
    val isScanning = remember(retryTrigger) { AtomicBoolean(false) }
    val cameraProviderRef = remember(retryTrigger) { AtomicReference<ProcessCameraProvider?>(null) }

    DisposableEffect(retryTrigger) {
        onDispose {
            try {
                cameraProviderRef.get()?.unbindAll()
            } catch (e: Throwable) {
                Log.w("QrScanner", "Error unbinding camera on dispose", e)
            }
            try {
                cameraExecutor.shutdown()
            } catch (e: Throwable) {
                Log.w("QrScanner", "Error shutting down camera executor", e)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            // Observe the stream state to detect when frames actively stream
            previewView.previewStreamState.observe(effectiveLifecycleOwner) { streamState ->
                onCameraStreaming(streamState == PreviewView.StreamState.STREAMING)
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProviderRef.set(cameraProvider)

                    val cameraSelector = when {
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                        else -> CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    val scanner: BarcodeScanner? = try {
                        BarcodeScanning.getClient(
                            BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                        )
                    } catch (e: Throwable) {
                        Log.w("QrScanner", "ML Kit barcode scanner initialization failed; using offline ZXing analyzer", e)
                        null
                    }

                    val mainExecutor = ContextCompat.getMainExecutor(ctx)
                    val onScannedAction: (String) -> Unit = { rawText ->
                        val payload = QrCodeEngine.parseLineaScannedText(rawText)
                        if (payload != null) {
                            mainExecutor.execute {
                                try {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } catch (_: Throwable) {}
                                onCodeDetected(payload)
                            }
                        }
                    }

                    var boundCamera: androidx.camera.core.Camera? = null

                    // ── Pass 1: Standard 720p ResolutionStrategy ─────────────────────────────
                    try {
                        cameraProvider.unbindAll()
                        val resolutionSelector = ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(1280, 720),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                )
                            )
                            .build()

                        val preview = Preview.Builder()
                            .setResolutionSelector(resolutionSelector)
                            .build()
                            .also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setResolutionSelector(resolutionSelector)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    processImageProxy(imageProxy, scanner, isScanning, onScannedAction)
                                }
                            }

                        boundCamera = cameraProvider.bindToLifecycle(
                            effectiveLifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Throwable) {
                        Log.w("QrScanner", "Pass 1 (720p strategy) failed; trying default CDD resolution", e)
                    }

                    // ── Pass 2: Fallback to CDD-guaranteed default combination ────────────────
                    if (boundCamera == null) {
                        try {
                            cameraProvider.unbindAll()
                            val preview = Preview.Builder()
                                .build()
                                .also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        processImageProxy(imageProxy, scanner, isScanning, onScannedAction)
                                    }
                                }

                            boundCamera = cameraProvider.bindToLifecycle(
                                effectiveLifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Throwable) {
                            Log.w("QrScanner", "Pass 2 (default resolution) failed; trying preview-only fallback", e)
                        }
                    }

                    // ── Pass 3: Preview-only fallback ─────────────────────────────────────────
                    if (boundCamera == null) {
                        try {
                            cameraProvider.unbindAll()
                            val preview = Preview.Builder()
                                .build()
                                .also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                            boundCamera = cameraProvider.bindToLifecycle(
                                effectiveLifecycleOwner,
                                cameraSelector,
                                preview
                            )
                        } catch (e: Throwable) {
                            Log.e("QrScanner", "Pass 3 (preview-only) failed", e)
                        }
                    }

                    if (boundCamera != null) {
                        val activeCam = boundCamera
                        onTorchReady { enable ->
                            try {
                                activeCam.cameraControl.enableTorch(enable)
                            } catch (e: Throwable) {
                                Log.w("QrScanner", "Failed to toggle torch", e)
                            }
                        }

                        // Safe tap-to-focus
                        previewView.setOnTouchListener { v, event ->
                            if (event.action == android.view.MotionEvent.ACTION_UP) {
                                try {
                                    if (previewView.width > 0 && previewView.height > 0) {
                                        val factory = previewView.meteringPointFactory
                                        val point = factory.createPoint(event.x, event.y)
                                        val action = FocusMeteringAction.Builder(
                                            point,
                                            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                        )
                                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                            .build()
                                        activeCam.cameraControl.startFocusAndMetering(action)
                                    }
                                } catch (e: Throwable) {
                                    Log.w("QrScanner", "Tap-to-focus error", e)
                                }
                                v.performClick()
                            }
                            true
                        }
                    } else {
                        mainExecutor.execute {
                            onCameraError("Camera hardware unavailable on this device")
                        }
                    }
                } catch (e: Throwable) {
                    Log.e("QrScanner", "Fatal camera initialization error", e)
                    ContextCompat.getMainExecutor(ctx).execute {
                        onCameraError("Unable to access camera: ${e.localizedMessage ?: "Device error"}")
                    }
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: androidx.camera.core.ImageProxy,
    scanner: BarcodeScanner?,
    isScanning: AtomicBoolean,
    onSuccess: (String) -> Unit
) {
    if (!isScanning.compareAndSet(false, true)) {
        imageProxy.close()
        return
    }

    // Path A: High-speed Google ML Kit detection (if successfully initialized)
    if (scanner != null) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            try {
                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val lineaCode = barcodes.firstOrNull { barcode ->
                            val raw = barcode.rawValue
                            !raw.isNullOrBlank() && QrCodeEngine.isLineaQrCode(raw)
                        }
                        if (lineaCode?.rawValue != null) {
                            onSuccess(lineaCode.rawValue!!)
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                        isScanning.set(false)
                    }
                return
            } catch (e: Throwable) {
                Log.w("QrScanner", "ML Kit frame processing failed; falling back to ZXing", e)
            }
        }
    }

    // Path B: Real-time offline ZXing frame fallback
    // Directly processes the 8-bit Y-plane luminance from CameraX without GMS or Play Services
    try {
        val plane = imageProxy.planes.firstOrNull()
        if (plane != null) {
            val buffer = plane.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val source = com.google.zxing.PlanarYUVLuminanceSource(
                bytes,
                imageProxy.width,
                imageProxy.height,
                0, 0,
                imageProxy.width,
                imageProxy.height,
                false
            )
            val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
            val reader = com.google.zxing.qrcode.QRCodeReader()

            var decodedText: String? = null
            try {
                val result = reader.decode(binaryBitmap)
                if (result != null && QrCodeEngine.isLineaQrCode(result.text)) {
                    decodedText = result.text
                }
            } catch (_: Exception) {
                // Secondary inverted pass for stylish dark mode QR codes
                try {
                    val invertedBitmap = com.google.zxing.BinaryBitmap(
                        com.google.zxing.common.HybridBinarizer(source.invert())
                    )
                    val invertedResult = reader.decode(invertedBitmap)
                    if (invertedResult != null && QrCodeEngine.isLineaQrCode(invertedResult.text)) {
                        decodedText = invertedResult.text
                    }
                } catch (_: Exception) {}
            }

            if (decodedText != null) {
                onSuccess(decodedText)
            }
        }
    } catch (e: Throwable) {
        Log.w("QrScanner", "ZXing frame analyzer error", e)
    } finally {
        imageProxy.close()
        isScanning.set(false)
    }
}

/**
 * Decoded contact result card with Call Now / Save as Contact actions.
 */
@Composable
private fun DecodedContactCard(
    payload: QrCodeEngine.ContactPayload,
    onCall: (String) -> Unit,
    onSave: () -> Unit,
    onScanAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(LineaColors.GlassFill)
            .border(1.dp, LineaColors.GlassBorder, RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success checkmark badge
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(LineaColors.AccentGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                fontSize = 26.sp,
                color = LineaColors.AccentGreen,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Contact Found",
            style = LineaTypography.titleMedium,
            color = LineaColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = payload.name,
            style = LineaTypography.headlineSmall,
            color = LineaColors.TitaniumBlue,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current

        payload.numbers.forEach { number ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Phone Number", number)
                        clipboard.setPrimaryClip(clip)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        Toast.makeText(context, "Copied $number to clipboard", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = number,
                    style = LineaTypography.bodyMedium,
                    color = LineaColors.TextSecondary
                )
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy number",
                    tint = LineaColors.TextTertiary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Call Now
            Button(
                onClick = {
                    payload.numbers.firstOrNull()?.let { onCall(it) }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LineaColors.AccentGreen
                ),
                shape = RoundedCornerShape(14.dp),
                enabled = payload.numbers.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Call Now", fontWeight = FontWeight.SemiBold)
            }

            // Save Contact
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LineaColors.TitaniumBlue
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Copy Number
            OutlinedButton(
                onClick = {
                    val numberToCopy = payload.numbers.firstOrNull() ?: ""
                    if (numberToCopy.isNotBlank()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Phone Number", numberToCopy)
                        clipboard.setPrimaryClip(clip)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        Toast.makeText(context, "Copied $numberToCopy to clipboard", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, LineaColors.GlassBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = LineaColors.GlassFill,
                    contentColor = LineaColors.TextPrimary
                ),
                enabled = payload.numbers.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = LineaColors.TextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Copy",
                    color = LineaColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Scan Again
            OutlinedButton(
                onClick = onScanAgain,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, LineaColors.GlassBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = LineaColors.GlassFill,
                    contentColor = LineaColors.TextSecondary
                )
            ) {
                Text(
                    "Scan Again",
                    color = LineaColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Fast dual-pass ZXing decoder for imported photos that filters specifically for LiNEA QR codes.
 * Decodes standard light-background and inverted dark-mode QR codes.
 * If multiple QR codes exist in the image, it returns the LiNEA QR code.
 */
private fun decodeLineaQrWithZxing(context: android.content.Context, uri: Uri): String? {
    return try {
        val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            android.graphics.ImageDecoder.decodeBitmap(
                android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
            ) { decoder, _, _ ->
                decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        } ?: return null

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
        val multiReader = com.google.zxing.multi.qrcode.QRCodeMultiReader()
        val singleReader = com.google.zxing.qrcode.QRCodeReader()

        // 1. Standard pass
        try {
            val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
            try {
                val results = multiReader.decodeMultiple(binaryBitmap)
                val lineaResult = results.firstOrNull { QrCodeEngine.isLineaQrCode(it.text) }
                if (lineaResult != null) return lineaResult.text
            } catch (_: Exception) {}

            try {
                val singleResult = singleReader.decode(binaryBitmap)
                if (QrCodeEngine.isLineaQrCode(singleResult.text)) return singleResult.text
            } catch (_: Exception) {}
        } catch (_: Exception) {}

        // 2. Inverted pass (for dark mode stylish QR codes)
        try {
            val invertedBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source.invert()))
            try {
                val results = multiReader.decodeMultiple(invertedBitmap)
                val lineaResult = results.firstOrNull { QrCodeEngine.isLineaQrCode(it.text) }
                if (lineaResult != null) return lineaResult.text
            } catch (_: Exception) {}

            try {
                val singleResult = singleReader.decode(invertedBitmap)
                if (QrCodeEngine.isLineaQrCode(singleResult.text)) return singleResult.text
            } catch (_: Exception) {}
        } catch (_: Exception) {}

        null
    } catch (_: Throwable) {
        null
    }
}

