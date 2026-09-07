package com.ryanshelby.linea.ui.navigation

import androidx.annotation.DrawableRes
import com.ryanshelby.linea.R

enum class LineaDestination(
    val route: String,
    val title: String,
    @DrawableRes val outlineIcon: Int,
    @DrawableRes val filledIcon: Int
) {
    DIALPAD(
        route = "dialpad",
        title = "Dial Pad",
        outlineIcon = R.drawable.ic_dialpad_outline,
        filledIcon = R.drawable.ic_dialpad_filled
    ),
    HISTORY(
        route = "history",
        title = "History",
        outlineIcon = R.drawable.ic_history_outline,
        filledIcon = R.drawable.ic_history_filled
    ),
    CONTACTS(
        route = "contacts",
        title = "Contacts",
        outlineIcon = R.drawable.ic_contacts_outline,
        filledIcon = R.drawable.ic_contacts_filled
    ),
    SETTINGS(
        route = "settings",
        title = "Settings",
        outlineIcon = R.drawable.ic_settings_outline,
        filledIcon = R.drawable.ic_settings_filled
    )
}
