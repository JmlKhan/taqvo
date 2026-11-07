package com.taqvo.app.feed

/**
 * How the feed's words look (feed spec §7): one style for every card, the share image and the
 * preview, saved the moment it changes. Ids rather than enums are stored, so a font or colour
 * removed in a later build falls back to the default instead of failing to read.
 */
data class FeedTextStyle(
    val translationFont: TranslationFont = TranslationFont.LORA,
    val arabicFont: ArabicFont = ArabicFont.AMIRI_QURAN,
    val sizePct: Int = 100,
    val color: TextColor = TextColor.AUTO,
    val align: TextAlignment = TextAlignment.CENTER,
) {
    companion object {
        const val MIN_SIZE = 80
        const val MAX_SIZE = 140
        const val SIZE_STEP = 5
    }
}

/**
 * The translation faces. Each covers every letter content.db uses, Uzbek's ʻ and ʼ and the Uzbek
 * Cyrillic қ ғ ҳ ў among them — Manrope, the interface face, lacks those, so it is not offered.
 */
enum class TranslationFont(val id: String) {
    LORA("lora"),
    NUNITO("nunito"),
    SYSTEM("system"),
    ;

    companion object {
        fun of(id: String?): TranslationFont = entries.firstOrNull { it.id == id } ?: LORA
    }
}

/**
 * The Arabic faces. Only Amiri Quran draws content.db's open-tanween marks (U+08F0–08F2), so it is
 * the only Quran face the feed offers; the bundled KFGQPC face shows them as empty boxes.
 */
enum class ArabicFont(val id: String) {
    AMIRI_QURAN("amiri_quran"),
    ;

    companion object {
        fun of(id: String?): ArabicFont = entries.firstOrNull { it.id == id } ?: AMIRI_QURAN
    }
}

/** [AUTO] is the wallpaper's own ink, which reads on the light Dawn wash as well as the dark ones. */
enum class TextColor(val id: String) {
    AUTO("auto"),
    WHITE("white"),
    CREAM("cream"),
    BLACK("black"),
    AMBER("amber"),
    SAGE("sage"),
    ROSE("rose"),
    ;

    companion object {
        fun of(id: String?): TextColor = entries.firstOrNull { it.id == id } ?: AUTO
    }
}

enum class TextAlignment(val id: String) {
    CENTER("center"),
    START("start"),
    ;

    companion object {
        fun of(id: String?): TextAlignment = entries.firstOrNull { it.id == id } ?: CENTER
    }
}

/**
 * The paywall seam (feed spec, global rules): every [PREMIUM?] choice asks this, and today it
 * answers yes for everything, so nothing is locked. The licences of the translations the feed shows
 * are non-commercial (docs/ATTRIBUTION.md), which is why none of it is paid yet.
 */
object FeedPremium {
    var isPremium: Boolean = true

    /** The free choices if [isPremium] were ever false (feed spec §7): two of each. */
    fun allowed(font: TranslationFont): Boolean = isPremium || font == TranslationFont.LORA || font == TranslationFont.SYSTEM
    fun allowed(color: TextColor): Boolean = isPremium || color == TextColor.AUTO || color == TextColor.WHITE
    fun canHideWatermark(): Boolean = isPremium
    fun collectionLimit(): Int = if (isPremium) Int.MAX_VALUE else 1
}
