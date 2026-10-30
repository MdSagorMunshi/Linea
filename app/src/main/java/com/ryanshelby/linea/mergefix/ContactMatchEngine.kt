package com.ryanshelby.linea.mergefix

object ContactMatchEngine {

    // Hardcoded nickname table for nickname resolution according to specification
    private val NICKNAME_MAP: Map<String, String> = buildMap {
        fun pair(a: String, b: String) {
            put(a.lowercase(), b.lowercase())
            put(b.lowercase(), b.lowercase())
        }
        pair("bob", "robert")
        pair("bobby", "robert")
        pair("rob", "robert")
        pair("mike", "michael")
        pair("mikey", "michael")
        pair("shuvo", "shouvo")
        pair("bill", "william")
        pair("billy", "william")
        pair("will", "william")
        pair("dick", "richard")
        pair("rick", "richard")
        pair("dan", "daniel")
        pair("danny", "daniel")
        pair("jim", "james")
        pair("jimmy", "james")
        pair("dave", "david")
        pair("davy", "david")
        pair("alex", "alexander")
        pair("tom", "thomas")
        pair("tommy", "thomas")
        pair("chris", "christopher")
        pair("tony", "anthony")
        pair("joe", "joseph")
        pair("joey", "joseph")
        pair("sam", "samuel")
        pair("sammy", "samuel")
        pair("ben", "benjamin")
        pair("benny", "benjamin")
        pair("ed", "edward")
        pair("eddie", "edward")
        pair("matt", "matthew")
        pair("matty", "matthew")
        pair("ken", "kenneth")
        pair("kenny", "kenneth")
        pair("steve", "stephen")
        pair("nick", "nicholas")
        pair("ron", "ronald")
        pair("greg", "gregory")
        pair("tim", "timothy")
        pair("ray", "raymond")
        pair("andy", "andrew")
        pair("ammu", "mother")
        pair("mom", "mother")
        pair("ma", "mother")
        pair("abbu", "father")
        pair("dad", "father")
        pair("baba", "father")
    }

