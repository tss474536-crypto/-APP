package com.shishi.schedule.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private data class AppTab(val label: String, val icon: @Composable () -> Unit)

@Composable
fun ShishiApp(model: AppViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val palette = paletteOf(state.profile.theme)
    var selected by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        AppTab("今天") { Icon(Icons.Outlined.Today, null) },
        AppTab("课表") { Icon(Icons.Outlined.CalendarMonth, null) },
        AppTab("手账") { Icon(Icons.Outlined.EditNote, null) },
        AppTab("设置") { Icon(Icons.Outlined.Settings, null) }
    )

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = palette.primary,
            secondary = palette.accent,
            background = palette.paper,
            surface = palette.paper,
            surfaceVariant = palette.soft,
            onPrimary = Color.White,
            onBackground = Color(0xFF302F2D),
            onSurface = Color(0xFF302F2D)
        )
    ) {
        Box(Modifier.fillMaxSize()) {
            DynamicBackdrop(state.scenes, palette)
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(containerColor = palette.paper.copy(alpha = .94f)) {
                        tabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = selected == index,
                                onClick = { selected = index },
                                icon = tab.icon,
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            ) { padding ->
                when (selected) {
                    0 -> TodayScreen(state, palette, padding, model::saveCourse, model::deleteCourse)
                    1 -> ScheduleScreen(state, palette, padding, model)
                    2 -> JournalScreen(state, palette, padding, model::saveJournal)
                    else -> SettingsScreen(state, palette, padding, model)
                }
            }
        }
        if (state.profile.name.isBlank()) {
            ProfileDialog(
                initial = state.profile,
                dismissible = false,
                onDismiss = {},
                onSave = model::saveProfile
            )
        }
    }
}
