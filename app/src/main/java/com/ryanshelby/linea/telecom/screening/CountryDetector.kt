package com.ryanshelby.linea.telecom.screening

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.ryanshelby.linea.telecom.InternationalCountryHelper
import java.util.Locale

data class CountryDetails(
    val countryIso: String,
    val countryName: String,
    val dialCode: String,
    val flagEmoji: String,
    val isSharedCallingCode: Boolean = false,
    val sharedCallingCode: String? = null,
    val formattedE164: String? = null,
    val nationalNumber: String? = null
)

sealed class CountryDetectionResult {
    data class Identified(val details: CountryDetails) : CountryDetectionResult()
    data class Unknown(val reason: String) : CountryDetectionResult()
}

object CountryDetector {

    private val phoneUtil: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

    // Known shared international dialing prefixes
    val SHARED_CALLING_CODES: Map<String, List<String>> = mapOf(
        "+1" to listOf(
            "US", "CA", "AG", "AI", "AS", "BB", "BM", "BS", "DM", "DO",
            "GD", "GU", "JM", "KN", "KY", "LC", "MP", "MS", "PR", "SX",
            "TC", "TT", "VC", "VG", "VI"
        ),
        "+7" to listOf("RU", "KZ"),
        "+44" to listOf("GB", "GG", "IM", "JE"),
        "+61" to listOf("AU", "CC", "CX"),
        "+590" to listOf("GP", "BL", "MF"),
        "+599" to listOf("CW", "BQ"),
        "+212" to listOf("MA", "EH"),
        "+262" to listOf("RE", "YT"),
        "+47" to listOf("NO", "SJ")
    )

    private val PRIVATE_INDICATORS = setOf(
        "private", "unknown", "withheld", "restricted", "anonymous", "unavailable",
        "private number", "unknown caller", "caller id withheld"
    )

    /**
     * Determines whether the given raw caller ID is a private, unknown, or withheld number.
     */
    fun isPrivateOrUnknown(rawNumber: String?): Boolean {
        if (rawNumber.isNullOrBlank()) return true
        val cleanLower = rawNumber.trim().lowercase(Locale.ROOT)
        if (cleanLower in PRIVATE_INDICATORS) return true
        val digitsOnly = rawNumber.filter { it.isDigit() }
        return digitsOnly.isEmpty()
    }

    /**
     * Accurately parses an incoming phone number and resolves its originating country.
     * Uses Google's libphonenumber with fallback to OfflineCallerIdEngine.
     *
     * Handles:
     * - Private, unknown, withheld, or empty numbers -> CountryDetectionResult.Unknown
     * - Unrecognizable numbers (< 2 digits, invalid formats) -> CountryDetectionResult.Unknown
     * - Shared calling codes (e.g. +1 US vs CA vs Caribbean, +7 RU vs KZ)
     */
    fun detectCountry(rawNumber: String?, userSimCountryIso: String? = null): CountryDetectionResult {
        if (isPrivateOrUnknown(rawNumber)) {
            return CountryDetectionResult.Unknown("Private or withheld caller ID")
        }

        val clean = rawNumber!!.trim().replace(Regex("[^0-9+]"), "")
        val digits = clean.filter { it.isDigit() }
        if (digits.length < 2) {
            return CountryDetectionResult.Unknown("Insufficient digits to identify country")
        }

        val defaultRegion = userSimCountryIso?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotBlank() }
            ?: SimCountryDetector.currentCountryIso.uppercase(Locale.ROOT)