    /**
     * Compares two contacts and returns a MergeCandidatePair if they match, or null otherwise.
     * Enforces hard rules:
     * 1. Vault contacts are completely off-limits (isPrivate == true).
     * 2. Dual SIM awareness: if two contacts share an identical normalized phone number but are
     *    associated with different SIM slots, they are NOT flagged as duplicates.
     */
    fun evaluatePair(contactA: NormalizedContact, contactB: NormalizedContact): MergeCandidatePair? {
        // Hard rule 1: Vault contacts are completely off-limits
        if (contactA.isPrivate || contactB.isPrivate) {
            return null
        }

        // Hard rule 2: Dual SIM awareness
        val bothHaveSimSlots = contactA.preferredSimSlot != null && contactB.preferredSimSlot != null
        val differentSimSlots = bothHaveSimSlots && contactA.preferredSimSlot != contactB.preferredSimSlot

        // Check shared normalized phone numbers (only if not on different SIM slots)
        val sharedNumbers = if (differentSimSlots) {
            emptySet()
        } else {
            val numsA = contactA.normalizedNumbers.filter { it.isNotBlank() }.toSet()
            val numsB = contactB.normalizedNumbers.filter { it.isNotBlank() }.toSet()
            numsA.intersect(numsB)
        }
        val hasSharedPhone = sharedNumbers.isNotEmpty()

        // Check shared normalized emails
        val emailsA = contactA.normalizedEmails.filter { it.isNotBlank() }.toSet()
        val emailsB = contactB.normalizedEmails.filter { it.isNotBlank() }.toSet()
        val sharedEmails = emailsA.intersect(emailsB)
        val hasSharedEmail = sharedEmails.isNotEmpty()

        val nameA = contactA.normalizedDisplayName
        val nameB = contactB.normalizedDisplayName
        val bothNamesPresent = nameA.isNotBlank() && nameB.isNotBlank()

        // Exact name match
        val isExactName = bothNamesPresent && nameA == nameB

        // Fuzzy name matching signals
        val isLevenshteinMatch = bothNamesPresent && (levenshteinDistance(nameA, nameB) <= 2)
        val isTokenAbbreviation = bothNamesPresent && isTokenAbbreviationMatch(nameA, nameB)
        val isPhoneticMatch = bothNamesPresent && areNamesPhoneticallyEquivalent(nameA, nameB)
        val isNickname = bothNamesPresent && isNicknameMatch(nameA, nameB)

        val isFuzzyName = isLevenshteinMatch || isTokenAbbreviation || isPhoneticMatch || isNickname

        // --- TIER EVALUATION ---

        if (hasSharedPhone) {
            val phoneDesc = sharedNumbers.first()
            if (isExactName) {
                return MergeCandidatePair(
                    contactA = contactA,
                    contactB = contactB,
                    confidence = ConfidenceTier.HIGH,
                    reason = "Identical normalized phone number ($phoneDesc) and exact name"
                )
            }

            if (!bothNamesPresent) {
                // Nameless duplicates sharing exact phone number
                return MergeCandidatePair(
                    contactA = contactA,
                    contactB = contactB,
                    confidence = ConfidenceTier.HIGH,
                    reason = "Identical normalized phone number ($phoneDesc) with matching or missing name"
                )
            }

            // Name extension / containment (e.g. "Rahim" and "Rahim Uddin" sharing +8801712345678)
            if (isNameExtensionOrPrefix(nameA, nameB)) {
                return MergeCandidatePair(
                    contactA = contactA,
                    contactB = contactB,
                    confidence = ConfidenceTier.HIGH,
                    reason = "Identical phone number ($phoneDesc) and name extension ('$nameA' & '$nameB')"
                )
            }

            // Phonetic match or slight name difference with shared phone -> MEDIUM suggested merge
            val matchType = when {
                isPhoneticMatch -> "Phonetically equivalent name ('$nameA' & '$nameB')"
                isNickname -> "Recognized nickname ('$nameA' & '$nameB')"
                isLevenshteinMatch -> "Similar name variation ('$nameA' & '$nameB')"
                else -> "Different names ('$nameA' & '$nameB')"
            }
            return MergeCandidatePair(
                contactA = contactA,
                contactB = contactB,
                confidence = ConfidenceTier.MEDIUM,
                reason = "Shared phone ($phoneDesc) with $matchType"
            )
        }

        // NO shared phone number:
        if (isExactName) {
            return if (hasSharedEmail) {
                MergeCandidatePair(
                    contactA = contactA,
                    contactB = contactB,
                    confidence = ConfidenceTier.HIGH,
                    reason = "Identical name ('$nameA') and shared email (${sharedEmails.first()})"
                )
            } else {
                MergeCandidatePair(
                    contactA = contactA,
                    contactB = contactB,
                    confidence = ConfidenceTier.HIGH,
                    reason = "Identical name ('$nameA')"
                )
            }
        }

        if (isFuzzyName) {
            if (hasSharedEmail) {
                return MergeCandidatePair(
                    contactA = contactA,
                    contactB = contactB,
                    confidence = ConfidenceTier.MEDIUM,
                    reason = "Similar name ('$nameA' & '$nameB') with shared email (${sharedEmails.first()})"
                )
            } else {
                // Tier 3: Weak signal (names fuzzy-match but share NO phone and NO email, e.g. "Mom" & "Ammu")
                val reasonDetail = when {
                    isNickname -> "Recognized family/nickname match ('$nameA' & '$nameB') with no shared fields"
                    isPhoneticMatch -> "Phonetically similar name ('$nameA' & '$nameB') with no shared fields"
                    isTokenAbbreviation -> "Name abbreviation ('$nameA' & '$nameB') with no shared fields"
                    else -> "Name similarity ('$nameA' & '$nameB') with no shared fields"
                }
                return MergeCandidatePair(
                    contactA = contactA,
                    contactB = contactB,
                    confidence = ConfidenceTier.LOW,
                    reason = reasonDetail
                )
            }
        }

        return null
    }

    /**
     * Standard Levenshtein edit distance algorithm.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val d = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) d[i][0] = i
        for (j in 0..s2.length) d[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                d[i][j] = minOf(
                    d[i - 1][j] + 1,       // deletion
                    d[i][j - 1] + 1,       // insertion
                    d[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return d[s1.length][s2.length]
    }

    /**
     * Standard Soundex algorithm (4-character phonetic code).
     * Maps:
     *   B, F, P, V -> 1
     *   C, G, J, K, Q, S, X, Z -> 2
     *   D, T -> 3
     *   L -> 4
     *   M, N -> 5
     *   R -> 6
     * Examples:
     *   "Jon" -> J500, "John" -> J500
     *   "Mohammad" -> M530, "Muhammed" -> M530
     *   "Shuvo" -> S100, "Shouvo" -> S100
     */
    fun soundex(rawWord: String): String {
        val word = rawWord.filter { it.isLetter() }.uppercase()
        if (word.isEmpty()) return "0000"

        val firstLetter = word.first()
        val builder = StringBuilder().append(firstLetter)

        fun charToCode(c: Char): Char = when (c) {
            'B', 'F', 'P', 'V' -> '1'
            'C', 'G', 'J', 'K', 'Q', 'S', 'X', 'Z' -> '2'
            'D', 'T' -> '3'
            'L' -> '4'
            'M', 'N' -> '5'
            'R' -> '6'
            else -> '0'
        }

        var prevCode = charToCode(firstLetter)

        for (i in 1 until word.length) {
            val code = charToCode(word[i])
            if (code != '0' && code != prevCode) {
                builder.append(code)
                if (builder.length == 4) break
            }
            if (code != '0' || (word[i] != 'H' && word[i] != 'W')) {
                prevCode = code
            }
        }

        while (builder.length < 4) {
            builder.append('0')
        }
        return builder.toString()
    }

