package com.ryanshelby.linea.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ryanshelby.linea.data.preferences.DesignStyle

// =============================================================================
// 1. NEUMORPHISM (Soft Tactile Physical UI)
// =============================================================================

val NeumorphicDarkPalette: LineaColorPalette = DarkPalette
val NeumorphicLightPalette: LineaColorPalette = LightPalette
val NeumorphicAmoledPalette: LineaColorPalette = AmoledPalette

// =============================================================================
// 2. ORGANIC / BIOPHILIC UI (Natural Curves, Earth Tones & Living Motion)
// =============================================================================

val OrganicDarkPalette = LineaColorPalette(
    isLight = false,
    BackgroundTop = Color(0xFF16211A),
    BackgroundBottom = Color(0xFF101913),
    BackgroundDeep = Color(0xFF0C130E),
    BackgroundElevated = Color(0xFF1D2C22),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF16211A), Color(0xFF101913))
    ),
    GlassFill = Color(0xFF1A281F),
    GlassBorder = Color(0xFF425E4B).copy(alpha = 0.35f),
    GlassFallbackFill = Color(0xFF16211A),
    TitaniumBlue = Color(0xFF5AC68B),        // Sprout green accent
    DesaturatedTeal = Color(0xFF3B7F61),     // Deep botanical teal
    MutedRust = Color(0xFFD47C59),           // Terracotta clay
    MutedBrickRed = Color(0xFFDE6456),       // Berry red
    MutedSageGreen = Color(0xFF5AC68B),      // Fresh sage
    GlassBorderFocused = Color(0xFF5AC68B).copy(alpha = 0.7f),
    Danger = Color(0xFFDE6456),
    Warning = Color(0xFFE2AA54),             // Sunlight gold amber
    Success = Color(0xFF5AC68B),
    CarmineRed = Color(0xFFDE6456),
    AccentGreen = Color(0xFF5AC68B),
    AccentAmber = Color(0xFFE2AA54),
    SurfaceElevated = Color(0xFF1F3025),
    TextPrimary = Color(0xFFEEF5F0),         // Botanical soft white
    TextSecondary = Color(0xFFA5BBAE),       // Muted moss
    TextTertiary = Color(0xFF708579),        // Earthy slate
    // Soft Botanical Ambient Lighting
    NeuBackground = Color(0xFF16211A),
    NeuLightShadow = Color(0xFF23352A),
    NeuDarkShadow = Color(0xFF0A100B),
    NeuSurfaceRaised = Color(0xFF1C2B21),
    NeuSurfaceSunken = Color(0xFF121B14),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF213227), Color(0xFF17241C))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF111A13), Color(0xFF1D2B21))
    ),
    NeuBorderHighlight = Color(0xFF6AB688).copy(alpha = 0.25f),
    NeuBorderShadow = Color.Black.copy(alpha = 0.45f)
)

val OrganicLightPalette = LineaColorPalette(
    isLight = true,
    BackgroundTop = Color(0xFFF3EFE6),       // Warm bamboo sand
    BackgroundBottom = Color(0xFFE7E1D2),
    BackgroundDeep = Color(0xFFDDD5C4),
    BackgroundElevated = Color(0xFFEDE6D8),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF3EFE6), Color(0xFFE7E1D2))
    ),
    GlassFill = Color(0xFFEBE4D5),
    GlassBorder = Color(0xFFBFAF9B).copy(alpha = 0.45f),
    GlassFallbackFill = Color(0xFFEBE4D5),
    TitaniumBlue = Color(0xFF2D6A4F),        // Deep botanical forest green
    DesaturatedTeal = Color(0xFF285E48),
    MutedRust = Color(0xFFBA5635),           // Warm terracotta
    MutedBrickRed = Color(0xFFC73E2C),
    MutedSageGreen = Color(0xFF2D6A4F),
    GlassBorderFocused = Color(0xFF2D6A4F).copy(alpha = 0.7f),
    Danger = Color(0xFFC73E2C),
    Warning = Color(0xFFB87823),
    Success = Color(0xFF2D6A4F),
    CarmineRed = Color(0xFFC73E2C),
    AccentGreen = Color(0xFF2D6A4F),
    AccentAmber = Color(0xFFB87823),
    SurfaceElevated = Color(0xFFEFE8DB),
    TextPrimary = Color(0xFF1D291F),         // Deep olive
    TextSecondary = Color(0xFF556859),
    TextTertiary = Color(0xFF86978A),
    // Soft Warm Ceramic Lighting
    NeuBackground = Color(0xFFF3EFE6),
    NeuLightShadow = Color.White.copy(alpha = 0.92f),
    NeuDarkShadow = Color(0xFFC7BDA9).copy(alpha = 0.55f),
    NeuSurfaceRaised = Color(0xFFEDE6D7),
    NeuSurfaceSunken = Color(0xFFDFD7C7),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFF7F3EB), Color(0xFFE5DDD0))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFDDD4C3), Color(0xFFF2ECE0))
    ),
    NeuBorderHighlight = Color.White.copy(alpha = 0.85f),
    NeuBorderShadow = Color(0xFFBDB29F).copy(alpha = 0.40f)
)

