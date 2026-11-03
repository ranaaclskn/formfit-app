package com.ranaaclskn.formfit.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object AndroidContext {
    lateinit var app: Context
}

actual fun readAppFile(name: String): String? =
    File(AndroidContext.app.filesDir, name).takeIf { it.exists() }?.readText()

actual fun writeAppFile(name: String, content: String) {
    // Önce geçici dosyaya yaz, sonra taşı: yazarken uygulama kapanırsa dosya bozulmasın
    val tmp = File(AndroidContext.app.filesDir, "$name.tmp")
    tmp.writeText(content)
    tmp.renameTo(File(AndroidContext.app.filesDir, name))
}

actual fun nowMillis(): Long = System.currentTimeMillis()

actual fun formatDateTime(epochMillis: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("tr")).format(Date(epochMillis))
