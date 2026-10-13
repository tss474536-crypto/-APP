package com.shishi.schedule.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shishi.schedule.data.AppSnapshot
import com.shishi.schedule.data.Course
import com.shishi.schedule.data.JournalEntry
import com.shishi.schedule.data.SceneSettings
import com.shishi.schedule.data.currentWeek
import com.shishi.schedule.data.happensInWeek
import com.shishi.schedule.ocr.OcrResult
import com.shishi.schedule.ocr.recognizeSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")
private val moods = listOf("开心" to "😄", "平静" to "😌", "疲惫" to "😪", "难过" to "😢", "生气" to "😠")

@Composable
fun TodayScreen(
    state: AppSnapshot,
    palette: Palette,
    padding: PaddingValues,
    onSave: (Course) -> Unit,
    onDelete: (String) -> Unit
) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    var editing by remember { mutableStateOf<Course?>(null) }
    val week = state.currentWeek(date)
    val courses = state.courses
        .filter { it.day == date.dayOfWeek.value && it.happensInWeek(week) }
        .sortedBy { it.startPeriod }
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            PageTitle(
                "${state.profile.name}，${greeting()}好",
                "${date.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA))} · 第${week}周"
            )
            WeekStrip(date, onSelect = { date = it })
            Spacer(Modifier.height(12.dp))
        }
        if (courses.isEmpty()) {
            item { EmptyCard("今天没有课，放心去晒太阳吧 🌿") }
        } else {
            courses.groupBy { it.startPeriod to it.endPeriod }.forEach { (slot, grouped) ->
                item {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("第${slot.first}-${slot.second}节", fontWeight = FontWeight.SemiBold, color = palette.primary)
                        if (grouped.size > 1) Text("这组课程时间冲突，已放在一起", style = MaterialTheme.typography.labelMedium, color = palette.accent)
                        grouped.forEach { course ->
                            CourseCard(course, { editing = course }, { onDelete(course.id) })
                        }
                    }
                }
            }
        }
    }
    editing?.let { course -> CourseEditor(course, { editing = null }, onSave) }
}