val OrganicAmoledPalette = LineaColorPalette(
    isLight = false,
    BackgroundTop = Color(0xFF070B08),
    BackgroundBottom = Color(0xFF030504),
    BackgroundDeep = Color(0xFF000000),
    BackgroundElevated = Color(0xFF0E1610),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF070B08), Color(0xFF030504))
    ),
    GlassFill = Color(0xFF0B120D),
    GlassBorder = Color(0xFF304A39).copy(alpha = 0.35f),
    GlassFallbackFill = Color(0xFF070B08),
    TitaniumBlue = Color(0xFF6EE7A8),
    DesaturatedTeal = Color(0xFF459470),
    MutedRust = Color(0xFFE08260),
    MutedBrickRed = Color(0xFFE56A5C),
    MutedSageGreen = Color(0xFF6EE7A8),
    GlassBorderFocused = Color(0xFF6EE7A8).copy(alpha = 0.7f),
    Danger = Color(0xFFE56A5C),
    Warning = Color(0xFFF3B75E),
    Success = Color(0xFF6EE7A8),
    CarmineRed = Color(0xFFE56A5C),
    AccentGreen = Color(0xFF6EE7A8),
    AccentAmber = Color(0xFFF3B75E),
    SurfaceElevated = Color(0xFF0E1610),
    TextPrimary = Color(0xFFF0FDF4),
    TextSecondary = Color(0xFFA7B8AD),
    TextTertiary = Color(0xFF6F7E75),
    NeuBackground = Color(0xFF070B08),
    NeuLightShadow = Color(0xFF131F16),
    NeuDarkShadow = Color(0xFF000000),
    NeuSurfaceRaised = Color(0xFF0C130E),
    NeuSurfaceSunken = Color(0xFF040705),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF101A13), Color(0xFF080D09))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF020403), Color(0xFF0E1711))
    ),
    NeuBorderHighlight = Color(0xFF5AC68B).copy(alpha = 0.20f),
    NeuBorderShadow = Color.Black.copy(alpha = 0.70f)
)

// =============================================================================
// 3. FINTECH MINIMAL (Swiss Precision, Razor Hairlines & Electric Cobalt)
// =============================================================================

val FintechDarkPalette = LineaColorPalette(
    isLight = false,
    BackgroundTop = Color(0xFF0F1116),       // Pitch obsidian slate
    BackgroundBottom = Color(0xFF0A0C0F),
    BackgroundDeep = Color(0xFF07080A),
    BackgroundElevated = Color(0xFF161922),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F1116), Color(0xFF0A0C0F))
    ),
    GlassFill = Color(0xFF13161F),
    GlassBorder = Color(0xFF262C3D),         // Razor-sharp 1dp hairline
    GlassFallbackFill = Color(0xFF0F1116),
    TitaniumBlue = Color(0xFF2970FF),        // Electric fintech cobalt
    DesaturatedTeal = Color(0xFF175CD3),
    MutedRust = Color(0xFFF79009),
    MutedBrickRed = Color(0xFFF04438),       // Crisp alert red
    MutedSageGreen = Color(0xFF12B76A),      // Crisp fintech emerald
    GlassBorderFocused = Color(0xFF2970FF),
    Danger = Color(0xFFF04438),
    Warning = Color(0xFFF79009),
    Success = Color(0xFF12B76A),
    CarmineRed = Color(0xFFF04438),
    AccentGreen = Color(0xFF12B76A),
    AccentAmber = Color(0xFFF79009),
    SurfaceElevated = Color(0xFF161922),
    TextPrimary = Color(0xFFFFFFFF),         // Pure white contrast
    TextSecondary = Color(0xFF94A3B8),       // Slate
    TextTertiary = Color(0xFF64748B),
    // Minimal Architectural Flat Shadows
    NeuBackground = Color(0xFF0F1116),
    NeuLightShadow = Color(0xFF1D2230),
    NeuDarkShadow = Color(0xFF050608),
    NeuSurfaceRaised = Color(0xFF141720),
    NeuSurfaceSunken = Color(0xFF0B0D12),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF171A24), Color(0xFF11141C))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF090B0E), Color(0xFF141822))
    ),
    NeuBorderHighlight = Color(0xFF2970FF).copy(alpha = 0.25f),
    NeuBorderShadow = Color.Black.copy(alpha = 0.50f)
)

