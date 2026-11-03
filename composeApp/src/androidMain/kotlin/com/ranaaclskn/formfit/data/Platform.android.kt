package com.ranaaclskn.formfit.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object AndroidContext {
    lateinit var app: Context
}

private fun historyFile() = File(AndroidContext.app.filesDir, "history.json")

actual fun readHistoryFile(): String? = historyFile().takeIf { it.exists() }?.readText()

actual fun writeHistoryFile(content: String) {
    // Önce geçici dosyaya yaz, sonra taşı: yazarken uygulama kapanırsa geçmiş bozulmasın
    val tmp = File(AndroidContext.app.filesDir, "history.json.tmp")
    tmp.writeText(content)
    tmp.renameTo(historyFile())
}

actual fun nowMillis(): Long = System.currentTimeMillis()

actual fun formatDateTime(epochMillis: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("tr")).format(Date(epochMillis))
