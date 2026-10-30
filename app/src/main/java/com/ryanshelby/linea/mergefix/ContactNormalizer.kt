package com.ryanshelby.linea.mergefix

import com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine
import com.ryanshelby.linea.telecom.screening.SimCountryDetector
import java.util.Locale

object ContactNormalizer {

    private val HONORIFICS = setOf(
        // English / International Titles
        "dr", "dr.", "prof", "prof.", "mr", "mr.", "mrs", "mrs.", "ms", "ms.",
        "miss", "master", "sir", "dame", "lady", "lord", "rev", "rev.", "reverend",
        "pastor", "father", "fr", "fr.", "brother", "br", "br.", "sister", "sr", "sr.",
        "rabbi", "cantor", "imam", "sheikh", "shaikh", "mufti",
        "hon", "hon.", "honorable", "judge", "justice", "capt", "capt.", "captain",
        "col", "col.", "colonel", "gen", "gen.", "general", "maj", "maj.", "major",
        "lt", "lt.", "lieutenant", "sgt", "sgt.", "sergeant", "sen", "sen.", "senator",
        "rep", "rep.", "representative", "gov", "gov.", "governor", "pres", "pres.", "president",
        // European (German, French, Spanish, Italian, Portuguese)
        "herr", "frau", "fraulein", "monsieur", "mme", "m.", "mlle",
        "senor", "señor", "senora", "señora", "senorita", "señorita", "sr", "sr.", "sra", "sra.", "srta", "srta.",
        "don", "dona", "sig", "sig.", "signore", "signora", "signorina",
        "dott", "dott.", "dottore", "ing", "ing.", "ingenieur",
        // South Asian / Middle Eastern / Islamic
        "md", "md.", "mohd", "mohd.", "engr", "engr.", "adv", "adv.", "advocate", "barrister",
        "haji", "alhaj", "al-haj", "syed", "sayyid", "ustad", "pandit",
        "shri", "sri", "smt", "kumari", "chaudhary", "chowdhury"
    )

    /**
     * Normalizes phone numbers to standard E.164 format.
     * 100% offline and sovereign across 250+ countries and territories.
     *
     * - Strips whitespace, dashes, parentheses, dots, tabs.
     * - Preserves any international number with leading '+' from any country (+1, +44, +91, +880, +49, +33, etc.).
     * - Converts international exit code '00' to '+'.
     * - Detects and applies the user's active SIM / device country code for domestic un-prefixed numbers.
     * - Supports universal patterns across major countries (e.g. BD 01..., UK 07..., AU 04..., FR 06/07..., NANP 10-digit).
     */
    fun normalizePhoneNumber(rawNumber: String?, defaultCountryIso: String? = null): String {
        if (rawNumber.isNullOrBlank()) return ""
        val cleaned = rawNumber.replace(Regex("[ \\-\\(\\)\\.\\t\\r\\n]"), "")
        if (cleaned.isBlank()) return ""

        // Replace leading 00 with + (international exit code standard)
        val withPlus = if (cleaned.startsWith("00")) {
            "+" + cleaned.substring(2)
        } else {
            cleaned
        }

        // 1. Explicit E.164 with leading +
        if (withPlus.startsWith("+")) {
            val digits = withPlus.substring(1).filter { it.isDigit() }
            return if (digits.isNotEmpty()) "+$digits" else ""
        }

        // Resolve active country from SIM card, subscription, network, or provided override
        val activeIso = defaultCountryIso?.trim()?.uppercase(Locale.ROOT)
            ?: SimCountryDetector.currentCountryIso
        val detectedCountry = OfflineCallerIdEngine.getCountryInfoByIso(activeIso)
        val dialDigits = detectedCountry?.dialCode?.removePrefix("+") ?: "1"

        // 2. Domestic numbers starting with trunk prefix '0'
        if (cleaned.startsWith("0")) {
            // Italy does not drop leading 0 for fixed lines
            if (activeIso == "IT") {
                return "+39$cleaned"
            }

            // If user's active country is specified or non-US, drop trunk 0 and apply active dial code
            if (defaultCountryIso != null || (activeIso != "US" && activeIso != "CA")) {
                val withoutTrunk = cleaned.removePrefix("0")
                if (withoutTrunk.isNotEmpty()) {
                    return "+$dialDigits$withoutTrunk"
                }
            }

            // Universal pattern matching when fallback is US/NANP (where domestic numbers never start with 0):
            // Bangladesh mobile (11 digits): 01[3-9]XXXXXXXX -> +8801[3-9]XXXXXXXX
            if (cleaned.matches(Regex("^01[3-9]\\d{8}$"))) {
                return "+88$cleaned"
            }
            // UK mobile (11 digits): 07XXXXXXXXX -> +447XXXXXXXXX
            if (cleaned.matches(Regex("^07\\d{9}$"))) {
                return "+44" + cleaned.substring(1)
            }
            // Australia mobile (10 digits): 04XXXXXXXX -> +614XXXXXXXX
            if (cleaned.matches(Regex("^04\\d{8}$"))) {
                return "+61" + cleaned.substring(1)
            }
            // France mobile (10 digits): 0[67]XXXXXXXX -> +33[67]XXXXXXXX
            if (cleaned.matches(Regex("^0[67]\\d{8}$"))) {
                return "+33" + cleaned.substring(1)
            }
            // Germany mobile (11-12 digits): 01[5-7]XXXXXXX... -> +491[5-7]...
            if (cleaned.matches(Regex("^01[5-7]\\d{7,9}$"))) {
                return "+49" + cleaned.substring(1)
            }
            // India with trunk 0 (11 digits): 0[6-9]XXXXXXXXX -> +91[6-9]...
            if (cleaned.matches(Regex("^0[6-9]\\d{9}$"))) {
                return "+91" + cleaned.substring(1)
            }

            // General trunk fallback: drop 0 and prepend detected dial code
            val withoutTrunk = cleaned.removePrefix("0")
            if (withoutTrunk.isNotEmpty()) {
                return "+$dialDigits$withoutTrunk"
            }
        }

        // 3. Numbers starting with country dial code digits without plus (e.g. 8801..., 447..., 91..., 1...)
        if (cleaned.startsWith(dialDigits) && cleaned.length >= dialDigits.length + 7) {
            return "+$cleaned"
        }
        if (cleaned.matches(Regex("^8801[3-9]\\d{8}$"))) {
            return "+$cleaned"
        }
        if (cleaned.matches(Regex("^447\\d{9}$"))) {
            return "+$cleaned"
        }
        if (cleaned.matches(Regex("^91[6-9]\\d{9}$"))) {
            return "+$cleaned"
        }
        if (cleaned.matches(Regex("^1[2-9]\\d{9}$"))) {
            return "+$cleaned"
        }

        // 4. North American Plan 10 digits: [2-9]XX[2-9]XXXXXX
        if (cleaned.matches(Regex("^[2-9]\\d{9}$"))) {
            return if (activeIso in setOf("US", "CA")) {
                "+1$cleaned"
            } else {
                "+$dialDigits$cleaned"
            }
        }

        // 5. General fallback: if digits length in 7..15
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