val FintechLightPalette = LineaColorPalette(
    isLight = true,
    BackgroundTop = Color(0xFFFFFFFF),       // Architectural crisp white
    BackgroundBottom = Color(0xFFF3F5F9),
    BackgroundDeep = Color(0xFFE8ECF2),
    BackgroundElevated = Color(0xFFFFFFFF),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF3F5F9))
    ),
    GlassFill = Color(0xFFFFFFFF),
    GlassBorder = Color(0xFFE2E7F0),         // Crisp 1dp divider hairline
    GlassFallbackFill = Color(0xFFFFFFFF),
    TitaniumBlue = Color(0xFF0052FF),        // Vivid electric cobalt
    DesaturatedTeal = Color(0xFF0A47CD),
    MutedRust = Color(0xFFDC6803),
    MutedBrickRed = Color(0xFFD92D20),
    MutedSageGreen = Color(0xFF039855),
    GlassBorderFocused = Color(0xFF0052FF),
    Danger = Color(0xFFD92D20),
    Warning = Color(0xFFDC6803),
    Success = Color(0xFF039855),
    CarmineRed = Color(0xFFD92D20),
    AccentGreen = Color(0xFF039855),
    AccentAmber = Color(0xFFDC6803),
    SurfaceElevated = Color(0xFFFFFFFF),
    TextPrimary = Color(0xFF0F172A),         // Sharp dark slate
    TextSecondary = Color(0xFF475569),
    TextTertiary = Color(0xFF94A3B8),
    NeuBackground = Color(0xFFF8FAFC),
    NeuLightShadow = Color.White,
    NeuDarkShadow = Color(0xFFCBD5E1).copy(alpha = 0.45f),
    NeuSurfaceRaised = Color(0xFFFFFFFF),
    NeuSurfaceSunken = Color(0xFFEEF2F6),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFE9EEF4), Color(0xFFF8FAFC))
    ),
    NeuBorderHighlight = Color(0xFFE2E8F0),
    NeuBorderShadow = Color(0xFFCBD5E1).copy(alpha = 0.35f)
)

val FintechAmoledPalette = LineaColorPalette(
    isLight = false,
    BackgroundTop = Color(0xFF000000),       // Pitch pure black
    BackgroundBottom = Color(0xFF000000),
    BackgroundDeep = Color(0xFF000000),
    BackgroundElevated = Color(0xFF0D0F14),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF000000), Color(0xFF000000))
    ),
    GlassFill = Color(0xFF0A0C10),
    GlassBorder = Color(0xFF1E222D),
    GlassFallbackFill = Color(0xFF000000),
    TitaniumBlue = Color(0xFF3B82F6),
    DesaturatedTeal = Color(0xFF2563EB),
    MutedRust = Color(0xFFFB923C),
    MutedBrickRed = Color(0xFFF87171),
    MutedSageGreen = Color(0xFF34D399),
    GlassBorderFocused = Color(0xFF3B82F6),
    Danger = Color(0xFFF87171),
    Warning = Color(0xFFFB923C),
    Success = Color(0xFF34D399),
    CarmineRed = Color(0xFFF87171),
    AccentGreen = Color(0xFF34D399),
    AccentAmber = Color(0xFFFB923C),
    SurfaceElevated = Color(0xFF0F1218),
    TextPrimary = Color(0xFFFFFFFF),
    TextSecondary = Color(0xFF94A3B8),
    TextTertiary = Color(0xFF64748B),
    NeuBackground = Color(0xFF000000),
    NeuLightShadow = Color(0xFF141720),
    NeuDarkShadow = Color(0xFF000000),
    NeuSurfaceRaised = Color(0xFF0C0E14),
    NeuSurfaceSunken = Color(0xFF040507),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF10131B), Color(0xFF08090D))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF020204), Color(0xFF0E1017))
    ),
    NeuBorderHighlight = Color(0xFF3B82F6).copy(alpha = 0.30f),
    NeuBorderShadow = Color.Black.copy(alpha = 0.80f)
)

