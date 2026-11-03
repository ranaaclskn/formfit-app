package com.ranaaclskn.formfit.ui

/** Cihazın Türkçe metin okuma (TTS) motoru. Yeni cümle, okunan cümleyi keser. */
expect object Speech {
    fun speak(text: String)
    fun stop()
}
