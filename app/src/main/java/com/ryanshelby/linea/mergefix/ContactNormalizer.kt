package com.ryanshelby.linea.mergefix

object ContactNormalizer {

    private val HONORIFICS = setOf(
        "dr", "dr.", "prof", "prof.", "mr", "mr.", "mrs", "mrs.", "ms", "ms.",
        "miss", "md", "md.", "engr", "engr.", "adv", "adv.", "barrister",
        "haji", "alhaj", "sheikh", "rev", "rev.", "sir"
    )

    /**
     * Normalizes phone numbers:
     * - Strips spaces, dashes, parentheses, dots, tabs.
     * - Converts to standard E.164 format.
     * For example:
     *   "01712345678" -> "+8801712345678"
     *   "01712-345-678" -> "+8801712345678"
     *   "+8801712345678" -> "+8801712345678"
     *   "008801712345678" -> "+8801712345678"
     */
    fun normalizePhoneNumber(rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) return ""
        val cleaned = rawNumber.replace(Regex("[ \\-\\(\\)\\.\\t\\r\\n]"), "")
        if (cleaned.isBlank()) return ""

        // Replace leading 00 with +
        val withPlus = if (cleaned.startsWith("00")) {
            "+" + cleaned.substring(2)
        } else {
            cleaned
        }

        if (withPlus.startsWith("+")) {
            val digits = withPlus.substring(1).filter { it.isDigit() }
            return if (digits.isNotEmpty()) "+$digits" else ""
        }

        // Bangladesh domestic format: 01[3-9]XXXXXXXX (11 digits) -> +8801[3-9]XXXXXXXX
        if (cleaned.matches(Regex("^01[3-9]\\d{8}$"))) {
            return "+88$cleaned"
        }

        // Bangladesh with country code prefix without plus: 8801[3-9]XXXXXXXX (13 digits)
        if (cleaned.matches(Regex("^8801[3-9]\\d{8}$"))) {
            return "+$cleaned"
        }

        // North American 10-digit number: [2-9]XX[2-9]XXXXXX
        if (cleaned.matches(Regex("^[2-9]\\d{9}$"))) {
            return "+1$cleaned"
        }

        // North American 11-digit number with leading 1
        if (cleaned.matches(Regex("^1[2-9]\\d{9}$"))) {
            return "+$cleaned"
        }

        // International standard digits length (7 to 15 digits) without +
        val digitsOnly = cleaned.filter { it.isDigit() }
        return if (digitsOnly.length in 7..15) {
            "+$digitsOnly"
        } else {
            digitsOnly
        }
    }

    /**
     * Checks if a phone number conforms strictly to E.164 format (+ followed by 7-15 digits).
     */
    fun isE164(number: String?): Boolean {
        if (number.isNullOrBlank()) return false
        return number.matches(Regex("^\\+[1-9]\\d{6,14}$"))
    }

    /**
     * Normalizes display names:
     * - Lowercase, trim whitespace.
     * - Removes honorifics (e.g. "Dr. Rahim Uddin" -> "rahim uddin").
     * - Collapses multiple spaces.
     */
    fun normalizeDisplayName(rawName: String?): String {
        if (rawName.isNullOrBlank()) return ""
        val lower = rawName.trim().lowercase()
        val tokens = lower.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""

        // Check if first token is an honorific
        val firstToken = tokens.first()
        val withoutHonorific = if (HONORIFICS.contains(firstToken) || HONORIFICS.contains(firstToken.removeSuffix("."))) {
            tokens.drop(1)
        } else {
            tokens
        }

        return withoutHonorific.joinToString(" ")
    }

    /**
     * Normalizes email: lowercase, trim whitespace.
     */
    fun normalizeEmail(rawEmail: String?): String {
        if (rawEmail.isNullOrBlank()) return ""
        return rawEmail.trim().lowercase()
    }
}

