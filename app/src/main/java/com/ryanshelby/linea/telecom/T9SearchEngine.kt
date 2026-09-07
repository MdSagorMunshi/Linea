package com.ryanshelby.linea.telecom

data class T9Contact(
    val id: Long,
    val displayName: String,
    val phoneNumber: String,
    val photoUri: String? = null
)

data class T9SearchResult(
    val contact: T9Contact,
    val matchedName: Boolean,
    val matchStartIndex: Int,
    val matchEndIndex: Int
)

object T9SearchEngine {

    private val charToDigitMap = mapOf(
        'a' to '2', 'b' to '2', 'c' to '2',
        'd' to '3', 'e' to '3', 'f' to '3',
        'g' to '4', 'h' to '4', 'i' to '4',
        'j' to '5', 'k' to '5', 'l' to '5',
        'm' to '6', 'n' to '6', 'o' to '6',
        'p' to '7', 'q' to '7', 'r' to '7', 's' to '7',
        't' to '8', 'u' to '8', 'v' to '8',
        'w' to '9', 'x' to '9', 'y' to '9', 'z' to '9'
    )

    fun convertNameToDigits(name: String): String {
        val sb = StringBuilder()
        for (ch in name.lowercase()) {
            val digit = charToDigitMap[ch]
            if (digit != null) {
                sb.append(digit)
            } else if (ch.isDigit()) {
                sb.append(ch)
            } else {
                sb.append(' ')
            }
        }
        return sb.toString()
    }

    fun search(contacts: List<T9Contact>, query: String): List<T9SearchResult> {
        val cleanQuery = query.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (cleanQuery.isEmpty()) return emptyList()

        val results = mutableListOf<T9SearchResult>()

        for (contact in contacts) {
            // 1. Check Name via T9 mapping
            val nameWords = contact.displayName.split(" ")
            var nameMatchFound = false

            // Check full name digit mapping
            val nameDigits = convertNameToDigits(contact.displayName)
            val nameMatchIndex = nameDigits.indexOf(cleanQuery)
            if (nameMatchIndex >= 0) {
                results.add(
                    T9SearchResult(
                        contact = contact,
                        matchedName = true,
                        matchStartIndex = nameMatchIndex,
                        matchEndIndex = nameMatchIndex + cleanQuery.length
                    )
                )
                nameMatchFound = true
            } else {
                // Check word starts in name (e.g. "R" in "Rahim S...")
                for (word in nameWords) {
                    val wordDigits = convertNameToDigits(word)
                    if (wordDigits.startsWith(cleanQuery)) {
                        val wordStart = contact.displayName.indexOf(word, ignoreCase = true)
                        if (wordStart >= 0) {
                            results.add(
                                T9SearchResult(
                                    contact = contact,
                                    matchedName = true,
                                    matchStartIndex = wordStart,
                                    matchEndIndex = wordStart + cleanQuery.length
                                )
                            )
                            nameMatchFound = true
                            break
                        }
                    }
                }
            }

            // 2. Check Number substring if name didn't already match
            if (!nameMatchFound) {
                val cleanNumber = contact.phoneNumber.filter { it.isDigit() || it == '+' }
                val numMatchIndex = cleanNumber.indexOf(cleanQuery)
                if (numMatchIndex >= 0) {
                    results.add(
                        T9SearchResult(
                            contact = contact,
                            matchedName = false,
                            matchStartIndex = numMatchIndex,
                            matchEndIndex = numMatchIndex + cleanQuery.length
                        )
                    )
                }
            }
        }

        return results.take(8) // Top matches
    }
}
