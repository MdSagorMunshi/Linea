package com.ryanshelby.linea.telecom

import androidx.annotation.DrawableRes
import com.ryanshelby.linea.R
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TimeOfDayState(
    val displayName: String,
    @DrawableRes val iconRes: Int
) {
    EARLY_MORNING("Early Morning", R.drawable.ic_time_early_morning),
    MORNING("Morning", R.drawable.ic_time_morning),
    MIDDAY("Midday", R.drawable.ic_time_midday),
    AFTERNOON("Afternoon", R.drawable.ic_time_afternoon),
    EVENING("Evening", R.drawable.ic_time_evening),
    NIGHT("Night", R.drawable.ic_time_night);

    companion object {
        fun fromHour(hour: Int): TimeOfDayState {
            return when (hour) {
                in 5..7 -> EARLY_MORNING   // 5:00 AM - 7:59 AM
                in 8..11 -> MORNING        // 8:00 AM - 11:59 AM
                in 12..13 -> MIDDAY        // 12:00 PM - 1:59 PM
                in 14..16 -> AFTERNOON     // 2:00 PM - 4:59 PM
                in 17..20 -> EVENING       // 5:00 PM - 8:59 PM
                else -> NIGHT              // 21:00 - 04:59 (9:00 PM - 4:59 AM)
            }
        }
    }
}

data class CountryInfo(
    val isoCode: String,
    val dialCode: String,
    val countryName: String,
    val flagEmoji: String,
    val primaryTimeZone: String
)

data class InternationalPreview(
    val countryName: String,
    val flagEmoji: String,
    val localTimeFormatted: String,
    val timeZoneShort: String,
    val timeOfDay: TimeOfDayState = TimeOfDayState.MORNING,
    val isLateNight: Boolean = (timeOfDay == TimeOfDayState.NIGHT),
    val lateNightWarning: String? = null
)

object InternationalCountryHelper {

    private val countryList = listOf(
        CountryInfo("US", "+1", "United States", "🇺🇸", "America/New_York"),
        CountryInfo("CA", "+1", "Canada", "🇨🇦", "America/Toronto"),
        CountryInfo("GB", "+44", "United Kingdom", "🇬🇧", "Europe/London"),
        CountryInfo("BD", "+880", "Bangladesh", "🇧🇩", "Asia/Dhaka"),
        CountryInfo("IN", "+91", "India", "🇮🇳", "Asia/Kolkata"),
        CountryInfo("PK", "+92", "Pakistan", "🇵🇰", "Asia/Karachi"),
        CountryInfo("AE", "+971", "United Arab Emirates", "🇦🇪", "Asia/Dubai"),
        CountryInfo("SA", "+966", "Saudi Arabia", "🇸🇦", "Asia/Riyadh"),
        CountryInfo("QA", "+974", "Qatar", "🇶🇦", "Asia/Qatar"),
        CountryInfo("KW", "+965", "Kuwait", "🇰🇼", "Asia/Kuwait"),
        CountryInfo("OM", "+968", "Oman", "🇴🇲", "Asia/Muscat"),
        CountryInfo("SG", "+65", "Singapore", "🇸🇬", "Asia/Singapore"),
        CountryInfo("MY", "+60", "Malaysia", "🇲🇾", "Asia/Kuala_Lumpur"),
        CountryInfo("JP", "+81", "Japan", "🇯🇵", "Asia/Tokyo"),
        CountryInfo("KR", "+82", "South Korea", "🇰🇷", "Asia/Seoul"),
        CountryInfo("CN", "+86", "China", "🇨🇳", "Asia/Shanghai"),
        CountryInfo("HK", "+852", "Hong Kong", "🇭🇰", "Asia/Hong_Kong"),
        CountryInfo("AU", "+61", "Australia", "🇦🇺", "Australia/Sydney"),
        CountryInfo("NZ", "+64", "New Zealand", "🇳🇿", "Pacific/Auckland"),
        CountryInfo("DE", "+49", "Germany", "🇩🇪", "Europe/Berlin"),
        CountryInfo("FR", "+33", "France", "🇫🇷", "Europe/Paris"),
        CountryInfo("IT", "+39", "Italy", "🇮🇹", "Europe/Rome"),
        CountryInfo("ES", "+34", "Spain", "🇪🇸", "Europe/Madrid"),
        CountryInfo("NL", "+31", "Netherlands", "🇳🇱", "Europe/Amsterdam"),
        CountryInfo("SE", "+46", "Sweden", "🇸🇪", "Europe/Stockholm"),
        CountryInfo("NO", "+47", "Norway", "🇳🇴", "Europe/Oslo"),
        CountryInfo("DK", "+45", "Denmark", "🇩🇰", "Europe/Copenhagen"),
        CountryInfo("CH", "+41", "Switzerland", "🇨🇭", "Europe/Zurich"),
        CountryInfo("AT", "+43", "Austria", "🇦🇹", "Europe/Vienna"),
        CountryInfo("IE", "+353", "Ireland", "🇮🇪", "Europe/Dublin"),
        CountryInfo("TR", "+90", "Turkey", "🇹🇷", "Europe/Istanbul"),
        CountryInfo("RU", "+7", "Russia", "🇷🇺", "Europe/Moscow"),
        CountryInfo("BR", "+55", "Brazil", "🇧🇷", "America/Sao_Paulo"),
        CountryInfo("MX", "+52", "Mexico", "🇲🇽", "America/Mexico_City"),
        CountryInfo("ZA", "+27", "South Africa", "🇿🇦", "Africa/Johannesburg"),
        CountryInfo("EG", "+20", "Egypt", "🇪🇬", "Africa/Cairo"),
        CountryInfo("NG", "+234", "Nigeria", "🇳🇬", "Africa/Lagos"),
        CountryInfo("KE", "+254", "Kenya", "🇰🇪", "Africa/Nairobi"),
        CountryInfo("ID", "+62", "Indonesia", "🇮🇩", "Asia/Jakarta"),
        CountryInfo("PH", "+63", "Philippines", "🇵🇭", "Asia/Manila"),
        CountryInfo("TH", "+66", "Thailand", "🇹🇭", "Asia/Bangkok"),
        CountryInfo("VN", "+84", "Vietnam", "🇻🇳", "Asia/Ho_Chi_Minh")
    ).sortedByDescending { it.dialCode.length } // Longest prefix match first

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val zoneShortFormatter = DateTimeFormatter.ofPattern("z", Locale.US)

