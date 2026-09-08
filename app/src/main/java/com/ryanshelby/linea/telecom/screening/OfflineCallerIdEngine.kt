package com.ryanshelby.linea.telecom.screening

data class CallerIdResult(
    val category: String,       // Emergency, Toll-Free, Country/Region, Mobile, Standard
    val regionOrCountry: String,
    val isEmergency: Boolean = false,
    val isTollFree: Boolean = false,
    val badgeLabel: String
)

object OfflineCallerIdEngine {

    private val EMERGENCY_NUMBERS = setOf(
        "911", "112", "999", "000", "100", "101", "102", "993"
    )

    private val TOLL_FREE_PREFIXES = listOf(
        "+1800", "1800", "800",
        "+1888", "1888", "888",
        "+1877", "1877", "877",
        "+1866", "1866", "866",
        "+1855", "1855", "855",
        "+1844", "1844", "844",
        "+1833", "1833", "833",
        "0800", "0808"
    )

    private val COUNTRY_CODES = listOf(
        "+880" to "Bangladesh",
        "+1" to "United States / Canada",
        "+44" to "United Kingdom",
        "+49" to "Germany",
        "+33" to "France",
        "+91" to "India",
        "+81" to "Japan",
        "+61" to "Australia",
        "+86" to "China",
        "+39" to "Italy",
        "+34" to "Spain",
        "+55" to "Brazil",
        "+7" to "Russia / Kazakhstan",
        "+82" to "South Korea",
        "+65" to "Singapore",
        "+971" to "United Arab Emirates",
        "+966" to "Saudi Arabia",
        "+41" to "Switzerland",
        "+31" to "Netherlands",
        "+46" to "Sweden",
        "+47" to "Norway",
        "+45" to "Denmark",
        "+358" to "Finland",
        "+27" to "South Africa",
        "+62" to "Indonesia",
        "+60" to "Malaysia",
        "+63" to "Philippines",
        "+92" to "Pakistan"
    )

    private val BD_CARRIERS = listOf(
        "017" to "Grameenphone",
        "013" to "Grameenphone",
        "018" to "Robi",
        "016" to "Airtel",
        "019" to "Banglalink",
        "014" to "Banglalink",
        "015" to "Teletalk"
    )

    fun identifyNumber(rawNumber: String): CallerIdResult {
        val clean = rawNumber.replace(Regex("[^0-9+]"), "")

        // 1. Emergency
        if (clean in EMERGENCY_NUMBERS) {
            return CallerIdResult(
                category = "Emergency",
                regionOrCountry = "Emergency Services",
                isEmergency = true,
                badgeLabel = "EMERGENCY"
            )
        }

        // 2. Toll-Free
        for (prefix in TOLL_FREE_PREFIXES) {
            if (clean.startsWith(prefix)) {
                return CallerIdResult(
                    category = "Toll-Free",
                    regionOrCountry = if (prefix.startsWith("080")) "UK Toll-Free" else "Toll-Free Helpline",
                    isTollFree = true,
                    badgeLabel = "TOLL-FREE"
                )
            }
        }

        // 3. Bangladesh Carrier Detection (Local)
        if (clean.startsWith("+880")) {
            val national = clean.substring(4)
            for ((prefix, carrier) in BD_CARRIERS) {
                if (national.startsWith(prefix.removePrefix("0"))) {
                    return CallerIdResult(
                        category = "Mobile Network",
                        regionOrCountry = "Bangladesh • $carrier",
                        badgeLabel = carrier.uppercase()
                    )
                }
            }
            return CallerIdResult(
                category = "International",
                regionOrCountry = "Bangladesh",
                badgeLabel = "BANGLADESH"
            )
        } else if (clean.startsWith("01")) {
            for ((prefix, carrier) in BD_CARRIERS) {
                if (clean.startsWith(prefix)) {
                    return CallerIdResult(
                        category = "Mobile Network",
                        regionOrCountry = "Bangladesh • $carrier",
                        badgeLabel = carrier.uppercase()
                    )
                }
            }
        }

        // 4. Country Code Lookup
        if (clean.startsWith("+")) {
            for ((code, country) in COUNTRY_CODES) {
                if (clean.startsWith(code)) {
                    return CallerIdResult(
                        category = "International",
                        regionOrCountry = country,
                        badgeLabel = country.uppercase()
                    )
                }
            }
        }

        return CallerIdResult(
            category = "Standard",
            regionOrCountry = "Cellular",
            badgeLabel = "CELLULAR"
        )
    }
}
