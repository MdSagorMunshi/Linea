package com.ryanshelby.linea.mergefix

object ContactFixEngine {

    /**
     * Scans contacts for all non-duplicate issues.
     * Enforces the hard rule: vault contacts (isPrivate == true) are completely isolated
     * and never included in any scan or suggestion.
     */
    fun scanForFixIssues(contacts: List<NormalizedContact>): List<FixIssue> {
        val publicContacts = contacts.filter { !it.isPrivate }
        val issues = mutableListOf<FixIssue>()

        for (c in publicContacts) {
            val name = c.displayName.trim()
            val hasNumbers = c.numbers.isNotEmpty()
            val hasEmails = c.emails.isNotEmpty()

            // 1. Unnamed contact: no display name, but has phone number
            if (name.isBlank() && hasNumbers) {
                issues.add(FixIssue.Unnamed(c))
            }

            // 2. Malformed name contact: display name contains only a phone number
            if (name.isNotBlank()) {
                val digitsOnly = name.replace(Regex("[ \\-\\(\\)\\+\\.]"), "")
                if (digitsOnly.isNotEmpty() && digitsOnly.all { it.isDigit() } && digitsOnly.length >= 5) {
                    issues.add(FixIssue.MalformedName(c, extractedNumber = name))
                }
            }

            // 3. Duplicate phone numbers within a single contact record
            val seenNumbers = mutableSetOf<String>()
            val duplicateNumbers = mutableListOf<String>()
            for (num in c.numbers) {
                val norm = num.normalizedNumber.ifBlank { ContactNormalizer.normalizePhoneNumber(num.number) }
                if (norm.isNotBlank()) {
                    if (!seenNumbers.add(norm)) {
                        duplicateNumbers.add(num.number)
                    }
                }
            }
            if (duplicateNumbers.isNotEmpty()) {
                issues.add(FixIssue.DuplicateNumberInRecord(c, duplicateNumbers.first()))
            }

            // 4. Unnormalized numbers: any phone number not in standard E.164 format
            for (num in c.numbers) {
                if (!ContactNormalizer.isE164(num.number)) {
                    val proposed = ContactNormalizer.normalizePhoneNumber(num.number)
                    if (proposed.isNotBlank() && proposed != num.number) {
                        issues.add(FixIssue.UnnormalizedNumber(c, rawNumber = num.number, proposedE164 = proposed))
                    }
                }
            }

            // 5. Empty contact: no phone number, no email, no notes, no address
            val hasNotes = !c.contact.notes.isNullOrBlank()
            val hasAddress = !c.contact.address.isNullOrBlank()
            if (!hasNumbers && !hasEmails && !hasNotes && !hasAddress) {
                issues.add(FixIssue.EmptyContact(c))
            }
        }

        return issues
    }
}

