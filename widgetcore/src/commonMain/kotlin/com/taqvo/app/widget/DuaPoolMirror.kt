package com.taqvo.app.widget

/**
 * One supplication as mirrored for the dua widget: its number in Hisn al-Muslim, the Arabic and
 * its translation in the interface language (empty where that language has none, as Arabic does).
 */
data class DuaPoolEntry(
    val hisn: Int,
    val arabic: String,
    val translation: String,
)

/**
 * The dua widget's snapshot, the twin of [AyahPoolMirror]: the app writes it into the widget
 * [KeyValueStore] beside the ayah pool, and both widget processes (Android Glance, iOS WidgetKit)
 * read it without ever opening the adhkar file themselves. The pool is the adhkar's supplications
 * said at any hour and short enough for a card (the wallpaper's own rule, `WallpaperTexts.duas`).
 */
data class DuaPoolMirror(
    val languageTag: String,
    /** The translation is written right to left (Urdu). */
    val translationRtl: Boolean,
    /** The app's own digit set when the mirror was written, as [AyahPoolMirror.arabicIndicDigits]. */
    val arabicIndicDigits: Boolean,
    val entries: List<DuaPoolEntry>,
) {
    /**
     * The entry to show on [epochDay], or null when there are none. The install's ayah seed turned
     * by [SEED_SALT], so the two cards share the install's randomness without moving in step.
     */
    fun entryFor(epochDay: Long, ayahSeed: Long): DuaPoolEntry? =
        entries.takeIf { it.isNotEmpty() }?.let { it[AyahRotation.indexFor(epochDay, ayahSeed xor SEED_SALT, it.size)] }

    companion object {
        /** The [KeyValueStore] key the serialised mirror is written under. */
        const val KEY = "dua_pool"

        const val VERSION = 1

        private const val SEED_SALT = 0x6475615F706F6F6CL // "dua_pool"
        private const val ENTRY_SEP = AyahPoolMirror.ENTRY_SEP
        private const val FIELD_SEP = AyahPoolMirror.FIELD_SEP
        private const val HEADER_FIELD_COUNT = 4
        private const val ENTRY_FIELD_COUNT = 3

        /**
         * Wire form, the ayah pool's shape: a header (`version`, `languageTag`, `0`/`1` for
         * [translationRtl], `0`/`1` for [arabicIndicDigits]) and one block per entry (`hisn`,
         * `arabic`, `translation`), blocks joined with [ENTRY_SEP] and fields with [FIELD_SEP].
         */
        fun serialize(m: DuaPoolMirror): String {
            val header = listOf(
                VERSION.toString(),
                m.languageTag,
                if (m.translationRtl) "1" else "0",
                if (m.arabicIndicDigits) "1" else "0",
            ).joinToString(FIELD_SEP.toString())
            val blocks = m.entries.map { e ->
                listOf(e.hisn.toString(), e.arabic, e.translation).joinToString(FIELD_SEP.toString())
            }
            return (listOf(header) + blocks).joinToString(ENTRY_SEP.toString())
        }

        /** The inverse of [serialize]; null, never a throw, on anything else (an older version included). */
        fun deserialize(raw: String): DuaPoolMirror? {
            if (raw.isEmpty()) return null
            val blocks = raw.split(ENTRY_SEP)
            val header = blocks.first().split(FIELD_SEP)
            if (header.size != HEADER_FIELD_COUNT || header[0] != VERSION.toString()) return null
            val translationRtl = flag(header[2]) ?: return null
            val arabicIndicDigits = flag(header[3]) ?: return null
            val entries = blocks.drop(1).map { block ->
                val fields = block.split(FIELD_SEP)
                if (fields.size != ENTRY_FIELD_COUNT) return null
                DuaPoolEntry(hisn = fields[0].toIntOrNull() ?: return null, arabic = fields[1], translation = fields[2])
            }
            return DuaPoolMirror(header[1], translationRtl, arabicIndicDigits, entries)
        }

        private fun flag(value: String): Boolean? = when (value) {
            "0" -> false
            "1" -> true
            else -> null
        }

        fun read(store: KeyValueStore): DuaPoolMirror? = store.getString(KEY)?.let(::deserialize)

        fun write(store: KeyValueStore, m: DuaPoolMirror) {
            store.putString(KEY, serialize(m))
        }
    }
}
