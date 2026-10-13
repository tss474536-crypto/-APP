package com.shishi.schedule.data

import android.content.Context
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppStore(context: Context) {
    private val preferences = context.getSharedPreferences("shishi_schedule", Context.MODE_PRIVATE)
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val _snapshot = MutableStateFlow(load())
    val snapshot: StateFlow<AppSnapshot> = _snapshot.asStateFlow()

    @Synchronized
    fun update(transform: (AppSnapshot) -> AppSnapshot) {
        val next = transform(_snapshot.value)
        _snapshot.value = next
        preferences.edit().putString(KEY, gson.toJson(next)).apply()
    }

    fun exportJson(): String = gson.toJson(_snapshot.value)

    fun importJson(json: String): Result<Unit> = runCatching {
        val parsed = gson.fromJson(json, AppSnapshot::class.java)
            ?: error("备份文件为空")
        require(parsed.courses.all { it.day in 1..7 && it.startPeriod in 1..12 && it.endPeriod in 1..12 }) {
            "课表数据格式不正确"
        }
        _snapshot.value = parsed
        preferences.edit().putString(KEY, gson.toJson(parsed)).commit()
    }

    private fun load(): AppSnapshot {
        val json = preferences.getString(KEY, null) ?: return AppSnapshot()
        return runCatching { gson.fromJson(json, AppSnapshot::class.java) }
            .getOrNull() ?: AppSnapshot()
    }

    companion object { private const val KEY = "snapshot_v1" }
}