// =============================================================================
// 4. MATERIAL 3 EXPRESSIVE (Dynamic Tonal Containers & Pill Shapes)
// =============================================================================

val Material3DarkPalette = LineaColorPalette(
    isLight = false,
    BackgroundTop = Color(0xFF1A1528),       // Expressive deep violet indigo
    BackgroundBottom = Color(0xFF130F1E),
    BackgroundDeep = Color(0xFF0E0A16),
    BackgroundElevated = Color(0xFF251F36),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1A1528), Color(0xFF130F1E))
    ),
    GlassFill = Color(0xFF221C32),
    GlassBorder = Color(0xFF49405F).copy(alpha = 0.5f),
    GlassFallbackFill = Color(0xFF1A1528),
    TitaniumBlue = Color(0xFFD0BCFF),        // Expressive violet primary
    DesaturatedTeal = Color(0xFF7DDAE8),     // Expressive cyan/teal
    MutedRust = Color(0xFFFFB4AB),           // Expressive coral/salmon
    MutedBrickRed = Color(0xFFFF897D),
    MutedSageGreen = Color(0xFF86D9A5),
    GlassBorderFocused = Color(0xFFD0BCFF),
    Danger = Color(0xFFFF897D),
    Warning = Color(0xFFFFD99D),
    Success = Color(0xFF86D9A5),
    CarmineRed = Color(0xFFFF897D),
    AccentGreen = Color(0xFF86D9A5),
    AccentAmber = Color(0xFFFFD99D),
    SurfaceElevated = Color(0xFF2B243D),
    TextPrimary = Color(0xFFE7E0EC),
    TextSecondary = Color(0xFFCCC2DC),
    TextTertiary = Color(0xFF948F99),
    NeuBackground = Color(0xFF1A1528),
    NeuLightShadow = Color(0xFF2C2440),
    NeuDarkShadow = Color(0xFF0C0913),
    NeuSurfaceRaised = Color(0xFF231D33),
    NeuSurfaceSunken = Color(0xFF151120),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF2A233D), Color(0xFF1E182C))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF120E1C), Color(0xFF251E36))
    ),
    NeuBorderHighlight = Color(0xFFD0BCFF).copy(alpha = 0.35f),
    NeuBorderShadow = Color.Black.copy(alpha = 0.45f)
)

val Material3LightPalette = LineaColorPalette(
    isLight = true,
    BackgroundTop = Color(0xFFFAF6FD),       // Expressive lavender milk
    BackgroundBottom = Color(0xFFF1EBF7),
    BackgroundDeep = Color(0xFFE7DFEE),
    BackgroundElevated = Color(0xFFF4ECFA),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFAF6FD), Color(0xFFF1EBF7))
    ),
    GlassFill = Color(0xFFEFE6F6),
    GlassBorder = Color(0xFF7A6F89).copy(alpha = 0.25f),
    GlassFallbackFill = Color(0xFFEFE6F6),
    TitaniumBlue = Color(0xFF6750A4),        // M3 Vivid violet
    DesaturatedTeal = Color(0xFF006876),
    MutedRust = Color(0xFFB3261E),
    MutedBrickRed = Color(0xFFB3261E),
    MutedSageGreen = Color(0xFF206C47),
    GlassBorderFocused = Color(0xFF6750A4),
    Danger = Color(0xFFB3261E),
    Warning = Color(0xFF8F4E00),
    Success = Color(0xFF206C47),
    CarmineRed = Color(0xFFB3261E),
    AccentGreen = Color(0xFF206C47),
    AccentAmber = Color(0xFF8F4E00),
    SurfaceElevated = Color(0xFFF7EFFD),
    TextPrimary = Color(0xFF1D1A22),
    TextSecondary = Color(0xFF49454E),
    TextTertiary = Color(0xFF7A757F),
    NeuBackground = Color(0xFFFAF6FD),
    NeuLightShadow = Color.White.copy(alpha = 0.95f),
    NeuDarkShadow = Color(0xFFD0C3DB).copy(alpha = 0.55f),
    NeuSurfaceRaised = Color(0xFFEFE6F6),
    NeuSurfaceSunken = Color(0xFFE3D9EB),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFF8F0FE), Color(0xFFE9DFEF))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFDFD4E7), Color(0xFFF4EBFB))
    ),
    NeuBorderHighlight = Color.White.copy(alpha = 0.85f),
    NeuBorderShadow = Color(0xFFC7B7D4).copy(alpha = 0.40f)
)

