package com.ryanshelby.linea.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ryanshelby.linea.telecom.ActiveCallInfo
import com.ryanshelby.linea.telecom.CallManager
import com.ryanshelby.linea.ui.incall.InCallActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    init {
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

@Singleton
class FloatingCallOverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callManagerProvider: Provider<CallManager>
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private val currentCallFlow = MutableStateFlow<ActiveCallInfo?>(null)
    private var isOverlayAttached = false

    fun canDrawOverlay(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun show(callInfo: ActiveCallInfo) {
        if (!canDrawOverlay()) return

        mainHandler.post {
            currentCallFlow.value = callInfo

            if (isOverlayAttached && composeView != null) {
                return@post
            }

            try {
                val owner = OverlayLifecycleOwner()
                val view = ComposeView(context).apply {
                    setViewTreeLifecycleOwner(owner)
                    setViewTreeSavedStateRegistryOwner(owner)
                    setViewTreeViewModelStoreOwner(owner)

                    setContent {
                        val activeInfo by currentCallFlow.collectAsState()
                        DontInterruptMeBanner(
                            callInfo = activeInfo,
                            onAnswer = {
                                callManagerProvider.get().answerCall()
                                dismiss()
                                openInCallScreen()
                            },
                            onReject = {
                                callManagerProvider.get().rejectCall()
                                dismiss()
                            },
                            onIgnore = {
                                callManagerProvider.get().silenceRinger()
                                dismiss()
                            },
                            onMessage = {
                                val number = activeInfo?.phoneNumber ?: ""
                                callManagerProvider.get().rejectCall(
                                    rejectWithMessage = true,
                                    textMessage = "I am busy, will call you back."
                                )
                                dismiss()
                                openSmsReply(number)
                            },
                            onExpand = {
                                dismiss()
                                openInCallScreen()
                            }
                        )
                    }
                }

                val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutFlag,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = 0
                }

                windowManager.addView(view, params)
                composeView = view
                lifecycleOwner = owner
                isOverlayAttached = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun update(callInfo: ActiveCallInfo) {
        mainHandler.post {
            currentCallFlow.value = callInfo
        }
    }

    fun dismiss() {
        mainHandler.post {
            currentCallFlow.value = null
            if (isOverlayAttached && composeView != null) {
                try {
                    windowManager.removeView(composeView)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    composeView = null
                    lifecycleOwner?.destroy()
                    lifecycleOwner = null
                    isOverlayAttached = false
                }
            }
        }
    }

    private fun openInCallScreen() {
        try {
            val intent = Intent(context, InCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openSmsReply(phoneNumber: String) {
        if (phoneNumber.isNotBlank()) {
            try {
                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$phoneNumber")
                    putExtra("sms_body", "I am currently busy. I will call you back shortly.")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(smsIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
