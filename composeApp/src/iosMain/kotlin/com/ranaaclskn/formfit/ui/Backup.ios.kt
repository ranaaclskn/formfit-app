package com.ranaaclskn.formfit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject

private class PickerDelegate(var onPick: (String?) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
    @OptIn(ExperimentalForeignApi::class)
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        val access = url?.startAccessingSecurityScopedResource() ?: false
        val text = url?.path?.let { NSString.stringWithContentsOfFile(it, NSUTF8StringEncoding, null) }
        if (access) url?.stopAccessingSecurityScopedResource()
        onPick(text)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) = Unit
}

@Suppress("DEPRECATION")
private fun rootController() = UIApplication.sharedApplication.keyWindow?.rootViewController

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun rememberBackupLauncher(onExported: (Boolean) -> Unit, onImported: (String?) -> Unit): BackupLauncher {
    val exported by rememberUpdatedState(onExported)
    val imported by rememberUpdatedState(onImported)
    // Seçici açıkken temsilci yaşamalı; remember ile tutulur
    val delegate = remember { PickerDelegate { imported(it) } }
    return remember {
        object : BackupLauncher {
            override fun export(fileName: String, content: String) {
                val path = NSTemporaryDirectory() + fileName
                val ok = NSString.create(string = content).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
                if (ok) {
                    val share = UIActivityViewController(activityItems = listOf(NSURL.fileURLWithPath(path)), applicationActivities = null)
                    rootController()?.presentViewController(share, animated = true, completion = null)
                }
                exported(ok)
            }

            override fun import() {
                val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeJSON))
                picker.delegate = delegate
                rootController()?.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}
