package com.ryanshelby.linea.smartfilter

import com.ryanshelby.linea.data.local.entities.ContactEmailEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object SmartFilterEngine {

    // --- 1. STRING SIMILARITY (JARO-WINKLER) ---

    fun jaroWinklerSimilarity(s1: String, s2: String): Double {
        val a = s1.trim().lowercase(Locale.ROOT)
        val b = s2.trim().lowercase(Locale.ROOT)
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0

        val maxDist = max(a.length, b.length) / 2 - 1
        val aMatches = BooleanArray(a.length)
        val bMatches = BooleanArray(b.length)

        var matches = 0
        for (i in a.indices) {
            val start = max(0, i - maxDist)
            val end = min(i + maxDist + 1, b.length)
            for (j in start until end) {
                if (!bMatches[j] && a[i] == b[j]) {
                    aMatches[i] = true
                    bMatches[j] = true
                    matches++
                    break
                }
            }
        }

        if (matches == 0) return 0.0

        var transpositions = 0
        var k = 0
        for (i in a.indices) {
            if (aMatches[i]) {
                while (!bMatches[k]) k++
                if (a[i] != b[k]) transpositions++
                k++
            }
        }

        val jaro = (matches.toDouble() / a.length +
                matches.toDouble() / b.length +
                (matches - transpositions / 2.0) / matches) / 3.0

        // Winkler prefix bonus (up to 4 chars)
        var prefixLength = 0
        val maxPrefix = min(4, min(a.length, b.length))
        for (i in 0 until maxPrefix) {
            if (a[i] == b[i]) prefixLength++ else break
        }

        val winkler = jaro + prefixLength * 0.1 * (1.0 - jaro)
        return winkler.coerceIn(0.0, 1.0)
    }

    // --- 2. PHONE NUMBER NORMALIZATION ---

    fun normalizePhoneNumber(rawNumber: String): String {
        val digits = rawNumber.filter { it.isDigit() }
        val hasPlus = rawNumber.trim().startsWith("+")
        return when {
            hasPlus -> "+$digits"
            else -> digits
        }
    }

    fun extractCanonicalDigits(rawNumber: String): String {
        val digits = rawNumber.filter { it.isDigit() }
        return when {
            digits.length >= 10 -> digits.takeLast(10)
            digits.length >= 7 -> digits.takeLast(7)
            else -> digits
        }
    }

    fun areNumbersEquivalent(num1: String, num2: String): Boolean {
        if (num1.isBlank() || num2.isBlank()) return false
        val digits1 = num1.filter { it.isDigit() }
        val digits2 = num2.filter { it.isDigit() }
        if (digits1 == digits2) return true

        // Match canonical national suffix (e.g. 10 digits for North America, 9-10 internationally)
        val c1 = extractCanonicalDigits(num1)
        val c2 = extractCanonicalDigits(num2)
        if (c1.isNotEmpty() && c2.isNotEmpty() && c1 == c2) {
            return true
        }

        if (digits1.length >= 7 && digits2.length >= 7) {
            val suffix1 = digits1.takeLast(7)
            val suffix2 = digits2.takeLast(7)
            if (suffix1 == suffix2 && (digits1.endsWith(digits2) || digits2.endsWith(digits1))) {
                return true
            }
        }
        return false
    }

    // --- 3. NAME CLEANUP & CAPITALIZATION ---

    private val nameParticles = setOf("van", "von", "der", "den", "de", "di", "da", "del", "du", "la", "le")
    private val romanNumerals = setOf("ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x", "xi", "xii")
    private val nameSuffixes = setOf("jr.", "jr", "sr.", "sr", "md", "phd", "esq") + romanNumerals

    fun evaluateNameCleanup(rawName: String): NameCleanupItem? {
        val trimmed = rawName.trim()
        if (trimmed.isBlank()) return null

        val hasExtraSpaces = trimmed.contains(Regex("\\s{2,}"))
        val letters = trimmed.filter { it.isLetter() }
        if (letters.isEmpty()) return null

        val isAllCaps = letters.length > 2 && letters.all { it.isUpperCase() }
        val isAllLower = letters.length > 2 && letters.all { it.isLowerCase() }
        val hasStrayPunctuation = trimmed.endsWith(",") || trimmed.endsWith("-") || trimmed.endsWith(";") || trimmed.startsWith("-")

        if (!hasExtraSpaces && !isAllCaps && !isAllLower && !hasStrayPunctuation) {
            return null
        }

        val cleanedName = cleanName(trimmed)
        if (cleanedName == trimmed) return null

        val (type, reason) = when {
            isAllCaps -> CleanupType.ALL_CAPS to "Convert ALL CAPS name to title case"
            isAllLower -> CleanupType.ALL_LOWERCASE to "Capitalize lowercase name"
            hasExtraSpaces -> CleanupType.EXTRA_SPACES to "Remove redundant extra spaces"
            else -> CleanupType.PUNCTUATION_FORMAT to "Clean up stray punctuation"
        }

        return NameCleanupItem(
            contactId = 0L,
            originalName = trimmed,
            proposedName = cleanedName,
            cleanupType = type,
            reason = reason
        )
    }

    fun cleanName(rawName: String): String {
        // Collapse spaces and remove stray punctuation at edges
        val collapsed = rawName.trim().replace(Regex("\\s+"), " ")
            .trimStart('-', ',', ';', ' ')
            .trimEnd('-', ',', ';', ' ')

        val words = collapsed.split(" ")
        val capitalizedWords = words.mapIndexed { index, word ->
            val lower = word.lowercase(Locale.ROOT)
            when {
                // Keep suffixes uppercase/standard
                lower in nameSuffixes -> when {
                    lower in romanNumerals -> lower.uppercase(Locale.ROOT)
                    lower in setOf("jr", "jr.") -> "Jr."
                    lower in setOf("sr", "sr.") -> "Sr."
                    lower == "md" -> "MD"
                    lower == "phd" -> "PhD"
                    lower == "esq" -> "Esq."
                    else -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                }
                // Keep particles lowercase if not the first word
                index > 0 && lower in nameParticles -> lower
                // Handle prefixes like Mc, O'
                lower.startsWith("mc") && lower.length > 2 -> {
                    "Mc" + lower.substring(2).replaceFirstChar { it.titlecase(Locale.ROOT) }
                }
                lower.startsWith("o'") && lower.length > 2 -> {
                    "O'" + lower.substring(2).replaceFirstChar { it.titlecase(Locale.ROOT) }
                }
                else -> {
                    word.lowercase(Locale.ROOT).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                    }
                }
            }
        }
        return capitalizedWords.joinToString(" ")
    }

    // --- 4. CONTACT COMPLETENESS & QUALITY EVALUATION ---

    fun calculateCompletenessScore(profile: ContactProfile): Int {
        var score = 0
        val name = profile.contact.displayName.trim()
        val hasFullName = name.contains(" ") && name.length >= 3
        val hasAnyName = name.isNotBlank() && name != profile.numbers.firstOrNull()?.number

        if (hasFullName) score += 30 else if (hasAnyName) score += 20
        if (profile.numbers.isNotEmpty()) score += 35
        if (profile.emails.isNotEmpty()) score += 15
        if (!profile.contact.photoUri.isNullOrBlank()) score += 10
        if (!profile.contact.company.isNullOrBlank()) score += 5
        if (!profile.contact.notes.isNullOrBlank()) score += 5

        return score.coerceIn(0, 100)
    }

    fun evaluateAddressBookQuality(
        profiles: List<ContactProfile>,
        duplicateMatches: List<DuplicateMatch>,
        cleanupItems: List<NameCleanupItem>,
        duplicateNumberGroups: List<DuplicateNumberGroup>
    ): QualityReport {
        val total = profiles.size
        if (total == 0) {
            return QualityReport(
                overallScore = 100,
                letterGrade = "A+",
                totalContacts = 0,
                duplicatePairsCount = 0,
                nameCleanupsCount = 0,
                incompleteContactsCount = 0,
                duplicateNumbersCount = 0,
                completenessScore = 100,
                consistencyScore = 100,
                hygieneScore = 100,
                recommendations = listOf("Your contact list is empty. Add contacts to begin.")
            )
        }

        val avgCompleteness = profiles.map { calculateCompletenessScore(it) }.average().toInt()
        val incompleteCount = profiles.count { calculateCompletenessScore(it) < 60 }
        val noNumberCount = profiles.count { it.numbers.isEmpty() }

        // Hygiene penalties
        val duplicatePenalty = (duplicateMatches.size * 12).coerceAtMost(40)
        val cleanupPenalty = (cleanupItems.size * 5).coerceAtMost(25)
        val duplicateNumberPenalty = (duplicateNumberGroups.size * 8).coerceAtMost(20)

        val hygieneScore = (100 - (duplicatePenalty + duplicateNumberPenalty)).coerceIn(0, 100)
        val consistencyScore = (100 - cleanupPenalty).coerceIn(0, 100)
        val overallScore = ((avgCompleteness * 0.40) + (hygieneScore * 0.35) + (consistencyScore * 0.25)).toInt().coerceIn(0, 100)

        val letterGrade = when {
            overallScore >= 92 -> "A+"
            overallScore >= 85 -> "A"
            overallScore >= 75 -> "B"
            overallScore >= 65 -> "C"
            else -> "Needs Work"
        }

        val recommendations = mutableListOf<String>()
        if (duplicateMatches.isNotEmpty()) {
            recommendations.add("Merge ${duplicateMatches.size} duplicate contact candidate(s) to remove clutter.")
        }
        if (cleanupItems.isNotEmpty()) {
            recommendations.add("Fix formatting and capitalization for ${cleanupItems.size} contact name(s).")
        }
        if (noNumberCount > 0) {
            recommendations.add("$noNumberCount contact(s) have no phone numbers saved.")
        }
        if (incompleteCount > 0) {
            recommendations.add("$incompleteCount contact(s) are missing emails or details.")
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Address book is in excellent condition! No issues detected.")
        }

        return QualityReport(
            overallScore = overallScore,
            letterGrade = letterGrade,
            totalContacts = total,
            duplicatePairsCount = duplicateMatches.size,
            nameCleanupsCount = cleanupItems.size,
            incompleteContactsCount = incompleteCount,
            duplicateNumbersCount = duplicateNumberGroups.size,
            completenessScore = avgCompleteness,
            consistencyScore = consistencyScore,
            hygieneScore = hygieneScore,
            recommendations = recommendations
        )
    }

    // --- 5. DUPLICATE DETECTION ALGORITHM ---

    fun findDuplicateMatches(
        profiles: List<ContactProfile>,
        config: SmartFilterConfig
    ): List<DuplicateMatch> {
        val matches = mutableListOf<DuplicateMatch>()
        val minConfidence = when (config.sensitivity) {
            MatchSensitivity.STRICT -> 90
            MatchSensitivity.BALANCED -> 75
            MatchSensitivity.AGGRESSIVE -> 60
        }

        val n = profiles.size
        for (i in 0 until n) {
            val a = profiles[i]
            for (j in i + 1 until n) {
                val b = profiles[j]

                val match = evaluatePair(a, b, config)
                if (match != null && match.confidence >= minConfidence) {
                    matches.add(match)
                }
            }
        }

        // Sort by highest confidence first
        return matches.sortedByDescending { it.confidence }
    }

    private fun evaluatePair(
        a: ContactProfile,
        b: ContactProfile,
        config: SmartFilterConfig
    ): DuplicateMatch? {
        val nameA = a.contact.displayName.trim()
        val nameB = b.contact.displayName.trim()
        val nameSimilarity = jaroWinklerSimilarity(nameA, nameB)

        // 1. Check Exact / Equivalent Phone Match
        val sharedNumbers = mutableListOf<String>()
        for (numA in a.numbers) {
            for (numB in b.numbers) {
                if (areNumbersEquivalent(numA.number, numB.number)) {
                    sharedNumbers.add(numA.number)
                }
            }
        }

        // 2. Check Shared Emails
        val sharedEmails = mutableListOf<String>()
        for (emA in a.emails) {
            for (emB in b.emails) {
                if (emA.email.trim().equals(emB.email.trim(), ignoreCase = config.ignoreCaseInEmails)) {
                    sharedEmails.add(emA.email.trim())
                }
            }
        }

        // Signal 1: Shared Phone Number with High Name Similarity (100% confidence)
        if (sharedNumbers.isNotEmpty() && nameSimilarity >= 0.82) {
            return DuplicateMatch(
                id = "${a.contact.id}_${b.contact.id}",
                contactA = a,
                contactB = b,
                confidence = 100,
                matchType = MatchType.EXACT_PHONE,
                explanation = "Identical phone number (${sharedNumbers.first()}) and matching name (${(nameSimilarity * 100).toInt()}% match)",
                matchedValues = sharedNumbers
            )
        }

        // Signal 2: Shared Phone Number with Different Name (e.g. "Mom" vs "Mary") (90% confidence)
        if (sharedNumbers.isNotEmpty()) {
            return DuplicateMatch(
                id = "${a.contact.id}_${b.contact.id}",
                contactA = a,
                contactB = b,
                confidence = 90,
                matchType = MatchType.SHARED_NUMBER_DIFFERENT_NAME,
                explanation = "Same phone number (${sharedNumbers.first()}) saved under '$nameA' and '$nameB'",
                matchedValues = sharedNumbers
            )
        }

        // Signal 3: Shared Email with Similar Name (90% confidence)
        if (sharedEmails.isNotEmpty() && nameSimilarity >= 0.70) {
            return DuplicateMatch(
                id = "${a.contact.id}_${b.contact.id}",
                contactA = a,
                contactB = b,
                confidence = 90,
                matchType = MatchType.SAME_EMAIL,
                explanation = "Shared email address (${sharedEmails.first()}) and similar name",
                matchedValues = sharedEmails
            )
        }

        // Signal 4: Very High Name Similarity (Transposed or First/Last identical)
        val nameWordsA = nameA.lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotBlank() }
        val nameWordsB = nameB.lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotBlank() }
        val isTransposed = nameWordsA.size >= 2 && nameWordsA.size == nameWordsB.size &&
                nameWordsA.toSet() == nameWordsB.toSet()

        if (isTransposed) {
            return DuplicateMatch(
                id = "${a.contact.id}_${b.contact.id}",
                contactA = a,
                contactB = b,
                confidence = 88,
                matchType = MatchType.HIGH_NAME_SIMILARITY,
                explanation = "Identical first and last name components in different order ('$nameA' and '$nameB')",
                matchedValues = listOf(nameA, nameB)
            )
        }

        // Signal 5: Strong Jaro-Winkler Similarity with shared company or additional attributes
        val companyMatches = !a.contact.company.isNullOrBlank() &&
                a.contact.company.equals(b.contact.company, ignoreCase = true)

        if (nameSimilarity >= 0.90) {
            val confidence = if (companyMatches) 85 else 78
            return DuplicateMatch(
                id = "${a.contact.id}_${b.contact.id}",
                contactA = a,
                contactB = b,
                confidence = confidence,
                matchType = MatchType.HIGH_NAME_SIMILARITY,
                explanation = "High name similarity (${(nameSimilarity * 100).toInt()}% match)" +
                        (if (companyMatches) " and identical company '${a.contact.company}'" else ""),
                matchedValues = listOf(nameA, nameB)
            )
        }

        if (nameSimilarity >= 0.80 && companyMatches) {
            return DuplicateMatch(
                id = "${a.contact.id}_${b.contact.id}",
                contactA = a,
                contactB = b,
                confidence = 72,
                matchType = MatchType.NAME_AND_ATTRIBUTE_MATCH,
                explanation = "Similar name (${(nameSimilarity * 100).toInt()}%) and shared company '${a.contact.company}'",
                matchedValues = listOf(nameA, nameB)
            )
        }

        return null
    }

    // --- 6. DUPLICATE PHONE NUMBERS ACROSS CONTACTS ---

    fun findDuplicateNumbersAcrossContacts(profiles: List<ContactProfile>): List<DuplicateNumberGroup> {
        val numberMap = mutableMapOf<String, MutableList<ContactProfile>>()
        val displayMap = mutableMapOf<String, String>()

        for (profile in profiles) {
            for (num in profile.numbers) {
                val canon = extractCanonicalDigits(num.number)
                if (canon.length >= 7) {
                    val list = numberMap.getOrPut(canon) { mutableListOf() }
                    if (list.none { it.contact.id == profile.contact.id }) {
                        list.add(profile)
                    }
                    displayMap.putIfAbsent(canon, num.number)
                }
            }
        }

        return numberMap.filter { it.value.size > 1 }
            .map { (canon, contacts) ->
                DuplicateNumberGroup(
                    normalizedNumber = canon,
                    displayFormattedNumber = displayMap[canon] ?: canon,
                    contacts = contacts
                )
            }
    }

    // --- 7. MERGE PREVIEW GENERATION ---

    fun generateMergePreview(primary: ContactProfile, secondary: ContactProfile): MergePreview {
        // Preferred display name: the one that is cleaner, longer, or has full words
        val nameA = primary.contact.displayName.trim()
        val nameB = secondary.contact.displayName.trim()
        val chosenName = when {
            nameA.contains(" ") && !nameB.contains(" ") -> nameA
            nameB.contains(" ") && !nameA.contains(" ") -> nameB
            nameA.length >= nameB.length -> nameA
            else -> nameB
        }

        val chosenCompany = primary.contact.company?.takeIf { it.isNotBlank() }
            ?: secondary.contact.company?.takeIf { it.isNotBlank() }

        val chosenPhoto = primary.contact.photoUri?.takeIf { it.isNotBlank() }
            ?: secondary.contact.photoUri?.takeIf { it.isNotBlank() }

        // Combine phone numbers without duplicates
        val mergedNumbers = mutableListOf<Pair<String, String>>()
        for (num in primary.numbers) {
            if (mergedNumbers.none { areNumbersEquivalent(it.first, num.number) }) {
                mergedNumbers.add(num.number to num.label)
            }
        }
        for (num in secondary.numbers) {
            if (mergedNumbers.none { areNumbersEquivalent(it.first, num.number) }) {
                mergedNumbers.add(num.number to num.label)
            }
        }

        // Combine emails without duplicates
        val mergedEmails = mutableListOf<String>()
        for (em in primary.emails) {
            if (em.email.isNotBlank() && mergedEmails.none { it.equals(em.email.trim(), ignoreCase = true) }) {
                mergedEmails.add(em.email.trim())
            }
        }
        for (em in secondary.emails) {
            if (em.email.isNotBlank() && mergedEmails.none { it.equals(em.email.trim(), ignoreCase = true) }) {
                mergedEmails.add(em.email.trim())
            }
        }

        // Combine notes
        val notesParts = listOfNotNull(
            primary.contact.notes?.takeIf { it.isNotBlank() },
            secondary.contact.notes?.takeIf { it.isNotBlank() }
        ).distinct()
        val mergedNotes = if (notesParts.isNotEmpty()) notesParts.joinToString("\n---\n") else null

        return MergePreview(
            primaryContact = primary,
            secondaryContact = secondary,
            proposedDisplayName = chosenName,
            proposedCompany = chosenCompany,
            proposedPhotoUri = chosenPhoto,
            mergedNumbers = mergedNumbers,
            mergedEmails = mergedEmails,
            mergedNotes = mergedNotes,
            explanation = "Merge will combine ${mergedNumbers.size} phone number(s) and ${mergedEmails.size} email(s) into '$chosenName'."
        )
    }
}
