package com.shishi.schedule.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = Color.White.copy(alpha = .94f),
                        shadowElevation = 9.dp
                    ) {
                        Row(Modifier.fillMaxWidth().height(74.dp).padding(5.dp)) {
                        tabs.forEachIndexed { index, tab ->
                            val active = selected == index
                            Surface(
                                modifier = Modifier.weight(1f).clickable { selected = index },
                                shape = RoundedCornerShape(24.dp),
                                color = if (active) palette.primary else Color.Transparent,
                                contentColor = if (active) Color.White else Color(0xFF6E8386)
                            ) {
                                Column(
                                    Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                                ) {
                                    Box(Modifier.size(25.dp), contentAlignment = Alignment.Center) { tab.icon() }
                                    Text(tab.label, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        }
                    }
                }
            ) { padding ->
                when (selected) {
                    0 -> RedesignedTodayScreen(state, palette, padding, model::saveCourse, model::deleteCourse)
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
