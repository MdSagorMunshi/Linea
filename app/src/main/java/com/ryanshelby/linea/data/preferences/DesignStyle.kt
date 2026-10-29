package com.ryanshelby.linea.data.preferences

/**
 * UI/UX design systems in LiNEA, including the original Classic & Crystal styles
 * alongside the complete design systems (Neumorphism, Organic / Biophilic,
 * Fintech Minimal, Material 3 Expressive, Neo-Brutalism, Handwritten Art Style).
 *
 * Independent of light/dark/amoled mode so changing a design style never resets color preferences.
 */
enum class DesignStyle(
    val id: String,
    val displayName: String,
    val tagLine: String,
    val description: String
) {
    CLASSIC(
        id = "CLASSIC",
        displayName = "Classic",
        tagLine = "Soft & Sculpted",
        description = "Original sculpted neumorphic surfaces and dual ambient shadows"
    ),
    CRYSTAL(
        id = "CRYSTAL",
        displayName = "Crystal",
        tagLine = "Glass & Titanium",
        description = "Live refracted glass, titanium ribbon backdrop & floating dock"
    ),
    NEUMORPHISM(
        id = "NEUMORPHISM",
        displayName = "Neumorphism",
        tagLine = "Tactile Extrusion",
        description = "Physical extruded surfaces, debossed wells & tactile depth"
    ),
    ORGANIC(
        id = "ORGANIC",
        displayName = "Organic / Biophilic",
        tagLine = "Natural & Earthy",
        description = "Curved fluid pebbles, warm botanicals & living motion"
    ),
    FINTECH(
        id = "FINTECH",
        displayName = "Fintech Minimal",
        tagLine = "Precision & Sharpness",
        description = "Ultra-clean hairlines, tabular data & electric cobalt"
    ),
    MATERIAL3(
        id = "MATERIAL3",
        displayName = "Material 3 Expressive",
        tagLine = "Expressive & Dynamic",
        description = "Dynamic pill geometry, tonal containers & bouncy physics"
    ),
    NEO_BRUTALISM(
        id = "NEO_BRUTALISM",
        displayName = "Neo-Brutalism",
        tagLine = "Bold & High-Contrast",
        description = "Chunky solid black borders, hard drop shadows & pop accents"
    ),
    HANDWRITTEN(
        id = "HANDWRITTEN",
        displayName = "Handwritten Art",
        tagLine = "Artisanal & Sketchbook",
        description = "Parchment paper, doodle borders, playful scribbles & ink"
    );

    val isLegacy: Boolean get() = this == CLASSIC || this == CRYSTAL

    companion object {
        /** All distinct UI/UX themes selectable in LiNEA. */
        val appThemes: List<DesignStyle> = listOf(
            CLASSIC,
            CRYSTAL,
            NEUMORPHISM,
            ORGANIC,
            FINTECH,
            MATERIAL3,
            NEO_BRUTALISM,
            HANDWRITTEN
        )

        fun fromStored(value: String?): DesignStyle = when (value?.uppercase()?.trim()) {
            "CLASSIC" -> CLASSIC
            "CRYSTAL" -> CRYSTAL
            "NEUMORPHISM" -> NEUMORPHISM
            "ORGANIC", "BIOPHILIC", "ORGANIC_BIOPHILIC" -> ORGANIC
            "FINTECH", "FINTECH_MINIMAL", "MINIMAL" -> FINTECH
            "MATERIAL3", "MATERIAL_3", "EXPRESSIVE", "MATERIAL_EXPRESSIVE" -> MATERIAL3
            "NEO_BRUTALISM", "NEOBRUTALISM", "BRUTALISM" -> NEO_BRUTALISM
            "HANDWRITTEN", "HANDWRITTEN_ART", "SKETCH", "HANDMADE" -> HANDWRITTEN
            else -> CLASSIC
        }
    }
}