    fun detectCountry(rawNumber: String, userCountryIso: String? = null): InternationalPreview? =
        detectCountryAndLocalTime(rawNumber, userCountryIso)

    fun detectCountryAndLocalTime(rawNumber: String, userCountryIso: String? = null): InternationalPreview? {
        val clean = rawNumber.trim().replace(Regex("[^0-9+]"), "")
        if (clean.length < 2) return null

        val homeIso = userCountryIso?.trim()?.uppercase(Locale.ROOT)
            ?: com.ryanshelby.linea.telecom.screening.SimCountryDetector.currentCountryIso

        val normalized = when {
            clean.startsWith("+") -> clean
            clean.startsWith("00") -> "+" + clean.substring(2)
            else -> {
                // If user is in US/CA and enters a 10-digit domestic NANP number, don't show international preview
                if ((homeIso == "US" || homeIso == "CA") && clean.length == 10 && clean[0] in '2'..'9' &&
                    com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.isNanpAreaCode(clean.substring(0, 3))) {
                    null
                } else if (homeIso == "BD" && clean.startsWith("1") && clean.length in 2..10 && clean[1] in '3'..'9') {
                    // Local domestic Bangladesh mobile
                    "+880" + clean
                } else {
                    // Smart detection without '+': match longest country dial code
                    val match = countryList.firstOrNull { clean.startsWith(it.dialCode.removePrefix("+")) }
                    if (match != null) {
                        "+" + clean
                    } else {
                        null
                    }
                }
            }
        } ?: return null

        // Need at least the dial code to show preview
        val matchedCountry = countryList.firstOrNull { normalized.startsWith(it.dialCode) } ?: return null

        // Only show if user has typed at least the dial code
        if (normalized.length < matchedCountry.dialCode.length) {
            return null
        }

        return try {
            val zoneId = ZoneId.of(matchedCountry.primaryTimeZone)
            val zonedDateTime = ZonedDateTime.now(zoneId)
            val timeString = zonedDateTime.format(timeFormatter)
            val zoneShort = zonedDateTime.format(zoneShortFormatter)

            val hour = zonedDateTime.hour
            val timeOfDay = TimeOfDayState.fromHour(hour)
            val isLateNight = timeOfDay == TimeOfDayState.NIGHT
            val lateNightWarning = if (isLateNight) "🌙 Night in ${matchedCountry.countryName}" else null

            InternationalPreview(
                countryName = matchedCountry.countryName,
                flagEmoji = matchedCountry.flagEmoji,
                localTimeFormatted = timeString,
                timeZoneShort = zoneShort,
                timeOfDay = timeOfDay,
                isLateNight = isLateNight,
                lateNightWarning = lateNightWarning
            )
        } catch (e: Exception) {
            null
        }
    }
}
