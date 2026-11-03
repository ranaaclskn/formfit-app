package com.ranaaclskn.formfit.ui

import android.speech.tts.TextToSpeech
import com.ranaaclskn.formfit.data.AndroidContext
import java.util.Locale

actual object Speech {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null

    private fun engine(): TextToSpeech = tts ?: TextToSpeech(AndroidContext.app) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val t = tts ?: return@TextToSpeech
            val tr = Locale.forLanguageTag("tr-TR")
            if (t.isLanguageAvailable(tr) >= TextToSpeech.LANG_AVAILABLE) t.language = tr
            ready = true
            pending?.let { speak(it) }
            pending = null
        }
    }.also { tts = it }

    actual fun speak(text: String) {
        val t = engine()
        if (!ready) {
            pending = text
            return
        }
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "formfit")
    }

    actual fun stop() {
        tts?.stop()
    }
}
