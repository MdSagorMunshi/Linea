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
     * Contact data payload with rich profile details.
     */
    data class ContactPayload(
        val name: String,
        val numbers: List<String>,
        val email: String? = null,
        val photoThumbnailBase64: String? = null,
        val social1Platform: String? = null,
        val social1Handle: String? = null,
        val social2Platform: String? = null,
        val social2Handle: String? = null
    )

    /**
     * Encode a contact payload into a universal LiNEA URI string.
     * Example: linea://contact?name=Alice+Smith&number=%2B1234567890&email=alice%40example.com&s1p=INSTAGRAM&s1h=alice
     */
    fun serializeToUri(payload: ContactPayload): String {
        val encodedName = URLEncoder.encode(payload.name, "UTF-8")
        val params = mutableListOf("name=$encodedName")
        payload.numbers.forEach { number ->
            params.add("number=${URLEncoder.encode(number, "UTF-8")}")
        }
        if (!payload.email.isNullOrBlank()) {
            params.add("email=${URLEncoder.encode(payload.email, "UTF-8")}")
        }
        if (!payload.photoThumbnailBase64.isNullOrBlank()) {
            params.add("photo=${URLEncoder.encode(payload.photoThumbnailBase64, "UTF-8")}")
        }
        if (!payload.social1Platform.isNullOrBlank() && !payload.social1Handle.isNullOrBlank()) {
            params.add("s1p=${URLEncoder.encode(payload.social1Platform, "UTF-8")}")
            params.add("s1h=${URLEncoder.encode(payload.social1Handle, "UTF-8")}")
        }
        if (!payload.social2Platform.isNullOrBlank() && !payload.social2Handle.isNullOrBlank()) {
            params.add("s2p=${URLEncoder.encode(payload.social2Platform, "UTF-8")}")
            params.add("s2h=${URLEncoder.encode(payload.social2Handle, "UTF-8")}")
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
        if (!payload.email.isNullOrBlank()) {
            sb.append("EMAIL;TYPE=INTERNET:${payload.email}\n")
        }
        if (!payload.social1Platform.isNullOrBlank() && !payload.social1Handle.isNullOrBlank()) {
            sb.append("X-SOCIAL-1:${payload.social1Platform}:${payload.social1Handle}\n")
        }
        if (!payload.social2Platform.isNullOrBlank() && !payload.social2Handle.isNullOrBlank()) {
            sb.append("X-SOCIAL-2:${payload.social2Platform}:${payload.social2Handle}\n")
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
     * Determines whether a scanned QR code text is an authentic LiNEA QR code.
     * Only codes with the LiNEA URI scheme (linea://contact or linea://) or
     * LiNEA-branded vCards (containing LiNEA Contact note) are identified as LiNEA QR codes.
     * All random QR codes (URLs, Wi-Fi, barcodes, generic text) return false.
     */
    fun isLineaQrCode(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val trimmed = text.trim()
        if (trimmed.startsWith("linea://contact", ignoreCase = true) ||
            trimmed.startsWith("linea://", ignoreCase = true)
        ) {
            return true
        }
        if (trimmed.contains("BEGIN:VCARD", ignoreCase = true) && trimmed.contains("LiNEA", ignoreCase = true)) {
            return true
        }
        return false
    }

    /**
     * Strictly parse a LiNEA QR code into ContactPayload.
     * Returns null if the scanned text is not a LiNEA QR code or cannot be parsed.
     */
    fun parseLineaScannedText(text: String?): ContactPayload? {
        if (!isLineaQrCode(text)) return null
        return parseScannedText(text!!)
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

        // 1. LiNEA URI (linea://contact?name=... or linea://...)
        if (trimmed.startsWith("linea://", ignoreCase = true)) {
            try {
                val query = if (trimmed.contains("?")) trimmed.substringAfter("?") else ""
                var name = ""
                val numbers = mutableListOf<String>()
                var email: String? = null
                var photo: String? = null
                var s1p: String? = null
                var s1h: String? = null
                var s2p: String? = null
                var s2h: String? = null
                for (param in query.split("&")) {
                    val parts = param.split("=")
                    if (parts.size >= 2) {
                        val key = URLDecoder.decode(parts[0], "UTF-8")
                        val value = URLDecoder.decode(parts.drop(1).joinToString("="), "UTF-8")
                        when {
                            key.equals("name", ignoreCase = true) -> name = value
                            key.equals("number", ignoreCase = true) -> numbers.add(value)
                            key.equals("email", ignoreCase = true) -> email = value
                            key.equals("photo", ignoreCase = true) -> photo = value
                            key.equals("s1p", ignoreCase = true) -> s1p = value
                            key.equals("s1h", ignoreCase = true) -> s1h = value
                            key.equals("s2p", ignoreCase = true) -> s2p = value
                            key.equals("s2h", ignoreCase = true) -> s2h = value
                        }
                    }
                }
                if (name.isNotEmpty() || numbers.isNotEmpty() || !email.isNullOrBlank()) {
                    return ContactPayload(
                        name = name.ifEmpty { "Scanned Contact" },
                        numbers = numbers,
                        email = email,
                        photoThumbnailBase64 = photo,
                        social1Platform = s1p,
                        social1Handle = s1h,
                        social2Platform = s2p,
                        social2Handle = s2h
                    )
                }
            } catch (_: Exception) {}
        }

        // 2. vCard format
        if (trimmed.contains("BEGIN:VCARD", ignoreCase = true)) {
            try {
                var name = ""
                val numbers = mutableListOf<String>()
                var email: String? = null
                var s1p: String? = null
                var s1h: String? = null
                var s2p: String? = null
                var s2h: String? = null
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
                        clean.startsWith("EMAIL", ignoreCase = true) -> {
                            email = clean.substringAfter(":").trim()
                        }
                        clean.startsWith("X-SOCIAL-1:", ignoreCase = true) -> {
                            val parts = clean.substringAfter("X-SOCIAL-1:").split(":")
                            if (parts.size >= 2) {
                                s1p = parts[0].trim()
                                s1h = parts.drop(1).joinToString(":").trim()
                            }
                        }
                        clean.startsWith("X-SOCIAL-2:", ignoreCase = true) -> {
                            val parts = clean.substringAfter("X-SOCIAL-2:").split(":")
                            if (parts.size >= 2) {
                                s2p = parts[0].trim()
                                s2h = parts.drop(1).joinToString(":").trim()
                            }
                        }
                    }
                }
                if (name.isNotEmpty() || numbers.isNotEmpty() || !email.isNullOrBlank()) {
                    return ContactPayload(
                        name = name.ifEmpty { "New Contact" },
                        numbers = numbers,
                        email = email,
                        social1Platform = s1p,
                        social1Handle = s1h,
                        social2Platform = s2p,
                        social2Handle = s2h
                    )
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
