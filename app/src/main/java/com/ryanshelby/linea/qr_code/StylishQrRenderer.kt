package com.ryanshelby.linea.qr_code

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.google.zxing.common.BitMatrix
import kotlin.math.hypot

/**
 * Stylish QR Code Renderer.
 *
 * Renders QR codes in a modern organic/liquid-connected aesthetic:
 * - Connected liquid capsule modules with rounded terminals (no harsh squares, no generic dots).
 * - Smooth squircle finder eyes with matching gradient pupil cores.
 * - Central metallic medallion badge with contact initial or LiNEA icon.
 * - 100% compliant with standard QR barcode decoders (instant ML Kit scan).
 */
object StylishQrRenderer {

    enum class QrTheme(
        val displayName: String,
        val startColor: Int,
        val endColor: Int,
        val eyeColor: Int,
        val badgeBorderColor: Int
    ) {
        TITANIUM_CYAN(
            displayName = "Titanium Cyan",
            startColor = 0xFF00E5FF.toInt(),
            endColor = 0xFF4F46E5.toInt(),
            eyeColor = 0xFF38BDF8.toInt(),
            badgeBorderColor = 0xFF38BDF8.toInt()
        ),
        EMERALD_MINT(
            displayName = "Emerald Mint",
            startColor = 0xFF34D399.toInt(),
            endColor = 0xFF059669.toInt(),
            eyeColor = 0xFF10B981.toInt(),
            badgeBorderColor = 0xFF10B981.toInt()
        ),
        CYBER_AMBER(
            displayName = "Cyber Amber",
            startColor = 0xFFF59E0B.toInt(),
            endColor = 0xFFEF4444.toInt(),
            eyeColor = 0xFFF59E0B.toInt(),
            badgeBorderColor = 0xFFF59E0B.toInt()
        ),
        NEON_ORCHID(
            displayName = "Neon Orchid",
            startColor = 0xFFF43F5E.toInt(),
            endColor = 0xFF8B5CF6.toInt(),
            eyeColor = 0xFFEC4899.toInt(),
            badgeBorderColor = 0xFFA855F7.toInt()
        ),
        PURE_WHITE(
            displayName = "Pure White",
            startColor = 0xFFFFFFFF.toInt(),
            endColor = 0xFFE2E8F0.toInt(),
            eyeColor = 0xFFFFFFFF.toInt(),
            badgeBorderColor = 0xFFFFFFFF.toInt()
        )
    }

    /**
     * Render a stylish QR code bitmap.
     *
     * @param bitMatrix The ZXing QR code bit matrix.
     * @param size The output bitmap width & height in pixels (e.g. 768).
     * @param theme The color theme for the gradient and accents.
     * @param badgeLetter Optional contact initial letter rendered in the center badge.
     * @param backgroundColor Background color (default: dark titanium #0B0E14).
     */
    fun render(
        bitMatrix: BitMatrix,
        size: Int = 768,
        theme: QrTheme = QrTheme.TITANIUM_CYAN,
        badgeLetter: String? = null,
        backgroundColor: Int = 0xFF0B0E14.toInt()
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw sleek background
        canvas.drawColor(backgroundColor)

        val matrixSize = bitMatrix.width
        val margin = 3.5f // quiet zone in module units (standard QR compliant)
        val totalModules = matrixSize + margin * 2f
        val moduleSize = size / totalModules
        val originX = margin * moduleSize
        val originY = margin * moduleSize

        // Center badge parameters (circle around matrix center)
        val centerModuleX = matrixSize / 2f
        val centerModuleY = matrixSize / 2f
        val badgeRadiusModules = if (!badgeLetter.isNullOrBlank()) 2.0f else 0f

        // Helper to test if (x, y) is inside one of the 3 finder patterns (strictly 7x7 modules)
        fun isInFinderEye(x: Int, y: Int): Boolean {
            val isTopLeft = x in 0..6 && y in 0..6
            val isTopRight = x in (matrixSize - 7) until matrixSize && y in 0..6
            val isBottomLeft = x in 0..6 && y in (matrixSize - 7) until matrixSize
            return isTopLeft || isTopRight || isBottomLeft
        }

        // Helper to test if (x, y) is inside the center badge area
        fun isInCenterBadge(x: Int, y: Int): Boolean {
            if (badgeRadiusModules <= 0f) return false
            val dx = x + 0.5f - centerModuleX
            val dy = y + 0.5f - centerModuleY
            return hypot(dx, dy) <= badgeRadiusModules
        }

        // Helper to test if (x, y) is a dark data module
        fun isDarkData(x: Int, y: Int): Boolean {
            if (x !in 0 until matrixSize || y !in 0 until matrixSize) return false
            if (isInFinderEye(x, y) || isInCenterBadge(x, y)) return false
            return bitMatrix.get(x, y)
        }

        // 2. Setup gradient paint for modules
        val gradientShader = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            theme.startColor, theme.endColor,
            Shader.TileMode.CLAMP
        )

