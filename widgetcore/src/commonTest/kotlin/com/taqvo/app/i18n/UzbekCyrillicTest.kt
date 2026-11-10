package com.taqvo.app.i18n

import kotlin.test.Test
import kotlin.test.assertEquals

class UzbekCyrillicTest {

    private fun cyr(latin: String) = UzbekCyrillic.fromLatin(latin)

    @Test
    fun theLettersThatAreTwoInLatinAreOneInCyrillic() {
        assertEquals("Ўзбекча", cyr("Oʻzbekcha"))
        assertEquals("ғоя", cyr("gʻoya"))
        assertEquals("Шом", cyr("Shom"))
        assertEquals("яхши", cyr("yaxshi"))
        assertEquals("Ёрдам", cyr("Yordam"))
        assertEquals("юклаб", cyr("yuklab"))
    }

    @Test
    fun theTutuqBecomesAHardSignAndEOpensAWordAsE() {
        assertEquals("Қуръон", cyr("Qur’on"))
        assertEquals("маъно", cyr("ma’no"))
        assertEquals("экран", cyr("ekran"))
        assertEquals("бекор", cyr("bekor"))
    }

    @Test
    fun placeholdersBoldRunsAndBrandNamesAreLeftAlone() {
        assertEquals("%1\$s та оят", cyr("%1\$s ta oyat"))
        assertEquals("{prayer}гача", cyr("{prayer}gacha"))
        assertEquals("Тугмани босинг: **Add Widget**", cyr("Tugmani bosing: **Add Widget**"))
        assertEquals("Taqvo сизни кутмоқда", cyr("Taqvo sizni kutmoqda"))
    }

    @Test
    fun capitalsFollowTheLatinOnes() {
        assertEquals("КУН ОЯТИ", cyr("KUN OYATI"))
        assertEquals("ШОМ", cyr("SHOM"))
        assertEquals("Чоршанба", cyr("Chorshanba"))
    }
}