val Material3AmoledPalette = LineaColorPalette(
    isLight = false,
    BackgroundTop = Color(0xFF09060F),
    BackgroundBottom = Color(0xFF040307),
    BackgroundDeep = Color(0xFF000000),
    BackgroundElevated = Color(0xFF140F21),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF09060F), Color(0xFF040307))
    ),
    GlassFill = Color(0xFF100C1B),
    GlassBorder = Color(0xFF382F4E).copy(alpha = 0.45f),
    GlassFallbackFill = Color(0xFF09060F),
    TitaniumBlue = Color(0xFFD8C7FF),
    DesaturatedTeal = Color(0xFF88E4F2),
    MutedRust = Color(0xFFFFB4AB),
    MutedBrickRed = Color(0xFFFF897D),
    MutedSageGreen = Color(0xFF97E6B4),
    GlassBorderFocused = Color(0xFFD8C7FF),
    Danger = Color(0xFFFF897D),
    Warning = Color(0xFFFFE0A8),
    Success = Color(0xFF97E6B4),
    CarmineRed = Color(0xFFFF897D),
    AccentGreen = Color(0xFF97E6B4),
    AccentAmber = Color(0xFFFFE0A8),
    SurfaceElevated = Color(0xFF161024),
    TextPrimary = Color(0xFFFFFFFF),
    TextSecondary = Color(0xFFD1C7DF),
    TextTertiary = Color(0xFF948A9E),
    NeuBackground = Color(0xFF09060F),
    NeuLightShadow = Color(0xFF1B142B),
    NeuDarkShadow = Color(0xFF000000),
    NeuSurfaceRaised = Color(0xFF110C1C),
    NeuSurfaceSunken = Color(0xFF050308),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF171126), Color(0xFF0C0816))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF030206), Color(0xFF140E20))
    ),
    NeuBorderHighlight = Color(0xFFD8C7FF).copy(alpha = 0.30f),
    NeuBorderShadow = Color.Black.copy(alpha = 0.75f)
)

// =============================================================================
// 5. NEO-BRUTALISM (Bold Pop Attitude, Chunky Borders & Solid Block Shadows)
// =============================================================================

val NeoBrutalismDarkPalette = LineaColorPalette(
    isLight = false,
    BackgroundTop = Color(0xFF18181E),       // Chunky dark slate
    BackgroundBottom = Color(0xFF111115),
    BackgroundDeep = Color(0xFF0A0A0D),
    BackgroundElevated = Color(0xFF22222B),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF18181E), Color(0xFF111115))
    ),
    GlassFill = Color(0xFF202028),
    GlassBorder = Color(0xFF000000),         // Solid black border outline
    GlassFallbackFill = Color(0xFF202028),
    TitaniumBlue = Color(0xFFFFE600),        // Pop canary yellow primary
    DesaturatedTeal = Color(0xFF00F0FF),     // Electric cyan
    MutedRust = Color(0xFFFF4D79),           // Hot pop pink
    MutedBrickRed = Color(0xFFFF3344),       // Loud red
    MutedSageGreen = Color(0xFF00F59B),      // Neo lime
    GlassBorderFocused = Color(0xFFFFE600),
    Danger = Color(0xFFFF3344),
    Warning = Color(0xFFFF9900),
    Success = Color(0xFF00F59B),
    CarmineRed = Color(0xFFFF3344),
    AccentGreen = Color(0xFF00F59B),
    AccentAmber = Color(0xFFFFE600),
    SurfaceElevated = Color(0xFF262633),
    TextPrimary = Color(0xFFFFFFFF),
    TextSecondary = Color(0xFFD1D1DE),
    TextTertiary = Color(0xFF9090A0),
    // Solid Block Shadows for Neo-Brutalism
    NeuBackground = Color(0xFF18181E),
    NeuLightShadow = Color(0xFF2E2E3C),
    NeuDarkShadow = Color(0xFF000000),       // Pitch black block shadow
    NeuSurfaceRaised = Color(0xFF22222C),
    NeuSurfaceSunken = Color(0xFF14141A),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF262634), Color(0xFF1E1E26))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF111116), Color(0xFF20202A))
    ),
    NeuBorderHighlight = Color(0xFFFFFFFF).copy(alpha = 0.40f),
    NeuBorderShadow = Color(0xFF000000)
)