    /**
     * Checks if two multi-word names are phonetically equivalent via Soundex.
     */
    fun areNamesPhoneticallyEquivalent(nameA: String, nameB: String): Boolean {
        val tokensA = nameA.split(Regex("\\s+")).filter { it.isNotBlank() }
        val tokensB = nameB.split(Regex("\\s+")).filter { it.isNotBlank() }

        if (tokensA.size != tokensB.size || tokensA.isEmpty()) return false

        for (i in tokensA.indices) {
            val sA = soundex(tokensA[i])
            val sB = soundex(tokensB[i])
            if (sA != sB) return false
        }
        return true
    }

    /**
     * Checks if tokens match an abbreviation or prefix (e.g. "Rahim Uddin" vs "Rahim U." or "John Wick" vs "John W").
     */
    fun isTokenAbbreviationMatch(nameA: String, nameB: String): Boolean {
        val tokensA = nameA.split(Regex("\\s+")).filter { it.isNotBlank() }
        val tokensB = nameB.split(Regex("\\s+")).filter { it.isNotBlank() }

        if (tokensA.isEmpty() || tokensB.isEmpty()) return false
        if (tokensA.size != tokensB.size) return false

        var hasAbbrev = false
        for (i in tokensA.indices) {
            val tA = tokensA[i].removeSuffix(".")
            val tB = tokensB[i].removeSuffix(".")
            if (tA == tB) continue

            // If one is an initial of the other
            if ((tA.length == 1 && tB.startsWith(tA)) || (tB.length == 1 && tA.startsWith(tB))) {
                hasAbbrev = true
                continue
            }

            // Or Levenshtein distance between tokens <= 1
            if (levenshteinDistance(tA, tB) <= 1) {
                hasAbbrev = true
                continue
            }

            return false
        }
        return hasAbbrev
    }

    /**
     * Checks if one name is an extension or prefix abbreviation of the other
     * (e.g. "rahim" and "rahim uddin", or "rahim uddin" and "rahim u.").
     */
    fun isNameExtensionOrPrefix(nameA: String, nameB: String): Boolean {
        if (nameA.isBlank() || nameB.isBlank()) return false
        val tokensA = nameA.split(Regex("\\s+")).filter { it.isNotBlank() }
        val tokensB = nameB.split(Regex("\\s+")).filter { it.isNotBlank() }

        if (tokensA.isEmpty() || tokensB.isEmpty()) return false

        val shorter = if (tokensA.size <= tokensB.size) tokensA else tokensB
        val longer = if (tokensA.size <= tokensB.size) tokensB else tokensA

        // Exact sublist prefix (e.g. ["rahim"] is prefix of ["rahim", "uddin"])
        if (longer.subList(0, shorter.size) == shorter) return true

        // Or all shorter tokens are prefixes/abbreviations of corresponding longer tokens
        if (shorter.size <= longer.size) {
            var allMatch = true
            for (i in shorter.indices) {
                val sToken = shorter[i].removeSuffix(".")
                val lToken = longer[i].removeSuffix(".")
                if (sToken != lToken && !(sToken.length == 1 && lToken.startsWith(sToken))) {
                    allMatch = false
                    break
                }
            }
            if (allMatch) return true
        }

        return false
    }

    /**
     * Checks if names match via the hardcoded nickname dictionary.
     */
    fun isNicknameMatch(nameA: String, nameB: String): Boolean {
        val tokensA = nameA.split(Regex("\\s+")).filter { it.isNotBlank() }
        val tokensB = nameB.split(Regex("\\s+")).filter { it.isNotBlank() }

        if (tokensA.isEmpty() || tokensB.isEmpty()) return false

        // Check single-token matches directly (e.g. "mom" vs "ammu", "bob" vs "robert")
        if (tokensA.size == 1 && tokensB.size == 1) {
            val canonA = NICKNAME_MAP[tokensA[0]] ?: tokensA[0]
            val canonB = NICKNAME_MAP[tokensB[0]] ?: tokensB[0]
            if (canonA == canonB && canonA != tokensA[0] || canonA == tokensB[0] || canonB == tokensA[0]) {
                return true
            }
        }

        // Multi-token names (e.g. "Bob Fox" vs "Robert Fox")
        if (tokensA.size == tokensB.size) {
            var nicknameMatched = false
            for (i in tokensA.indices) {
                val tA = tokensA[i]
                val tB = tokensB[i]
                if (tA == tB) continue

                val canonA = NICKNAME_MAP[tA] ?: tA
                val canonB = NICKNAME_MAP[tB] ?: tB
                if (canonA == canonB || canonA == tB || canonB == tA) {
                    nicknameMatched = true
                } else {
                    return false
                }
            }
            return nicknameMatched
        }

        return false
    }
}

