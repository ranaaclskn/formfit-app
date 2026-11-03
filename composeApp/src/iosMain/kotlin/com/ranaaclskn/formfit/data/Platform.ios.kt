package com.ranaaclskn.formfit.data

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSLocale
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile

private fun historyPath(): String {
    val dir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
    return "$dir/history.json"
}

@OptIn(ExperimentalForeignApi::class)
actual fun readHistoryFile(): String? =
    NSString.stringWithContentsOfFile(historyPath(), NSUTF8StringEncoding, null)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun writeHistoryFile(content: String) {
    NSString.create(string = content).writeToFile(historyPath(), atomically = true, encoding = NSUTF8StringEncoding, error = null)
}

actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun formatDateTime(epochMillis: Long): String {
    val f = NSDateFormatter()
    f.locale = NSLocale(localeIdentifier = "tr_TR")
    f.dateFormat = "d MMM yyyy, HH:mm"
    return f.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0))
}