val NeoBrutalismLightPalette = LineaColorPalette(
    isLight = true,
    BackgroundTop = Color(0xFFFFFDF5),       // Cream pop canvas
    BackgroundBottom = Color(0xFFFFF9E4),
    BackgroundDeep = Color(0xFFF3ECCF),
    BackgroundElevated = Color(0xFFFFFFFF),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFFDF5), Color(0xFFFFF9E4))
    ),
    GlassFill = Color(0xFFFFFFFF),
    GlassBorder = Color(0xFF000000),         // Solid 2.5dp black stroke
    GlassFallbackFill = Color(0xFFFFFFFF),
    TitaniumBlue = Color(0xFFFFE600),        // Electric pop canary yellow
    DesaturatedTeal = Color(0xFF00E5FF),     // Electric cyan
    MutedRust = Color(0xFFFF3377),           // Punchy pop pink
    MutedBrickRed = Color(0xFFFF2233),
    MutedSageGreen = Color(0xFF00E676),      // Neo lime
    GlassBorderFocused = Color(0xFF000000),
    Danger = Color(0xFFFF2233),
    Warning = Color(0xFFFF9900),
    Success = Color(0xFF00E676),
    CarmineRed = Color(0xFFFF2233),
    AccentGreen = Color(0xFF00E676),
    AccentAmber = Color(0xFFFFE600),
    SurfaceElevated = Color(0xFFFFFFFF),
    TextPrimary = Color(0xFF000000),         // Pitch black text
    TextSecondary = Color(0xFF2E2E2E),
    TextTertiary = Color(0xFF6B6B6B),
    NeuBackground = Color(0xFFFFFDF5),
    NeuLightShadow = Color.White,
    NeuDarkShadow = Color(0xFF000000),       // Pure solid block shadow
    NeuSurfaceRaised = Color(0xFFFFFFFF),
    NeuSurfaceSunken = Color(0xFFF5EFCF),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFFAF5E8))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFEFE8C4), Color(0xFFFAF5E8))
    ),
    NeuBorderHighlight = Color(0xFF000000),
    NeuBorderShadow = Color(0xFF000000)
)

val NeoBrutalismAmoledPalette = LineaColorPalette(
    isLight = false,
    BackgroundTop = Color(0xFF000000),
    BackgroundBottom = Color(0xFF000000),
    BackgroundDeep = Color(0xFF000000),
    BackgroundElevated = Color(0xFF141418),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF000000), Color(0xFF000000))
    ),
    GlassFill = Color(0xFF111116),
    GlassBorder = Color(0xFFFFFFFF),         // High-contrast chalk border in AMOLED
    GlassFallbackFill = Color(0xFF000000),
    TitaniumBlue = Color(0xFFFFEE00),
    DesaturatedTeal = Color(0xFF00FFFF),
    MutedRust = Color(0xFFFF3D7F),
    MutedBrickRed = Color(0xFFFF2A3C),
    MutedSageGreen = Color(0xFF00FF88),
    GlassBorderFocused = Color(0xFFFFEE00),
    Danger = Color(0xFFFF2A3C),
    Warning = Color(0xFFFF9D00),
    Success = Color(0xFF00FF88),
    CarmineRed = Color(0xFFFF2A3C),
    AccentGreen = Color(0xFF00FF88),
    AccentAmber = Color(0xFFFFEE00),
    SurfaceElevated = Color(0xFF181820),
    TextPrimary = Color(0xFFFFFFFF),
    TextSecondary = Color(0xFFD8D8E4),
    TextTertiary = Color(0xFF9E9EAE),
    NeuBackground = Color(0xFF000000),
    NeuLightShadow = Color(0xFF20202A),
    NeuDarkShadow = Color(0xFF000000),
    NeuSurfaceRaised = Color(0xFF14141A),
    NeuSurfaceSunken = Color(0xFF08080B),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF1A1A22), Color(0xFF0E0E14))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF050508), Color(0xFF15151C))
    ),
    NeuBorderHighlight = Color(0xFFFFFFFF).copy(alpha = 0.60f),
    NeuBorderShadow = Color.Black
)