@Composable
private fun WeekStrip(selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val monday = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        repeat(7) { index ->
            val date = monday.plusDays(index.toLong())
            val active = date == selected
            Surface(
                modifier = Modifier.clickable { onSelect(date) },
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = .70f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dayNames[index], color = if (active) Color.White else MaterialTheme.colorScheme.onSurface)
                    Text(date.dayOfMonth.toString(), fontWeight = FontWeight.Bold, color = if (active) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun ScheduleScreen(state: AppSnapshot, palette: Palette, padding: PaddingValues, model: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Course?>(null) }
    var creating by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var ocrResult by remember { mutableStateOf<OcrResult?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            message = "正在识别课表图片…"
            scope.launch {
                runCatching { withContext(Dispatchers.Default) { recognizeSchedule(context, uri) } }
                    .onSuccess { ocrResult = it; message = if (it.courses.isEmpty()) "识别到文字，但没有自动定位出课程。" else "" }
                    .onFailure { message = "识别失败：${it.message ?: "请换一张清晰截图"}" }
                busy = false
            }
        }
    }
    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
            item {
                PageTitle("本周课表", "第${state.currentWeek()}周 · 冲突课程会叠放在同一时段")
                Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { picker.launch(arrayOf("image/*")) }) {
                        Icon(Icons.Outlined.ImageSearch, null)
                        Text(" 图片导入")
                    }
                    OutlinedButton(onClick = { creating = true }) {
                        Icon(Icons.Outlined.Add, null)
                        Text(" 手动添加")
                    }
                }
                if (busy || message.isNotBlank()) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text(message, Modifier.padding(start = if (busy) 10.dp else 0.dp))
                    }
                }
            }
            dayNames.forEachIndexed { index, name ->
                val dayCourses = state.courses.filter { it.day == index + 1 && it.happensInWeek(state.currentWeek()) }
                    .sortedBy { it.startPeriod }
                item {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("星期$name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primary)
                        if (dayCourses.isEmpty()) {
                            Text("无课", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f))
                        } else {
                            dayCourses.groupBy { it.startPeriod to it.endPeriod }.forEach { (_, group) ->
                                if (group.size > 1) {
                                    Surface(shape = RoundedCornerShape(18.dp), color = palette.accent.copy(alpha = .18f)) {
                                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                            Text("同时段 ${group.size} 门", style = MaterialTheme.typography.labelMedium, color = palette.accent)
                                            group.forEach { c -> CourseCard(c, { editing = c }, { model.deleteCourse(c.id) }) }
                                        }
                                    }
                                } else {
                                    val c = group.first()
                                    CourseCard(c, { editing = c }, { model.deleteCourse(c.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = palette.primary
        ) { Icon(Icons.Outlined.Add, "添加课程", tint = Color.White) }
    }
    if (creating) CourseEditor(null, { creating = false }, model::saveCourse)
    editing?.let { CourseEditor(it, { editing = null }, model::saveCourse) }
    ocrResult?.let { result ->
        OcrConfirmDialog(
            result,
            onDismiss = { ocrResult = null },
            onImport = { model.addCourses(result.courses); ocrResult = null; message = "已导入 ${result.courses.size} 门课程，可逐条点开校对。" }
        )
    }
}

@Composable
private fun OcrConfirmDialog(result: OcrResult, onDismiss: () -> Unit, onImport: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("识别结果") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("自动识别会受截图布局影响，导入后请检查星期和节次。", color = MaterialTheme.colorScheme.primary)
                result.courses.forEach { Text("星期${dayNames[it.day - 1]}  第${it.startPeriod}-${it.endPeriod}节  ${it.name} @${it.room}") }
                if (result.courses.isEmpty()) {
                    Text(result.rawText.take(700).ifBlank { "没有识别到文字" })
                }
            }
        },
        confirmButton = {
            Button(enabled = result.courses.isNotEmpty(), onClick = onImport) { Text("确认导入") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun JournalScreen(state: AppSnapshot, palette: Palette, padding: PaddingValues, onSave: (JournalEntry) -> Unit) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    val existing = state.journals.firstOrNull { it.date == selected.toString() }
    var mood by remember(selected, existing) { mutableStateOf(existing?.mood ?: "平静") }
    var note by remember(selected, existing) { mutableStateOf(existing?.note ?: "") }
    var period by remember(selected, existing) { mutableStateOf(existing?.periodStart ?: false) }
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
        PageTitle("心情手账", "把今天留下一点，不需要写得很伟大")
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            colors = CardDefaults.cardColors(containerColor = palette.paper.copy(alpha = .90f))
        ) {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ month = month.minusMonths(1) }) { Icon(Icons.Outlined.ArrowBack, null) }
                Text("${month.year}年 ${month.monthValue}月", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                IconButton({ month = month.plusMonths(1) }) { Icon(Icons.Outlined.ArrowForward, null) }
            }
            MonthGrid(month, selected, state.journals, palette) { selected = it }
            Spacer(Modifier.height(10.dp))
        }
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${selected.monthValue}月${selected.dayOfMonth}日", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                moods.forEach { (label, emoji) ->
                    Surface(
                        modifier = Modifier.clickable { mood = label },
                        shape = CircleShape,
                        color = if (mood == label) palette.soft else palette.paper.copy(alpha = .75f)
                    ) { Text(emoji, Modifier.padding(11.dp)) }
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth().height(130.dp),
                label = { Text("今天的心情和见解") }
            )
            if (state.profile.gender != "男") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = period, onCheckedChange = { period = it })
                    Text(" 记为经期开始日（仅保存在本机）")
                }
            }
            Button(
                onClick = { onSave(JournalEntry(selected.toString(), mood, note.trim(), if (state.profile.gender == "男") false else period)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存这一天") }
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, selected: LocalDate, entries: List<JournalEntry>, palette: Palette, onSelect: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        dayNames.forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium) }
    }
    val blanks = month.atDay(1).dayOfWeek.value - 1
    val cells = (0 until blanks).map { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            repeat(7) { index ->
                val date = week.getOrNull(index)
                val entry = entries.firstOrNull { it.date == date?.toString() }
                Box(
                    Modifier.weight(1f).height(54.dp).padding(3.dp)
                        .clickable(enabled = date != null) { date?.let(onSelect) },
                    contentAlignment = Alignment.Center
                ) {
                    if (date != null) {
                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = if (date == selected) palette.primary else Color.Transparent
                        ) {
                            Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(date.dayOfMonth.toString(), color = if (date == selected) Color.White else MaterialTheme.colorScheme.onSurface)
                                Text(moods.firstOrNull { it.first == entry?.mood }?.second ?: if (entry?.periodStart == true) "🌙" else "", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(state: AppSnapshot, palette: Palette, padding: PaddingValues, model: AppViewModel) {
    val context = LocalContext.current
    var profileOpen by remember { mutableStateOf(false) }
    var semesterName by remember(state.semesterName) { mutableStateOf(state.semesterName) }
    var semesterStart by remember(state.semesterStart) { mutableStateOf(state.semesterStart) }
    var notice by remember { mutableStateOf("") }
    val videosPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val valid = uris.mapNotNull { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                uri.toString()
            }.getOrNull()
        }
        if (valid.isNotEmpty()) {
            model.saveScenes(state.scenes.copy(videoUris = (state.scenes.videoUris + valid).distinct()))
            notice = "已加入 ${valid.size} 个本地背景视频"
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(model.exportJson()) }
        }.onSuccess { notice = "备份已导出" }.onFailure { notice = "导出失败：${it.message}" }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("文件无法读取")
        }.mapCatching { model.importJson(it).getOrThrow() }
            .onSuccess { notice = "备份已恢复" }.onFailure { notice = "恢复失败：${it.message}" }
    }

    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
        PageTitle("设置", "所有个人数据默认只在这台手机")
        SettingsCard("个人与颜色") {
            Text("${state.profile.name} · ${state.profile.gender} · ${state.profile.theme}")
            OutlinedButton(onClick = { profileOpen = true }) { Text("修改") }
        }
        SettingsCard("学期") {
            OutlinedTextField(semesterName, { semesterName = it }, label = { Text("学期名称") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(semesterStart, { semesterStart = it }, label = { Text("开学日期 YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { model.saveSemester(semesterName, semesterStart); notice = "学期设置已保存" }) { Text("保存学期") }
        }
        SettingsCard("动态风景") {
            Text("内置 6 段海浪、海岸、秋叶、雨景和雪林，可按季节或全部轮播；也可以导入自己的风景视频。")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("季节", "全部风景").forEach { mode ->
                    FilterChip(
                        selected = state.scenes.mode == mode,
                        onClick = {
                            model.saveScenes(state.scenes.copy(mode = mode))
                        },
                        label = { Text(mode) }
                    )
                }
            }
            FilledTonalButton(onClick = { videosPicker.launch(arrayOf("video/*")) }) {
                Icon(Icons.Outlined.UploadFile, null)
                Text(" 导入自己的风景视频")
            }
            if (state.scenes.videoUris.isNotEmpty()) {
                Text("已导入 ${state.scenes.videoUris.size} 个视频")
                FilterChip(
                    selected = state.scenes.mode == "随机视频",
                    onClick = { model.saveScenes(state.scenes.copy(mode = "随机视频")) },
                    label = { Text("轮播我的视频") }
                )
                state.scenes.videoUris.forEachIndexed { index, uri ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("视频 ${index + 1}", Modifier.weight(1f))
                        FilterChip(
                            selected = state.scenes.fixedVideoUri == uri,
                            onClick = { model.saveScenes(state.scenes.copy(mode = "固定视频", fixedVideoUri = uri)) },
                            label = { Text("设为固定") }
                        )
                    }
                }
                TextButton(onClick = { model.saveScenes(SceneSettings()) }) { Text("移除全部自选视频") }
            }
        }
        SettingsCard("备份与换手机") {
            Text("导出的是 JSON 文件，包含课程、手账和设置，请像保护日记一样保管。")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { exportLauncher.launch("诗诗课表备份.json") }) { Text("导出备份") }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) }) { Text("恢复备份") }
            }
        }
        if (notice.isNotBlank()) Text(notice, Modifier.padding(20.dp), color = palette.primary)
        Spacer(Modifier.height(30.dp))
    }
    if (profileOpen) ProfileDialog(state.profile, true, { profileOpen = false }, model::saveProfile)
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .90f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    Surface(
        Modifier.fillMaxWidth().padding(18.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .85f)
    ) { Text(text, Modifier.padding(28.dp), textAlign = TextAlign.Center) }
}

private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..10 -> "早上"
    in 11..13 -> "中午"
    in 14..18 -> "下午"
    else -> "晚上"
}
