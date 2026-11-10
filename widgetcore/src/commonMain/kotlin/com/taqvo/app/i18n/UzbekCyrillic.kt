package com.taqvo.app.i18n

/**
 * Uzbek written in the Latin script (2021 orthography) turned into the Cyrillic one.
 *
 * The app's Cyrillic copy is the Latin copy run through this, so the two cannot drift apart, and
 * the tables that are written in code (prayer names, Hijri months, notification words) get their
 * Cyrillic row the same way. The mapping is the official correspondence table plus the three
 * context rules Uzbek spelling needs:
 *
 *  - `e` opens a word as `э` and is `е` elsewhere;
 *  - `ye`, `ya`, `yu`, `yo` are the single letters `е`, `я`, `ю`, `ё`;
 *  - `oʻ` and `gʻ` (written with ‘ ʻ or a plain apostrophe) are `ў` and `ғ`, while the tutuq
 *    belgisi (’ ʼ) between letters is `ъ`.
 *
 * Words that are not Uzbek and must not be respelled — brand names, and any word with a letter the
 * alphabet does not have (`w`, a lone `c`) — are left as they are, as is anything between `**`
 * marks (the names of buttons in the phone's own, Latin, settings) and the `%1$s` placeholders.
 */
object UzbekCyrillic {
    private val KEEP_WORDS = setOf(
        "Taqvo", "Android", "iPhone", "iOS", "iPad", "Google", "Apple", "Safari", "Chrome", "Samsung",
        "TestFlight", "App", "Store", "Play", "GitHub", "Wi", "Fi", "WiFi", "TTS", "MB", "GB", "KB",
        "Manrope", "Amiri", "Lora", "Nunito", "OFL", "CC", "BY", "SA", "PDF", "URL", "OK", "AM", "PM",
        "Tasma", "Hijri", "Quran", "Sahih", "Saheeh", "QuranEnc", "Kuliev", "Kulieva",
    )

    private val NOT_UZBEK_C = Regex("c(?!h)")

    private val PLACEHOLDER =Regex("""%(\d+\$)?[sd]|%%|\\n|\\'|&[a-zA-Z]+;|\{[a-z]+}|<[^>]+>""")

    fun fromLatin(text: String): String {
        if (text.isEmpty()) return text
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            if (text.startsWith("**", i)) {
                // A bold run names something in the phone's own settings: Latin, so copied as it is.
                val end = text.indexOf("**", i + 2)
                if (end > 0) {
                    out.append(text, i, end + 2)
                    i = end + 2
                    continue
                }
            }
            val placeholder = PLACEHOLDER.matchAt(text, i)
            if (placeholder != null) {
                out.append(placeholder.value)
                i = placeholder.range.last + 1
                continue
            }
            if (isLetter(text[i])) {
                var j = i
                while (j < text.length && (isLetter(text[j]) || (isApostrophe(text[j]) && j + 1 < text.length && isLetter(text[j + 1]) && j > i))) j++
                out.append(word(text.substring(i, j)))
                i = j
            } else {
                out.append(text[i])
                i++
            }
        }
        return out.toString()
    }

    private fun isLetter(c: Char) = c in 'a'..'z' || c in 'A'..'Z'

    private fun isApostrophe(c: Char) = c == '\'' || c == '‘' || c == '’' || c == 'ʻ' || c == 'ʼ' || c == '`'

    private fun word(w: String): String {
        val letters = w.filter { isLetter(it) }
        val lower = letters.lowercase()
        if (letters in KEEP_WORDS || lower.contains('w') || NOT_UZBEK_C.containsMatchIn(lower)) return w
        val sb = StringBuilder()
        var i = 0
        while (i < w.length) {
            val c = w[i]
            val lc = c.lowercaseChar()
            val next = w.getOrNull(i + 1)
            val lnext = next?.lowercaseChar()
            val upper = c.isUpperCase()
            // The next letter in capitals means the whole word is: "SH" is Ш, not Шh.
            val shout = upper && next != null && next.isUpperCase()
            fun put(cyr: String, consumed: Int = 1) {
                sb.append(if (upper) (if (shout && cyr.length > 1) cyr.uppercase() else cyr.replaceFirstChar { it.uppercaseChar() }) else cyr)
                i += consumed
            }
            when {
                (lc == 'o' || lc == 'g') && next != null && isApostrophe(next) && next != '’' && next != 'ʼ' ->
                    put(if (lc == 'o') "ў" else "ғ", 2)
                lc == 's' && lnext == 'h' -> put("ш", 2)
                lc == 'c' && lnext == 'h' -> put("ч", 2)
                lc == 'y' && lnext == 'a' -> put("я", 2)
                lc == 'y' && lnext == 'u' -> put("ю", 2)
                lc == 'y' && lnext == 'o' -> put("ё", 2)
                lc == 'y' && lnext == 'e' -> put("е", 2)
                lc == 'e' && i == 0 -> put("э")
                (c == '’' || c == 'ʼ' || c == '\'' || c == '‘' || c == 'ʻ' || c == '`') -> { sb.append('ъ'); i++ }
                else -> {
                    val cyr = SINGLE[lc]
                    if (cyr == null) { sb.append(c); i++ } else put(cyr)
                }
            }
        }
        return sb.toString()
    }

    private val SINGLE = mapOf(
        'a' to "а", 'b' to "б", 'd' to "д", 'e' to "е", 'f' to "ф", 'g' to "г", 'h' to "ҳ", 'i' to "и",
        'j' to "ж", 'k' to "к", 'l' to "л", 'm' to "м", 'n' to "н", 'o' to "о", 'p' to "п", 'q' to "қ",
        'r' to "р", 's' to "с", 't' to "т", 'u' to "у", 'v' to "в", 'x' to "х", 'y' to "й", 'z' to "з",
    )
}
