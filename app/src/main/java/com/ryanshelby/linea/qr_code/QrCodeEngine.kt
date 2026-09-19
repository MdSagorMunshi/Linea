package com.ryanshelby.linea.qr_code

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * High-performance QR Code Engine for LiNEA contact sharing.
 *
 * Encodes contacts into universal format and parses scanned text from any
 * standard QR code (LiNEA URI, vCard, MECARD, JSON, plain telephone).
 */
object QrCodeEngine {

    /**
     * Contact data payload.
     */
    data class ContactPayload(
        val name: String,
        val numbers: List<String>
    )

    /**
     * Encode a contact payload into a universal LiNEA URI string.
     * Example: linea://contact?name=Alice+Smith&number=%2B1234567890&number=%2B1987654321
     */
    fun serializeToUri(payload: ContactPayload): String {
        val encodedName = URLEncoder.encode(payload.name, "UTF-8")
        val params = mutableListOf("name=$encodedName")
        payload.numbers.forEach { number ->
            params.add("number=${URLEncoder.encode(number, "UTF-8")}")
        }
        return "linea://contact?${params.joinToString("&")}"
    }

    /**
     * Encode a contact payload into a standard vCard 3.0 string.
     * Compatible with native iOS/Android camera apps and all 3rd-party scanners.
     */
    fun serializeToVCard(payload: ContactPayload): String {
        val sb = StringBuilder()
        sb.append("BEGIN:VCARD\n")
        sb.append("VERSION:3.0\n")
        sb.append("FN:${payload.name}\n")
        payload.numbers.forEach { num ->
            sb.append("TEL;TYPE=CELL:$num\n")
        }
        sb.append("NOTE:LiNEA Contact\n")
        sb.append("END:VCARD")
        return sb.toString()
    }

    /**
     * Generate a standard ZXing BitMatrix for QR Code.
     * Uses ErrorCorrectionLevel.Q (25% recovery) by default for center badge styling.
     */
    fun generateBitMatrix(
        content: String,
        errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.Q
    ): BitMatrix {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to errorCorrectionLevel,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 0
        )
        return QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
    }

    /**
     * Parse scanned QR text into ContactPayload.
     * Supports:
     * 1. LiNEA URI (linea://contact?name=...&number=...)
     * 2. vCard (BEGIN:VCARD...FN:...TEL:...END:VCARD)
     * 3. MECARD (MECARD:N:...;TEL:...;;)
     * 4. JSON ({"name":"...","numbers":["..."]})
     * 5. Plain phone number or tel: URI
     */
    fun parseScannedText(text: String): ContactPayload? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        // 1. LiNEA URI
        if (trimmed.startsWith("linea://contact", ignoreCase = true)) {
            try {
                val query = if (trimmed.contains("?")) trimmed.substringAfter("?") else ""
                var name = ""
                val numbers = mutableListOf<String>()
                for (param in query.split("&")) {
                    val parts = param.split("=")
                    if (parts.size == 2) {
                        val key = URLDecoder.decode(parts[0], "UTF-8")
                        val value = URLDecoder.decode(parts[1], "UTF-8")
                        if (key.equals("name", ignoreCase = true)) {
                            name = value
                        } else if (key.equals("number", ignoreCase = true)) {
                            numbers.add(value)
                        }
                    }
                }
                if (name.isNotEmpty() || numbers.isNotEmpty()) {
                    return ContactPayload(name = name.ifEmpty { "Scanned Contact" }, numbers = numbers)
                }
            } catch (_: Exception) {}
        }

        // 2. vCard format
        if (trimmed.contains("BEGIN:VCARD", ignoreCase = true)) {
            try {
                var name = ""
                val numbers = mutableListOf<String>()
                trimmed.lines().forEach { line ->
                    val clean = line.trim()
                    when {
                        clean.startsWith("FN:", ignoreCase = true) -> {
                            name = clean.substringAfter(":").trim()
                        }
                        clean.startsWith("N:", ignoreCase = true) && name.isEmpty() -> {
                            val parts = clean.substringAfter(":").split(";")
                            val last = parts.getOrNull(0) ?: ""
                            val first = parts.getOrNull(1) ?: ""
                            name = "$first $last".trim()
                        }
                        clean.startsWith("TEL", ignoreCase = true) -> {
                            val num = clean.substringAfter(":").trim()
                            if (num.isNotEmpty()) numbers.add(num)
                        }
                    }
                }
                if (name.isNotEmpty() || numbers.isNotEmpty()) {
                    return ContactPayload(name = name.ifEmpty { "New Contact" }, numbers = numbers)
                }
            } catch (_: Exception) {}
        }

        // 3. MECARD format: MECARD:N:Smith,John;TEL:1234567890;;
        if (trimmed.startsWith("MECARD:", ignoreCase = true)) {
            try {
                var name = ""
                val numbers = mutableListOf<String>()
                val content = trimmed.removePrefix("MECARD:").removePrefix("mecard:")
                for (field in content.split(";")) {
                    val parts = field.split(":")
                    if (parts.size >= 2) {
                        val tag = parts[0].uppercase()
                        val value = parts.drop(1).joinToString(":")
                        when (tag) {
                            "N" -> {
                                name = if (value.contains(",")) {
                                    val nParts = value.split(",")
                                    val last = nParts.getOrNull(0)?.trim() ?: ""
                                    val first = nParts.getOrNull(1)?.trim() ?: ""
                                    "$first $last".trim().ifEmpty { value }
                                } else {
                                    value.trim()
                                }
                            }
                            "TEL" -> if (value.isNotBlank()) numbers.add(value.trim())
                        }
                    }
                }
                if (name.isNotEmpty() || numbers.isNotEmpty()) {
                    return ContactPayload(name = name.ifEmpty { "New Contact" }, numbers = numbers)
                }
            } catch (_: Exception) {}
        }

        // 4. JSON format
        if (trimmed.startsWith("{") && trimmed.contains("\"name\"")) {
            try {
                val nameRegex = Regex("\"name\"\\s*:\\s*\"([^\"]*)\"")
                val name = nameRegex.find(trimmed)?.groupValues?.get(1) ?: ""

                val numbersRegex = Regex("\"numbers\"\\s*:\\s*\\[(.*?)\\]")
                val numbersContent = numbersRegex.find(trimmed)?.groupValues?.get(1) ?: ""
                val numbers = if (numbersContent.isNotBlank()) {
                    Regex("\"([^\"]*)\"").findAll(numbersContent).map { it.groupValues[1] }.toList()
                } else emptyList()

                if (name.isNotEmpty() || numbers.isNotEmpty()) {
                    return ContactPayload(name = name.ifEmpty { "Contact" }, numbers = numbers)
                }
            } catch (_: Exception) {}
        }

        // 5. tel: URI
        if (trimmed.startsWith("tel:", ignoreCase = true)) {
            val number = trimmed.removePrefix("tel:").removePrefix("TEL:").trim()
            if (number.isNotEmpty()) {
                return ContactPayload(name = "New Contact", numbers = listOf(number))
            }
        }

        // 6. Plain phone number or name
        val digitsOnly = trimmed.replace(Regex("[+\\-\\s()\\.]"), "")
        if (digitsOnly.isNotEmpty() && digitsOnly.all { it.isDigit() } && digitsOnly.length >= 3) {
            return ContactPayload(name = "New Contact", numbers = listOf(trimmed))
        }

        return ContactPayload(name = trimmed, numbers = emptyList())
    }
}
