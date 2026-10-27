package com.ryanshelby.linea.ui.components

internal enum class CrystalOpticsMode { OPAQUE, BLUR, REFRACTION }

/** Android 11 and software canvases retain a readable material without unsupported GPU effects. */
internal fun crystalOpticsMode(sdk: Int, hardwareAccelerated: Boolean, sourceAvailable: Boolean): CrystalOpticsMode = when {
    !hardwareAccelerated || !sourceAvailable || sdk < 31 -> CrystalOpticsMode.OPAQUE
    sdk < 33 -> CrystalOpticsMode.BLUR
    else -> CrystalOpticsMode.REFRACTION
}
