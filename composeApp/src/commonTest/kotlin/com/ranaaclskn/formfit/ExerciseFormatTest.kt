package com.ranaaclskn.formfit

import com.ranaaclskn.formfit.model.EXERCISES
import kotlin.test.Test
import kotlin.test.fail

/**
 * Her hareketin açıklamaları aynı formatta ve ayrıntıda olmalı:
 * en az 3 kas grubu, kamera yerleşimi, 4 adım, en az 3 dikkat maddesi ve 3 püf noktası;
 * tüm cümleler noktayla biter, büyük harfli başlıkta küçük harf yoktur.
 */
class ExerciseFormatTest {
    @Test
    fun everyExerciseHasTheSameDetailedFormat() {
        val problems = mutableListOf<String>()
        for (ex in EXERCISES) {
            fun bad(msg: String) { problems += "${ex.id}: $msg" }
            if (ex.muscles.size < 3) bad("kas grubu ${ex.muscles.size} (en az 3)")
            if (ex.steps.size != 4) bad("adım sayısı ${ex.steps.size} (4 olmalı)")
            if (ex.cautions.size < 3) bad("dikkat maddesi ${ex.cautions.size} (en az 3)")
            if (ex.tips.size < 3) bad("püf noktası ${ex.tips.size} (en az 3)")
            if (ex.setup.length < 40) bad("kamera yerleşimi çok kısa")
            val sentences = listOf(ex.setup) + ex.steps + ex.cautions + ex.tips
            sentences.filterNot { it.trimEnd().endsWith(".") }.forEach { bad("noktayla bitmiyor: \"$it\"") }
            sentences.filter { it.length < 20 }.forEach { bad("çok kısa cümle: \"$it\"") }
            ex.upperName?.let { if (it.any { c -> c.isLowerCase() }) bad("büyük harfli başlıkta küçük harf: $it") }
            if (ex.muscles.any { it.isBlank() }) bad("boş kas grubu")
        }
        if (problems.isNotEmpty()) fail("${problems.size} sorun:\n" + problems.joinToString("\n"))
    }
}
