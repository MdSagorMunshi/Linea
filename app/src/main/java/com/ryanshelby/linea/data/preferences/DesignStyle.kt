package com.ryanshelby.linea.data.preferences

/** Independent of light/dark mode so changing a design never resets color preferences. */
enum class DesignStyle {
    CLASSIC, CRYSTAL;

    companion object {
        fun fromStored(value: String?): DesignStyle = entries.firstOrNull { it.name == value } ?: CLASSIC
    }
}
