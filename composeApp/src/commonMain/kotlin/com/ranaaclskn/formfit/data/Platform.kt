package com.ranaaclskn.formfit.data

/** Uygulamanın kendi klasöründeki bir dosyayı okur; yoksa null. */
expect fun readAppFile(name: String): String?

/** Dosyayı güvenli şekilde (önce geçici dosyaya) yazar. */
expect fun writeAppFile(name: String, content: String)

expect fun nowMillis(): Long

/** Ör. "3 Kas 2026, 18:42" */
expect fun formatDateTime(epochMillis: Long): String