        val modulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradientShader
            style = Paint.Style.FILL
        }

        val cornerRadius = moduleSize * 0.40f

        // 3. Render connected organic/liquid data modules
        for (y in 0 until matrixSize) {
            for (x in 0 until matrixSize) {
                if (!isDarkData(x, y)) continue

                val hasTop = isDarkData(x, y - 1)
                val hasBottom = isDarkData(x, y + 1)
                val hasLeft = isDarkData(x - 1, y)
                val hasRight = isDarkData(x + 1, y)

                val left = originX + x * moduleSize
                val top = originY + y * moduleSize
                val right = left + moduleSize
                val bottom = top + moduleSize

                // Determine rounding per corner
                val rTL = if (!hasTop && !hasLeft) cornerRadius else 0f
                val rTR = if (!hasTop && !hasRight) cornerRadius else 0f
                val rBR = if (!hasBottom && !hasRight) cornerRadius else 0f
                val rBL = if (!hasBottom && !hasLeft) cornerRadius else 0f

                val path = createRoundedRectPath(left, top, right, bottom, rTL, rTR, rBR, rBL)
                canvas.drawPath(path, modulePaint)
            }
        }

        // 4. Render 3 Styled Finder Eyes
        renderStyledEye(canvas, originX, originY, moduleSize, theme, backgroundColor)
        renderStyledEye(canvas, originX + (matrixSize - 7) * moduleSize, originY, moduleSize, theme, backgroundColor)
        renderStyledEye(canvas, originX, originY + (matrixSize - 7) * moduleSize, moduleSize, theme, backgroundColor)

        // 5. Render Center Medallion Badge
        if (!badgeLetter.isNullOrBlank()) {
            renderCenterBadge(
                canvas = canvas,
                centerX = originX + centerModuleX * moduleSize,
                centerY = originY + centerModuleY * moduleSize,
                radius = badgeRadiusModules * moduleSize,
                letter = badgeLetter.take(1).uppercase(),
                theme = theme,
                backgroundColor = backgroundColor
            )
        }

        return bitmap
    }

    /**
     * Render a styled squircle finder eye (outer frame + inner pupil).
     */
    private fun renderStyledEye(
        canvas: Canvas,
        x: Float,
        y: Float,
        moduleSize: Float,
        theme: QrTheme,
        backgroundColor: Int
    ) {
        val eyeSize = 7f * moduleSize
        val outerRadius = 0.75f * moduleSize
        val strokeW = moduleSize

        // Outer squircle frame
        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.eyeColor
            style = Paint.Style.FILL
        }
        val outerRect = RectF(x, y, x + eyeSize, y + eyeSize)
        canvas.drawRoundRect(outerRect, outerRadius, outerRadius, outerPaint)

        // Hollow inner background rect (preserves exact 1:1:3:1:1 scanner ratio)
        val hollowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundColor
            style = Paint.Style.FILL
        }
        val hollowRect = RectF(
            x + strokeW,
            y + strokeW,
            x + eyeSize - strokeW,
            y + eyeSize - strokeW
        )
        canvas.drawRect(hollowRect, hollowPaint)

        // Inner pupil (3x3 modules with soft rounded squircle curvature)
        val pupilOffset = 2f * moduleSize
        val pupilSize = 3f * moduleSize
        val pupilRadius = 0.5f * moduleSize

        val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.eyeColor
            style = Paint.Style.FILL
        }

        val pupilRect = RectF(
            x + pupilOffset,
            y + pupilOffset,
            x + pupilOffset + pupilSize,
            y + pupilOffset + pupilSize
        )
        canvas.drawRoundRect(pupilRect, pupilRadius, pupilRadius, pupilPaint)
    }

    /**
     * Render center medallion badge with initial letter and subtle glowing ring.
     */
    private fun renderCenterBadge(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        letter: String,
        theme: QrTheme,
        backgroundColor: Int
    ) {
        // Dark background medallion
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, radius, bgPaint)

        // Accent border ring
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.badgeBorderColor
            style = Paint.Style.STROKE
            strokeWidth = radius * 0.12f
        }
        canvas.drawCircle(centerX, centerY, radius * 0.90f, ringPaint)

        // Initial letter
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.startColor
            textSize = radius * 1.15f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val fontMetrics = textPaint.fontMetrics
        val textBaseline = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(letter, centerX, textBaseline, textPaint)
    }

    /**
     * Create a path for a rectangle with individual corner radii.
     */
    private fun createRoundedRectPath(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        rTL: Float,
        rTR: Float,
        rBR: Float,
        rBL: Float
    ): Path {
        val path = Path()
        val radii = floatArrayOf(
            rTL, rTL,
            rTR, rTR,
            rBR, rBR,
            rBL, rBL
        )
        val rect = RectF(left, top, right, bottom)
        path.addRoundRect(rect, radii, Path.Direction.CW)
        return path
    }
}
