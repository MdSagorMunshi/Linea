package com.ryanshelby.linea

import com.ryanshelby.linea.qr_code.QrCodeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeTest {

    @Test
    fun testPayloadSerializationAndParsing() {
        val payload = QrCodeEngine.ContactPayload(
            name = "Marcus Vance",
            numbers = listOf("+15554321098", "+15559876543")
        )
        val uri = QrCodeEngine.serializeToUri(payload)
        val parsed = QrCodeEngine.parseScannedText(uri)

        assertNotNull("Parsed payload must not be null", parsed)
        assertEquals("Name must match", payload.name, parsed?.name)
        assertEquals("Numbers must match", payload.numbers, parsed?.numbers)
    }

    @Test
    fun testVCardSerializationAndParsing() {
        val payload = QrCodeEngine.ContactPayload(
            name = "Sarah Connor",
            numbers = listOf("+15551234567")
        )
        val vcard = QrCodeEngine.serializeToVCard(payload)
        val parsed = QrCodeEngine.parseScannedText(vcard)

        assertNotNull("vCard must be parsed", parsed)
        assertEquals("Sarah Connor", parsed?.name)
        assertEquals(listOf("+15551234567"), parsed?.numbers)
    }

    @Test
    fun testMecardParsing() {
        val mecard = "MECARD:N:Smith,John;TEL:+1234567890;EMAIL:john@example.com;;"
        val parsed = QrCodeEngine.parseScannedText(mecard)

        assertNotNull("MECARD must be parsed", parsed)
        assertEquals("John Smith", parsed?.name)
        assertEquals(listOf("+1234567890"), parsed?.numbers)
    }

    @Test
    fun testJsonParsing() {
        val json = """{"name":"Cyberpunk Dial","numbers":["+19998887777"]}"""
        val parsed = QrCodeEngine.parseScannedText(json)

        assertNotNull("JSON payload must be parsed", parsed)
        assertEquals("Cyberpunk Dial", parsed?.name)
        assertEquals(listOf("+19998887777"), parsed?.numbers)
    }

    @Test
    fun testPlainNumberFallback() {
        val rawNumber = "+1 (555) 019-2834"
        val parsed = QrCodeEngine.parseScannedText(rawNumber)

        assertNotNull("Plain number must be parsed", parsed)
        assertEquals(listOf(rawNumber), parsed?.numbers)
    }

    @Test
    fun testBitMatrixGeneration() {
        val content = "linea://contact?name=Test&number=123"
        val bitMatrix = QrCodeEngine.generateBitMatrix(content)

        assertNotNull(bitMatrix)
        assertTrue(bitMatrix.width > 0)
        assertTrue(bitMatrix.height > 0)
        assertEquals(bitMatrix.width, bitMatrix.height)
    }

    @Test
    fun testDecodeSavedQr() {
        val file = java.io.File("/tmp/latest_saved_qr.png")
        if (!file.exists()) return
        val image = javax.imageio.ImageIO.read(file)
        val width = image.width
        val height = image.height
        val pixels = IntArray(width * height)
        image.getRGB(0, 0, width, height, pixels, 0, width)

        val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
        val reader = com.google.zxing.qrcode.QRCodeReader()

        // 1. Normal
        try {
            val bitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
            val result = reader.decode(bitmap)
            System.err.println("ZXING_DECODED_NORMAL: " + result.text)
        } catch (e: Exception) {
            System.err.println("ZXING_NORMAL_FAILED: " + e.javaClass.simpleName + ": " + e.message)
        }

        // 2. Inverted
        try {
            val invertedBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source.invert()))
            val resultInv = reader.decode(invertedBitmap)
            System.err.println("ZXING_DECODED_INVERTED: " + resultInv.text)
        } catch (e2: Exception) {
            System.err.println("ZXING_INVERTED_FAILED: " + e2.javaClass.simpleName + ": " + e2.message)
        }
    }

    @Test
    fun testPureBitMatrixDecode() {
        val content = "linea://contact?name=Test&number=123"
        val matrix = QrCodeEngine.generateBitMatrix(content)
        val scale = 10
        val width = matrix.width * scale
        val height = matrix.height * scale
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val bit = matrix.get(x / scale, y / scale)
                pixels[y * width + x] = if (bit) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
        val bitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
        val reader = com.google.zxing.qrcode.QRCodeReader()
        val result = reader.decode(bitmap)
        assertEquals(content, result.text)
    }

    @Test
    fun testStylishQrDecodeSimulation() {
        val hints = mapOf(
            com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.Q,
            com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8",
            com.google.zxing.EncodeHintType.MARGIN to 0
        )
        val content = "linea://contact?name=Ryan+Shelby&number=%2B15551234567"
        val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(content, com.google.zxing.BarcodeFormat.QR_CODE, 0, 0, hints)
        val matrixSize = bitMatrix.width
        val margin = 4
        val totalModules = matrixSize + margin * 2
        val moduleSize = 16
        val size = totalModules * moduleSize

        val image = java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g2d = image.createGraphics()
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)

        // Dark background (theme background)
        val darkBg = java.awt.Color(0x0B, 0x0E, 0x14)
        g2d.color = darkBg
        g2d.fillRect(0, 0, size, size)

        val originX = margin * moduleSize
        val originY = margin * moduleSize
        val cornerRadius = (moduleSize * 0.40f).toInt()

        fun isInFinderEye(x: Int, y: Int): Boolean {
            val isTopLeft = x in 0..6 && y in 0..6
            val isTopRight = x in (matrixSize - 7) until matrixSize && y in 0..6
            val isBottomLeft = x in 0..6 && y in (matrixSize - 7) until matrixSize
            return isTopLeft || isTopRight || isBottomLeft
        }

        // Center badge radius in modules (fits safely inside QR Level Q 25% recovery)
        val centerMod = matrixSize / 2.0
        val badgeRadiusMod = 2.0
        fun isInBadge(x: Int, y: Int): Boolean {
            val dx = (x + 0.5) - centerMod
            val dy = (y + 0.5) - centerMod
            return Math.hypot(dx, dy) <= badgeRadiusMod
        }

        fun isDarkData(x: Int, y: Int): Boolean {
            if (x !in 0 until matrixSize || y !in 0 until matrixSize) return false
            if (isInFinderEye(x, y) || isInBadge(x, y)) return false
            return bitMatrix.get(x, y)
        }

        // Module cyan color
        val moduleColor = java.awt.Color(0x00, 0xE5, 0xFF)
        val eyeColor = java.awt.Color(0x38, 0xBD, 0xF8)

        // Test connected liquid modules using GeneralPath
        g2d.color = moduleColor
        for (y in 0 until matrixSize) {
            for (x in 0 until matrixSize) {
                if (!isDarkData(x, y)) continue
                val px = originX + x * moduleSize
                val py = originY + y * moduleSize

                val hasTop = isDarkData(x, y - 1)
                val hasBottom = isDarkData(x, y + 1)
                val hasLeft = isDarkData(x - 1, y)
                val hasRight = isDarkData(x + 1, y)

                val rTL = if (!hasTop && !hasLeft) cornerRadius.toFloat() else 0f
                val rTR = if (!hasTop && !hasRight) cornerRadius.toFloat() else 0f
                val rBR = if (!hasBottom && !hasRight) cornerRadius.toFloat() else 0f
                val rBL = if (!hasBottom && !hasLeft) cornerRadius.toFloat() else 0f

                // Draw path with rounded outer corners
                val path = java.awt.geom.Path2D.Float()
                val w = moduleSize.toFloat()
                val h = moduleSize.toFloat()
                val x0 = px.toFloat()
                val y0 = py.toFloat()

                path.moveTo(x0 + rTL, y0)
                path.lineTo(x0 + w - rTR, y0)
                if (rTR > 0) path.quadTo(x0 + w, y0, x0 + w, y0 + rTR)
                path.lineTo(x0 + w, y0 + h - rBR)
                if (rBR > 0) path.quadTo(x0 + w, y0 + h, x0 + w - rBR, y0 + h)
                path.lineTo(x0 + rBL, y0 + h)
                if (rBL > 0) path.quadTo(x0, y0 + h, x0, y0 + h - rBL)
                path.lineTo(x0, y0 + rTL)
                if (rTL > 0) path.quadTo(x0, y0, x0 + rTL, y0)
                path.closePath()

                g2d.fill(path)
            }
        }

        // Draw 3 squircle finder eyes
        fun drawEye(ox: Int, oy: Int) {
            val eyeSize = 7 * moduleSize
            val outerArc = (moduleSize * 0.75f).toInt()
            val strokeW = moduleSize

            // Outer squircle ring (drawn by outer box minus inner background box)
            g2d.color = eyeColor
            g2d.fillRoundRect(ox, oy, eyeSize, eyeSize, outerArc, outerArc)

            g2d.color = darkBg
            g2d.fillRect(ox + strokeW, oy + strokeW, 5 * moduleSize, 5 * moduleSize)

            // Inner pupil (3x3 with soft rounding)
            val pupilArc = (moduleSize * 0.5f).toInt()
            g2d.color = eyeColor
            g2d.fillRoundRect(ox + 2 * moduleSize, oy + 2 * moduleSize, 3 * moduleSize, 3 * moduleSize, pupilArc, pupilArc)
        }

        drawEye(originX, originY)
        drawEye(originX + (matrixSize - 7) * moduleSize, originY)
        drawEye(originX, originY + (matrixSize - 7) * moduleSize)

        // Draw center badge
        val badgePx = (originX + centerMod * moduleSize).toInt()
        val badgePy = (originY + centerMod * moduleSize).toInt()
        val badgeR = (badgeRadiusMod * moduleSize).toInt()
        g2d.color = darkBg
        g2d.fillOval(badgePx - badgeR, badgePy - badgeR, badgeR * 2, badgeR * 2)
        g2d.color = eyeColor
        g2d.drawOval(badgePx - badgeR, badgePy - badgeR, badgeR * 2, badgeR * 2)

        g2d.dispose()

        // Decode with ZXing using inverted (since light foreground on dark background)
        val pixels = IntArray(size * size)
        image.getRGB(0, 0, size, size, pixels, 0, size)
        val source = com.google.zxing.RGBLuminanceSource(size, size, pixels)
        // Light on dark -> invert for standard QR reader
        val invertedBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source.invert()))
        val reader = com.google.zxing.qrcode.QRCodeReader()
        val decoded = reader.decode(invertedBitmap)

        assertNotNull("Decoded result must not be null", decoded)
        assertEquals("Decoded content must match", content, decoded.text)
    }
}
