package com.shishi.schedule.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.shishi.schedule.data.Course
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class OcrResult(val rawText: String, val courses: List<Course>)

suspend fun recognizeSchedule(context: Context, uri: Uri): OcrResult {
    val image = InputImage.fromFilePath(context, uri)
    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    val visionText = suspendCoroutine<Text> { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener { continuation.resumeWithException(it) }
    }
    recognizer.close()
    return OcrResult(
        rawText = visionText.text,
        courses = parseSpatially(visionText, image.width, image.height)
    )
}

private fun parseSpatially(text: Text, imageWidth: Int, imageHeight: Int): List<Course> {
    val ignored = listOf("第", "学期", "课表", "周", "月", "星期", "添加", "广告")
    val roomPattern = Regex("(?:@|＠)?([A-Za-z]?\\d{3,4}[A-Za-z]?)", RegexOption.IGNORE_CASE)
    val result = mutableListOf<Course>()

    text.textBlocks.forEach { block ->
        val box = block.boundingBox ?: return@forEach
        val compact = block.text.replace("\\n", " ").replace(Regex("\\s+"), " ").trim()
        val roomMatch = roomPattern.find(compact) ?: return@forEach
        if (ignored.any { compact == it } || compact.length < 4) return@forEach

        val room = roomMatch.groupValues[1]
        var name = compact.substringBefore("@").substringBefore("＠").trim()
        if (name == compact) name = compact.substring(0, roomMatch.range.first).trim()
        name = name.replace(Regex("^[0-9一二三四五六七日\\s]+"), "").trim()
        if (name.length < 2 || name.all { it.isDigit() }) return@forEach

        val scheduleLeft = imageWidth * 0.07f
        val columnWidth = imageWidth * 0.92f / 7f
        val centerX = box.centerX().toFloat()
        val day = (((centerX - scheduleLeft) / columnWidth).toInt() + 1).coerceIn(1, 7)

        val scheduleTop = imageHeight * 0.20f
        val rowHeight = imageHeight * 0.69f / 8f
        val start = (((box.top - scheduleTop) / rowHeight).toInt() + 1).coerceIn(1, 12)
        val span = (box.height() / rowHeight).toInt().coerceAtLeast(1)
        val end = (start + span).coerceIn(start, 12)

        result += Course(
            name = name,
            day = day,
            startPeriod = start,
            endPeriod = end,
            room = room,
            weeks = "1-20",
            color = result.size % 7
        )
    }

    return result.distinctBy { listOf(it.name, it.day, it.startPeriod, it.room) }
}
