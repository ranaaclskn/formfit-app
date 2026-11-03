package com.ranaaclskn.formfit.ui

import androidx.compose.runtime.Composable

/** Yedek dosyasını kaydetme ve seçme; platforma göre dosya penceresi açar. */
interface BackupLauncher {
    fun export(fileName: String, content: String)
    fun import()
}

/** [onImported] seçilen dosyanın metniyle (ya da okunamadıysa null) çağrılır. */
@Composable
expect fun rememberBackupLauncher(onExported: (Boolean) -> Unit, onImported: (String?) -> Unit): BackupLauncher
