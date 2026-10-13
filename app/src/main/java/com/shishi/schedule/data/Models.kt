package com.shishi.schedule.data

import java.time.LocalDate
import java.util.UUID

data class UserProfile(
    val name: String = "",
    val gender: String = "女",
    val theme: String = "鼠尾草"
)

data class Course(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val day: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val room: String = "",
    val teacher: String = "",
    val weeks: String = "1-20",
    val color: Int = 0,
    val isRetake: Boolean = false
)

data class JournalEntry(
    val date: String,
    val mood: String = "平静",
    val note: String = "",
    val periodStart: Boolean = false
)

data class SceneSettings(
    val mode: String = "季节",
    val videoUris: List<String> = emptyList(),
    val fixedVideoUri: String = ""
)

data class AppSnapshot(
    val profile: UserProfile = UserProfile(),
    val semesterName: String = "2026 秋季学期",
    val semesterStart: String = "2026-09-07",
    val courses: List<Course> = defaultCourses(),
    val journals: List<JournalEntry> = emptyList(),
    val scenes: SceneSettings = SceneSettings()
)

fun Course.happensInWeek(week: Int): Boolean {
    if (week < 1) return false
    val normalized = weeks.replace("周", "").replace("第", "")
    val oddOnly = normalized.contains("单")
    val evenOnly = normalized.contains("双")
    if (oddOnly && week % 2 == 0) return false
    if (evenOnly && week % 2 != 0) return false
    val parts = normalized.replace("，", ",").split(",", ";", "；")
    return parts.any { part ->
        val numbers = Regex("\\d+").findAll(part).map { it.value.toInt() }.toList()
        when {
            numbers.isEmpty() -> false
            part.contains("-") && numbers.size >= 2 -> week in numbers[0]..numbers[1]
            else -> week in numbers
        }
    }
}

fun AppSnapshot.currentWeek(date: LocalDate = LocalDate.now()): Int = runCatching {
    val start = LocalDate.parse(semesterStart)
    ((date.toEpochDay() - start.toEpochDay()) / 7 + 1).toInt()
}.getOrDefault(1).coerceAtLeast(1)

fun defaultCourses(): List<Course> = listOf(
    Course(name = "建筑智能环境学", day = 1, startPeriod = 1, endPeriod = 2, room = "1204D", weeks = "2-4,6-17", color = 4),
    Course(name = "建筑智能环境学", day = 1, startPeriod = 5, endPeriod = 6, room = "1204D", weeks = "2-4,6-17", color = 4),
    Course(name = "自动控制原理", day = 1, startPeriod = 7, endPeriod = 8, room = "2305Z", weeks = "1-14", color = 0),
    Course(name = "建筑供配电与照明", day = 2, startPeriod = 1, endPeriod = 2, room = "5303D", weeks = "2-4,6-17", color = 1),
    Course(name = "自动控制原理", day = 2, startPeriod = 3, endPeriod = 4, room = "6305D", weeks = "1-14", color = 0),
    Course(name = "计算机网络与通信", day = 2, startPeriod = 5, endPeriod = 6, room = "5307D", weeks = "1-8", color = 2),
    Course(name = "信息设施系统", day = 2, startPeriod = 7, endPeriod = 8, room = "1204D", weeks = "1-8", color = 3),
    Course(name = "建筑智能环境学", day = 3, startPeriod = 1, endPeriod = 2, room = "1204D", weeks = "2-4,6-17", color = 4),
    Course(name = "电子技术基础", day = 3, startPeriod = 3, endPeriod = 4, room = "6304D", weeks = "1-10,14-15", color = 5, isRetake = true),
    Course(name = "计算机网络与通信", day = 3, startPeriod = 5, endPeriod = 6, room = "5307D", weeks = "1-8", color = 2),
    Course(name = "建筑设备", day = 3, startPeriod = 7, endPeriod = 8, room = "5303D", weeks = "1-12", color = 0),
    Course(name = "建筑设备", day = 4, startPeriod = 1, endPeriod = 2, room = "5303D", weeks = "1-12", color = 0),
    Course(name = "马克思主义基本原理", day = 4, startPeriod = 3, endPeriod = 4, room = "1303D", weeks = "1-12", color = 6),
    Course(name = "电子技术基础", day = 4, startPeriod = 7, endPeriod = 8, room = "1510D", weeks = "1-10,14-15", color = 5, isRetake = true),
    Course(name = "马克思主义基本原理", day = 5, startPeriod = 1, endPeriod = 2, room = "6305D", weeks = "1-12", color = 6),
    Course(name = "信息设施系统", day = 5, startPeriod = 3, endPeriod = 4, room = "1204D", weeks = "1-8", color = 3)
)
