package com.shishi.schedule.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shishi.schedule.data.AppSnapshot
import com.shishi.schedule.data.AppStore
import com.shishi.schedule.data.Course
import com.shishi.schedule.data.JournalEntry
import com.shishi.schedule.data.SceneSettings
import com.shishi.schedule.data.UserProfile
import kotlinx.coroutines.flow.StateFlow

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppStore(application)
    val state: StateFlow<AppSnapshot> = store.snapshot

    fun saveProfile(profile: UserProfile) = store.update { it.copy(profile = profile) }

    fun saveSemester(name: String, start: String) = store.update {
        it.copy(semesterName = name.trim(), semesterStart = start.trim())
    }

    fun saveCourse(course: Course) = store.update { old ->
        val exists = old.courses.any { it.id == course.id }
        old.copy(courses = if (exists) {
            old.courses.map { if (it.id == course.id) course else it }
        } else old.courses + course)
    }

    fun addCourses(courses: List<Course>) = store.update { it.copy(courses = it.courses + courses) }

    fun deleteCourse(id: String) = store.update { it.copy(courses = it.courses.filterNot { c -> c.id == id }) }

    fun saveJournal(entry: JournalEntry) = store.update { old ->
        old.copy(journals = old.journals.filterNot { it.date == entry.date } + entry)
    }

    fun saveScenes(settings: SceneSettings) = store.update { it.copy(scenes = settings) }

    fun exportJson(): String = store.exportJson()
    fun importJson(json: String): Result<Unit> = store.importJson(json)
}
