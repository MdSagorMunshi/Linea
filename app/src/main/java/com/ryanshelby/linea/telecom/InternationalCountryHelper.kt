package com.ryanshelby.linea.telecom

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val isLateNight: Boolean,
    val lateNightWarning: String?
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

    fun detectCountry(rawNumber: String): InternationalPreview? = detectCountryAndLocalTime(rawNumber)

    fun detectCountryAndLocalTime(rawNumber: String): InternationalPreview? {
        val clean = rawNumber.trim().replace(" ", "").replace("-", "")
        if (!clean.startsWith("+") && !clean.startsWith("00")) {
            return null
        }

        val normalized = if (clean.startsWith("00")) "+" + clean.substring(2) else clean

        // Need at least dial code + 1 digit to show preview
        val matchedCountry = countryList.firstOrNull { normalized.startsWith(it.dialCode) } ?: return null

        // Only show if user has started typing the number after dial code
        if (normalized.length <= matchedCountry.dialCode.length) {
            return null
        }

        return try {
            val zoneId = ZoneId.of(matchedCountry.primaryTimeZone)
            val zonedDateTime = ZonedDateTime.now(zoneId)
            val timeString = zonedDateTime.format(timeFormatter)
            val zoneShort = zonedDateTime.format(zoneShortFormatter)

            val hour = zonedDateTime.hour
            val isLateNight = hour in 22..23 || hour in 0..6
            val lateNightWarning = if (isLateNight) "🌙 Night in ${matchedCountry.countryName}" else null

            InternationalPreview(
                countryName = matchedCountry.countryName,
                flagEmoji = matchedCountry.flagEmoji,
                localTimeFormatted = timeString,
                timeZoneShort = zoneShort,
                isLateNight = isLateNight,
                lateNightWarning = lateNightWarning
            )
        } catch (e: Exception) {
            null
        }
    }
}
