package com.ryanshelby.linea.telecom

import android.view.KeyEvent
import android.view.Window

/** Intercepts call controls before focused views while forwarding all other window callbacks. */
fun Window.installCallKeyInterceptor(intercept: (KeyEvent) -> Boolean) {
    val original = callback ?: return
    callback = object : Window.Callback by original {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean =
            intercept(event) || original.dispatchKeyEvent(event)
    }
}
