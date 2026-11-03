package com.ranaaclskn.formfit.data

/** Geçmişin saklandığı JSON dosyasını okur; yoksa null. */
expect fun readHistoryFile(): String?

expect fun writeHistoryFile(content: String)

expect fun nowMillis(): Long

/** Ör. "3 Kas 2026, 18:42" */
expect fun formatDateTime(epochMillis: Long): String
