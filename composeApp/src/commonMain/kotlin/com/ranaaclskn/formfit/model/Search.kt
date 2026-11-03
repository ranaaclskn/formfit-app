package com.ranaaclskn.formfit.model

/** Aramada Türkçe karakter ve büyük/küçük harf farkını yok sayar: "SINAV" = "şınav" = "sinav". */
fun normalizeForSearch(text: String): String = buildString {
    for (c in text) {
        append(
            when (c) {
                'İ', 'I', 'ı', 'i' -> 'i'
                'Ç', 'ç' -> 'c'
                'Ğ', 'ğ' -> 'g'
                'Ö', 'ö' -> 'o'
                'Ş', 'ş' -> 's'
                'Ü', 'ü' -> 'u'
                else -> c.lowercaseChar()
            }
        )
    }
}

/** Hareket adında, kaslarda, kategoride ya da alette geçen her kelime aranır. */
fun Exercise.matches(query: String): Boolean {
    val words = normalizeForSearch(query).split(' ').filter { it.isNotBlank() }
    if (words.isEmpty()) return true
    val haystack = normalizeForSearch(
        (listOf(name, upperName ?: "", category.title, gear.title) + muscles).joinToString(" ")
    )
    return words.all { it in haystack }
}
