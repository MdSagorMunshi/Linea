package com.ryanshelby.linea.mergefix

import java.util.UUID

object ContactGroupingEngine {

    /**
     * Groups matched contacts into disjoint merge groups of two or more.
     * Enforces the hard rule: a single contact may belong to only one group.
     */
    fun findMergeGroups(contacts: List<NormalizedContact>): List<MergeGroup> {
        // Vault contacts are strictly excluded
        val publicContacts = contacts.filter { !it.isPrivate }
        if (publicContacts.size < 2) return emptyList()

        val contactMap = publicContacts.associateBy { it.id }
        val parent = mutableMapOf<Long, Long>()
        for (c in publicContacts) {
            parent[c.id] = c.id
        }

        fun find(i: Long): Long {
            var root = i
            while (root != (parent[root] ?: root)) {
                root = parent[root] ?: root
            }
            var curr = i
            while (curr != root) {
                val nxt = parent[curr] ?: root
                parent[curr] = root
                curr = nxt
            }
            return root
        }

        fun union(i: Long, j: Long) {
            val rootI = find(i)
            val rootJ = find(j)
            if (rootI != rootJ) {
                parent[rootI] = rootJ
            }
        }

        // Detect all valid pairwise candidate matches
        val pairs = mutableListOf<MergeCandidatePair>()
        for (i in 0 until publicContacts.size) {
            val contactA = publicContacts[i]
            for (j in i + 1 until publicContacts.size) {
                val contactB = publicContacts[j]
                val pair = ContactMatchEngine.evaluatePair(contactA, contactB)
                if (pair != null) {
                    pairs.add(pair)
                    union(contactA.id, contactB.id)
                }
            }
        }

        if (pairs.isEmpty()) return emptyList()

        // Cluster contacts by root component
        val clusters = mutableMapOf<Long, MutableList<NormalizedContact>>()
        for (c in publicContacts) {
            val root = find(c.id)
            clusters.getOrPut(root) { mutableListOf() }.add(c)
        }

        val resultGroups = mutableListOf<MergeGroup>()

        for ((_, memberList) in clusters) {
            // A group must have 2 or more contacts
            if (memberList.size < 2) continue

            val memberIds = memberList.map { it.id }.toSet()
            val relevantPairs = pairs.filter { it.contactA.id in memberIds && it.contactB.id in memberIds }

            // Group confidence calculation:
            // - HIGH: All members share exact normalized phone number or exact normalized name.
            // - MEDIUM: Fuzzy name plus shared phone, or medium confidence links.
            // - LOW: Name-only fuzzy with no shared fields.
            // Never promote a group containing LOW signals to HIGH!
            val confidences = relevantPairs.map { it.confidence }
            val sharedNumbersAcrossGroup = memberList
                .map { it.normalizedNumbers.filter { n -> n.isNotBlank() }.toSet() }
                .reduceOrNull { acc, set -> acc.intersect(set) } ?: emptySet()

            val groupConfidence = when {
                confidences.all { it == ConfidenceTier.HIGH } -> ConfidenceTier.HIGH
                else -> ConfidenceTier.MEDIUM
            }

            val explanations = relevantPairs.map { it.reason }.distinct()
            val explanation = if (explanations.size == 1) {
                explanations.first()
            } else {
                "${memberList.size} contacts grouped: ${explanations.first()}"
            }

            resultGroups.add(
                MergeGroup(
                    id = UUID.randomUUID().toString(),
                    confidence = groupConfidence,
                    contacts = memberList.sortedBy { it.displayName },
                    explanation = explanation
                )
            )
        }

        // Sort: HIGH first, then MEDIUM, then LOW
        return resultGroups.sortedWith(
            compareBy<MergeGroup> {
                when (it.confidence) {
                    ConfidenceTier.HIGH -> 0
                    ConfidenceTier.MEDIUM -> 1
                    ConfidenceTier.LOW -> 2
                }
            }.thenByDescending { it.contacts.size }
        )
    }
}