        // 1. Try parsing with Google libphonenumber
        val parsedNumber = tryParseWithLibPhoneNumber(clean, defaultRegion)
        if (parsedNumber != null) {
            val region = phoneUtil.getRegionCodeForNumber(parsedNumber)
            val countryCode = parsedNumber.countryCode
            val dialCode = "+$countryCode"

            if (!region.isNullOrBlank() && region != "ZZ" && region != "001") {
                val resolvedDetails = resolveCountryDetails(
                    iso = region,
                    dialCode = dialCode,
                    formattedE164 = phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164),
                    nationalNumber = parsedNumber.nationalNumber.toString()
                )
                if (resolvedDetails != null) {
                    return CountryDetectionResult.Identified(resolvedDetails)
                }
            }
        }

        // 2. Fallback resolution using OfflineCallerIdEngine & InternationalCountryHelper
        val fallback = detectWithOfflineCatalog(clean, defaultRegion)
        if (fallback != null) {
            return CountryDetectionResult.Identified(fallback)
        }

        return CountryDetectionResult.Unknown("Country could not be determined reliably")
    }

    private fun tryParseWithLibPhoneNumber(clean: String, defaultRegion: String): Phonenumber.PhoneNumber? {
        val numberToParse = when {
            clean.startsWith("+") -> clean
            clean.startsWith("00") -> "+" + clean.substring(2)
            else -> clean
        }

        return try {
            val parsed = phoneUtil.parse(numberToParse, defaultRegion)
            if (phoneUtil.isValidNumber(parsed) || phoneUtil.isPossibleNumber(parsed)) {
                parsed
            } else {
                // If parsing without + in defaultRegion failed, try prepending '+' if it might be international
                if (!numberToParse.startsWith("+")) {
                    try {
                        val parsedWithPlus = phoneUtil.parse("+$numberToParse", null)
                        if (phoneUtil.isValidNumber(parsedWithPlus) || phoneUtil.isPossibleNumber(parsedWithPlus)) {
                            parsedWithPlus
                        } else parsed
                    } catch (_: Exception) {
                        parsed
                    }
                } else parsed
            }
        } catch (_: Exception) {
            if (!clean.startsWith("+")) {
                try {
                    phoneUtil.parse("+$clean", null)
                } catch (_: Exception) {
                    null
                }
            } else null
        }
    }

    private fun detectWithOfflineCatalog(clean: String, defaultRegion: String): CountryDetails? {
        val callerId = OfflineCallerIdEngine.identifyNumber(clean, defaultRegion)
        val countryName = callerId.countryName ?: callerId.regionOrCountry.split("•").lastOrNull()?.trim()

        if (!countryName.isNullOrBlank() && countryName != "Cellular" && countryName != "Emergency Services") {
            // Find country in catalog
            val allCountries = InternationalCountryHelper.getAllCountries()
            val matched = allCountries.firstOrNull { it.countryName.equals(countryName, ignoreCase = true) }
                ?: allCountries.firstOrNull { clean.startsWith(it.dialCode) || clean.startsWith(it.dialCode.removePrefix("+")) }

            if (matched != null) {
                // Special check for NANP (+1) area codes
                val finalIso = when {
                    matched.dialCode == "+1" -> {
                        val digits = clean.removePrefix("+").removePrefix("1")
                        if (digits.length >= 3 && OfflineCallerIdEngine.isCanadianAreaCode(digits.substring(0, 3))) {
                            "CA"
                        } else {
                            "US"
                        }
                    }
                    matched.dialCode == "+7" -> {
                        val digits = clean.removePrefix("+").removePrefix("7")
                        if (digits.startsWith("6") || digits.startsWith("7")) "KZ" else "RU"
                    }
                    else -> matched.isoCode
                }

                return resolveCountryDetails(
                    iso = finalIso,
                    dialCode = matched.dialCode,
                    formattedE164 = if (clean.startsWith("+")) clean else matched.dialCode + clean.removePrefix("0")
                )
            }
        }
        return null
    }

    /**
     * Resolves metadata (flag, name, shared code status) for a given ISO.
     */
    fun resolveCountryDetails(
        iso: String,
        dialCode: String? = null,
        formattedE164: String? = null,
        nationalNumber: String? = null
    ): CountryDetails? {
        val cleanIso = iso.trim().uppercase(Locale.ROOT)
        val info = InternationalCountryHelper.getCountryByIso(cleanIso)
            ?: OfflineCallerIdEngine.getCountryInfoByIso(cleanIso)?.let {
                com.ryanshelby.linea.telecom.CountryInfo(
                    isoCode = it.iso,
                    dialCode = it.dialCode,
                    countryName = it.name,
                    flagEmoji = it.flag,
                    primaryTimeZone = "UTC"
                )
            } ?: return null

        val effectiveDialCode = dialCode ?: info.dialCode
        val isShared = SHARED_CALLING_CODES.containsKey(effectiveDialCode)

        return CountryDetails(
            countryIso = cleanIso,
            countryName = info.countryName,
            dialCode = effectiveDialCode,
            flagEmoji = info.flagEmoji,
            isSharedCallingCode = isShared,
            sharedCallingCode = if (isShared) effectiveDialCode else null,
            formattedE164 = formattedE164,
            nationalNumber = nationalNumber
        )
    }

    /**
     * Returns a deduplicated catalog of all 248 countries for the country picker.
     */
    fun getAllCatalogCountries(): List<CountryDetails> {
        val helperList = InternationalCountryHelper.getAllCountries()
        return helperList.map { item ->
            val isShared = SHARED_CALLING_CODES.containsKey(item.dialCode)
            CountryDetails(
                countryIso = item.isoCode,
                countryName = item.countryName,
                dialCode = item.dialCode,
                flagEmoji = item.flagEmoji,
                isSharedCallingCode = isShared,
                sharedCallingCode = if (isShared) item.dialCode else null
            )
        }.distinctBy { it.countryIso }
            .sortedBy { it.countryName }
    }

    /**
     * Returns the list of territory names and ISOs sharing a calling code.
     */
    fun getSharedTerritoriesForDialCode(dialCode: String): List<CountryDetails> {
        val isos = SHARED_CALLING_CODES[dialCode] ?: return emptyList()
        val all = getAllCatalogCountries()
        return all.filter { it.countryIso in isos }
    }
}
