package com.pairshot.core.rendering

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileNameGenerator
@Inject
constructor() {
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
    private val timeFormat = SimpleDateFormat("HHmmss", Locale.US)

    fun generateBeforeFileName(
        sequenceNumber: Int,
        prefix: String = "",
    ): String {
        val now = Date()
        val date = dateFormat.format(now)
        val time = timeFormat.format(now)
        val prefixPart = if (prefix.isNotEmpty()) "${prefix}_" else ""
        return "${prefixPart}BEFORE_%03d_%s_%s.jpg".format(sequenceNumber, date, time)
    }

    fun generateAfterFileName(
        sequenceNumber: Int,
        prefix: String = "",
    ): String {
        val now = Date()
        val date = dateFormat.format(now)
        val time = timeFormat.format(now)
        val prefixPart = if (prefix.isNotEmpty()) "${prefix}_" else ""
        return "${prefixPart}AFTER_%03d_%s_%s.jpg".format(sequenceNumber, date, time)
    }

    fun generatePairFileName(
        sequenceNumber: Int,
        prefix: String = "",
    ): String {
        val now = Date()
        val date = dateFormat.format(now)
        val time = timeFormat.format(now)
        val prefixPart = if (prefix.isNotEmpty()) "${prefix}_" else ""
        return "${prefixPart}PAIR_%03d_%s_%s.jpg".format(sequenceNumber, date, time)
    }
}