// =============================================================================
// 6. HANDWRITTEN ART STYLE (Sketchbook, Ink Doodles & Parchment Paper)
// =============================================================================

val HandwrittenDarkPalette = LineaColorPalette(
    isLight = false,
    BackgroundTop = Color(0xFF1E1B19),       // Charcoal drawing paper
    BackgroundBottom = Color(0xFF171412),
    BackgroundDeep = Color(0xFF110F0D),
    BackgroundElevated = Color(0xFF272320),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E1B19), Color(0xFF171412))
    ),
    GlassFill = Color(0xFF24201D),
    GlassBorder = Color(0xFF85786B).copy(alpha = 0.5f), // Hand-sketched sepia line
    GlassFallbackFill = Color(0xFF1E1B19),
    TitaniumBlue = Color(0xFF7CB8DC),        // Watercolor blue wash
    DesaturatedTeal = Color(0xFF68A398),     // Sage wash
    MutedRust = Color(0xFFDF8773),           // Tea rose / terracotta wash
    MutedBrickRed = Color(0xFFDC6554),       // Carmine sketch
    MutedSageGreen = Color(0xFF8FB995),      // Botanical sketch green
    GlassBorderFocused = Color(0xFF7CB8DC),
    Danger = Color(0xFFDC6554),
    Warning = Color(0xFFE8B65A),             // Highlighter amber
    Success = Color(0xFF8FB995),
    CarmineRed = Color(0xFFDC6554),
    AccentGreen = Color(0xFF8FB995),
    AccentAmber = Color(0xFFE8B65A),
    SurfaceElevated = Color(0xFF2B2623),
    TextPrimary = Color(0xFFF3EFE7),         // Warm sketch cream
    TextSecondary = Color(0xFFB5ABA0),       // Pencil graphite
    TextTertiary = Color(0xFF7A7065),
    NeuBackground = Color(0xFF1E1B19),
    NeuLightShadow = Color(0xFF2D2824),
    NeuDarkShadow = Color(0xFF0F0E0C),
    NeuSurfaceRaised = Color(0xFF25211E),
    NeuSurfaceSunken = Color(0xFF171513),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF2B2723), Color(0xFF201D1A))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF13110F), Color(0xFF24201D))
    ),
    NeuBorderHighlight = Color(0xFFE0D7CB).copy(alpha = 0.25f),
    NeuBorderShadow = Color.Black.copy(alpha = 0.45f)
)

val HandwrittenLightPalette = LineaColorPalette(
    isLight = true,
    BackgroundTop = Color(0xFFFAF7EE),       // Warm parchment paper
    BackgroundBottom = Color(0xFFF1EBDD),
    BackgroundDeep = Color(0xFFE5DCCF),
    BackgroundElevated = Color(0xFFFFFDF8),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFAF7EE), Color(0xFFF1EBDD))
    ),
    GlassFill = Color(0xFFFDFBF5),
    GlassBorder = Color(0xFF42372E).copy(alpha = 0.35f), // Sepia ink pen border
    GlassFallbackFill = Color(0xFFFDFBF5),
    TitaniumBlue = Color(0xFF32577A),        // Fountain pen indigo
    DesaturatedTeal = Color(0xFF2B6B61),
    MutedRust = Color(0xFFB84C32),           // Sepia terracotta
    MutedBrickRed = Color(0xFFBF352B),
    MutedSageGreen = Color(0xFF387A4F),
    GlassBorderFocused = Color(0xFF32577A),
    Danger = Color(0xFFBF352B),
    Warning = Color(0xFFB5771E),
    Success = Color(0xFF387A4F),
    CarmineRed = Color(0xFFBF352B),
    AccentGreen = Color(0xFF387A4F),
    AccentAmber = Color(0xFFB5771E),
    SurfaceElevated = Color(0xFFFFFDF8),
    TextPrimary = Color(0xFF2B231C),         // Deep sepia ink
    TextSecondary = Color(0xFF5E544A),       // Soft pencil graphite
    TextTertiary = Color(0xFF8E8478),
    NeuBackground = Color(0xFFFAF7EE),
    NeuLightShadow = Color.White.copy(alpha = 0.95f),
    NeuDarkShadow = Color(0xFFC7BDAE).copy(alpha = 0.50f),
    NeuSurfaceRaised = Color(0xFFFDFBF5),
    NeuSurfaceSunken = Color(0xFFEAE2D2),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF6F0E4))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFE4DBC8), Color(0xFFF9F5EC))
    ),
    NeuBorderHighlight = Color.White.copy(alpha = 0.85f),
    NeuBorderShadow = Color(0xFFBAAF9D).copy(alpha = 0.40f)
)

