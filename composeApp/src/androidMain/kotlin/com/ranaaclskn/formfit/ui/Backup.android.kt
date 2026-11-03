package com.ranaaclskn.formfit.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberBackupLauncher(onExported: (Boolean) -> Unit, onImported: (String?) -> Unit): BackupLauncher {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<String?>(null) }
    val exported by rememberUpdatedState(onExported)
    val imported by rememberUpdatedState(onImported)
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val text = pending
        pending = null
        if (uri == null || text == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } != null
        }.getOrDefault(false)
        exported(ok)
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        imported(runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull())
    }
    return remember {
        object : BackupLauncher {
            override fun export(fileName: String, content: String) {
                pending = content
                create.launch(fileName)
            }

            override fun import() = open.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
        }
    }
}
