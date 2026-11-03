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

private fun appFilePath(name: String): String {
    val dir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
    return "$dir/$name"
}

@OptIn(ExperimentalForeignApi::class)
actual fun readAppFile(name: String): String? =
    NSString.stringWithContentsOfFile(appFilePath(name), NSUTF8StringEncoding, null)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun writeAppFile(name: String, content: String) {
    NSString.create(string = content).writeToFile(appFilePath(name), atomically = true, encoding = NSUTF8StringEncoding, error = null)
}

actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun formatDateTime(epochMillis: Long): String {
    val f = NSDateFormatter()
    f.locale = NSLocale(localeIdentifier = "tr_TR")
    f.dateFormat = "d MMM yyyy, HH:mm"
    return f.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0))
}