val HandwrittenAmoledPalette = LineaColorPalette(
    isLight = false,
    BackgroundTop = Color(0xFF0C0A09),
    BackgroundBottom = Color(0xFF060505),
    BackgroundDeep = Color(0xFF000000),
    BackgroundElevated = Color(0xFF171412),
    BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0C0A09), Color(0xFF060505))
    ),
    GlassFill = Color(0xFF13110F),
    GlassBorder = Color(0xFF6E6254).copy(alpha = 0.45f),
    GlassFallbackFill = Color(0xFF0C0A09),
    TitaniumBlue = Color(0xFF8BBDE0),
    DesaturatedTeal = Color(0xFF75B5A8),
    MutedRust = Color(0xFFE8927F),
    MutedBrickRed = Color(0xFFE57262),
    MutedSageGreen = Color(0xFF9BC5A1),
    GlassBorderFocused = Color(0xFF8BBDE0),
    Danger = Color(0xFFE57262),
    Warning = Color(0xFFF0C26D),
    Success = Color(0xFF9BC5A1),
    CarmineRed = Color(0xFFE57262),
    AccentGreen = Color(0xFF9BC5A1),
    AccentAmber = Color(0xFFF0C26D),
    SurfaceElevated = Color(0xFF181513),
    TextPrimary = Color(0xFFFFFFFF),
    TextSecondary = Color(0xFFC4B8AC),
    TextTertiary = Color(0xFF857A6F),
    NeuBackground = Color(0xFF0C0A09),
    NeuLightShadow = Color(0xFF1C1815),
    NeuDarkShadow = Color(0xFF000000),
    NeuSurfaceRaised = Color(0xFF13100E),
    NeuSurfaceSunken = Color(0xFF070605),
    NeuRaisedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF1A1613), Color(0xFF0F0D0B))
    ),
    NeuSunkenGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF040303), Color(0xFF14110F))
    ),
    NeuBorderHighlight = Color(0xFF8BBDE0).copy(alpha = 0.30f),
    NeuBorderShadow = Color.Black.copy(alpha = 0.75f)
)

// =============================================================================
// Helper resolver for theme palette
// =============================================================================

fun resolveThemePalette(
    style: DesignStyle,
    themeMode: String,
    systemInDark: Boolean
): LineaColorPalette {
    val isLight = when (themeMode.uppercase()) {
        "LIGHT" -> true
        "AMOLED" -> false
        "SYSTEM" -> !systemInDark
        else -> false
    }
    val isAmoled = themeMode.uppercase() == "AMOLED"

    return when (style) {
        DesignStyle.NEUMORPHISM, DesignStyle.CLASSIC -> when {
            isLight -> NeumorphicLightPalette
            isAmoled -> NeumorphicAmoledPalette
            else -> NeumorphicDarkPalette
        }
        DesignStyle.ORGANIC -> when {
            isLight -> OrganicLightPalette
            isAmoled -> OrganicAmoledPalette
            else -> OrganicDarkPalette
        }
        DesignStyle.FINTECH -> when {
            isLight -> FintechLightPalette
            isAmoled -> FintechAmoledPalette
            else -> FintechDarkPalette
        }
        DesignStyle.MATERIAL3 -> when {
            isLight -> Material3LightPalette
            isAmoled -> Material3AmoledPalette
            else -> Material3DarkPalette
        }
        DesignStyle.NEO_BRUTALISM -> when {
            isLight -> NeoBrutalismLightPalette
            isAmoled -> NeoBrutalismAmoledPalette
            else -> NeoBrutalismDarkPalette
        }
        DesignStyle.HANDWRITTEN -> when {
            isLight -> HandwrittenLightPalette
            isAmoled -> HandwrittenAmoledPalette
            else -> HandwrittenDarkPalette
        }
        DesignStyle.CRYSTAL -> {
            val base = when {
                isLight -> LightPalette
                isAmoled -> AmoledPalette
                else -> DarkPalette
            }
            crystalPalette(base)
        }
    }
}
