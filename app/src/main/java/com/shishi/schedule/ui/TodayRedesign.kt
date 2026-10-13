package com.shishi.schedule.ui

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.shishi.schedule.data.AppSnapshot
import com.shishi.schedule.data.Course
import com.shishi.schedule.data.SceneSettings
import com.shishi.schedule.data.currentWeek
import com.shishi.schedule.data.happensInWeek
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val START_PAGE = 10_000
private const val PAGE_COUNT = 20_001
private val redesignedDayNames = listOf("一", "二", "三", "四", "五", "六", "日")

private data class ScenicVideo(val title: String, val url: String)

private val builtInScenicVideos = listOf(
    ScenicVideo("海岸晨浪", "https://videos.pexels.com/video-files/11441789/11441789-hd_1080_1920_60fps.mp4"),
    ScenicVideo("温柔潮汐", "https://videos.pexels.com/video-files/6189653/6189653-hd_1080_1920_30fps.mp4"),
    ScenicVideo("金色海面", "https://videos.pexels.com/video-files/8366022/8366022-hd_1080_1920_25fps.mp4"),
    ScenicVideo("秋叶林间", "https://videos.pexels.com/video-files/5873317/5873317-hd_1080_1920_24fps.mp4"),
    ScenicVideo("雨落窗前", "https://videos.pexels.com/video-files/8549582/8549582-hd_1080_1920_25fps.mp4"),
    ScenicVideo("冬日雪林", "https://videos.pexels.com/video-files/6316369/6316369-hd_1080_1920_25fps.mp4")
)

@Composable
fun RedesignedTodayScreen(
    state: AppSnapshot,
    palette: Palette,
    padding: PaddingValues,
    onSave: (Course) -> Unit,
    onDelete: (String) -> Unit
) {
    val baseDate = remember { LocalDate.now() }
    val pager = rememberPagerState(initialPage = START_PAGE, pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val selectedDate = baseDate.plusDays((pager.currentPage - START_PAGE).toLong())
    val week = state.currentWeek(selectedDate)
    var editing by remember { mutableStateOf<Course?>(null) }
    var creating by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    selectedDate.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)),
                    color = palette.primary.copy(alpha = .84f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${greetingForRedesign()}好，${state.profile.name}",
                    fontSize = 29.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF153B43)
                )
            }
            FloatingActionButton(
                onClick = { creating = true },
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = palette.primary,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(5.dp)
            ) { Icon(Icons.Outlined.Add, "添加课程", Modifier.size(30.dp)) }
        }

        HeroSceneCard(state, palette, week)
        Spacer(Modifier.height(14.dp))
        RedesignedWeekStrip(selectedDate) { date ->
            val page = START_PAGE + (date.toEpochDay() - baseDate.toEpochDay()).toInt()
            if (page in 0 until PAGE_COUNT) scope.launch { pager.animateScrollToPage(page) }
        }
        Spacer(Modifier.height(10.dp))

        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val pageDate = baseDate.plusDays((page - START_PAGE).toLong())
            val pageWeek = state.currentWeek(pageDate)
            val courses = state.courses
                .filter { it.day == pageDate.dayOfWeek.value && it.happensInWeek(pageWeek) }
                .sortedBy { it.startPeriod }
            CourseDayList(
                date = pageDate,
                courses = courses,
                palette = palette,
                isToday = pageDate == LocalDate.now(),
                onToday = { scope.launch { pager.animateScrollToPage(START_PAGE) } },
                onEdit = { editing = it },
                onDelete = onDelete
            )
        }
    }

    if (creating) CourseEditor(null, { creating = false }, onSave)
    editing?.let { course -> CourseEditor(course, { editing = null }, onSave) }
}

@Composable
private fun HeroSceneCard(state: AppSnapshot, palette: Palette, week: Int) {
    val mediaUris = remember(state.scenes) { sceneUris(state.scenes) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(178.dp)
            .shadow(8.dp, RoundedCornerShape(28.dp), clip = false)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(palette.primary, palette.accent)))
    ) {
        if (mediaUris.isNotEmpty()) ScenicVideoPlayer(mediaUris, state.scenes.mode != "固定视频")
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = .05f), Color.Black.copy(alpha = .50f))
                )
            )
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF24484C).copy(alpha = .74f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .55f))
            ) {
                Text("第 $week 周", Modifier.padding(horizontal = 14.dp, vertical = 7.dp), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text(state.semesterName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(seasonalSentence(), color = Color.White.copy(alpha = .84f), fontSize = 14.sp)
        }
        Text("〰", Modifier.align(Alignment.BottomEnd).padding(22.dp), color = Color.White, fontSize = 31.sp)
    }
}

@Composable
private fun ScenicVideoPlayer(uris: List<String>, shuffle: Boolean) {
    val context = LocalContext.current
    val player = remember(uris, shuffle) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItems(uris.map { MediaItem.fromUri(Uri.parse(it)) })
            repeatMode = Player.REPEAT_MODE_ALL
            shuffleModeEnabled = shuffle
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                this.player = player
            }
        },
        update = { it.player = player }
    )
}

private fun sceneUris(settings: SceneSettings): List<String> = when (settings.mode) {
    "固定视频" -> listOfNotNull(settings.fixedVideoUri.takeIf(String::isNotBlank))
    "随机视频" -> settings.videoUris.ifEmpty { builtInScenicVideos.map { it.url } }
    "全部风景" -> builtInScenicVideos.map { it.url }
    else -> {
        val builtIns = when (LocalDate.now().monthValue) {
            in 3..5 -> builtInScenicVideos.filter { it.title in setOf("温柔潮汐", "雨落窗前", "海岸晨浪") }
            in 6..8 -> builtInScenicVideos.filter { it.title in setOf("海岸晨浪", "温柔潮汐", "金色海面") }
            in 9..11 -> builtInScenicVideos.filter { it.title in setOf("秋叶林间", "金色海面", "雨落窗前") }
            else -> builtInScenicVideos.filter { it.title in setOf("冬日雪林", "雨落窗前", "温柔潮汐") }
        }
        builtIns.map { it.url }
    }
}

@Composable
private fun RedesignedWeekStrip(selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val monday = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(7) { index ->
            val date = monday.plusDays(index.toLong())
            val active = date == selected
            Surface(
                modifier = Modifier.weight(1f).clickable { onSelect(date) },
                shape = RoundedCornerShape(22.dp),
                color = if (active) Color(0xFF66898C) else Color.White.copy(alpha = .76f),
                shadowElevation = if (active) 5.dp else 1.dp,
                border = if (active) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .72f))
            ) {
                Column(
                    Modifier.padding(vertical = 11.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("周${redesignedDayNames[index]}", color = if (active) Color.White else Color(0xFF73888B), fontSize = 13.sp)
                    Text(date.dayOfMonth.toString(), color = if (active) Color.White else Color(0xFF73888B), fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun CourseDayList(
    date: LocalDate,
    courses: List<Course>,
    palette: Palette,
    isToday: Boolean,
    onToday: () -> Unit,
    onEdit: (Course) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 13.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 3.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(if (isToday) "今天" else "${date.monthValue}月${date.dayOfMonth}日", color = Color(0xFF123E47), fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text("${courses.size} 节课", color = Color(0xFF789092), fontSize = 15.sp)
                }
                if (!isToday) Text("回到今天", Modifier.clickable(onClick = onToday).padding(8.dp), color = palette.primary, fontWeight = FontWeight.Bold)
            }
        }
        if (courses.isEmpty()) {
            item {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White.copy(alpha = .78f)
                ) { Text("今天没有课，放心去晒太阳吧 🌿", Modifier.padding(30.dp), color = Color(0xFF60787C)) }
            }
        } else {
            val groups = courses.groupBy { it.startPeriod to it.endPeriod }.values.toList()
            items(groups, key = { group -> group.joinToString("|") { it.id } }) { group ->
                if (group.size == 1) {
                    val course = group.first()
                    ModernCourseCard(course, palette, onEdit = { onEdit(course) }, onDelete = { onDelete(course.id) })
                } else {
                    Surface(
                        shape = RoundedCornerShape(29.dp),
                        color = palette.accent.copy(alpha = .13f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, palette.accent.copy(alpha = .22f))
                    ) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "同时段 ${group.size} 门 · 含重修课程",
                                Modifier.padding(start = 10.dp, top = 4.dp),
                                color = Color(0xFF875F68),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            group.forEach { course ->
                                ModernCourseCard(course, palette, onEdit = { onEdit(course) }, onDelete = { onDelete(course.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernCourseCard(
    course: Course,
    palette: Palette,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val accent = when (course.startPeriod) {
        1, 2 -> Color(0xFFE3A62F)
        3, 4 -> Color(0xFF8769D8)
        5, 6 -> Color(0xFF61AEB8)
        7, 8 -> Color(0xFFD98592)
        else -> courseColors[course.color.mod(courseColors.size)]
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(27.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .91f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(112.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(8.dp).height(92.dp).clip(RoundedCornerShape(20.dp)).background(accent))
            Column(Modifier.width(112.dp).padding(start = 19.dp)) {
                val time = periodTime(course.startPeriod, course.endPeriod)
                Text(time.first, color = Color(0xFF214A52), fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(time.second, color = Color(0xFF6D878B), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        course.name,
                        color = Color(0xFF173F47),
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (course.isRetake) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(50), color = palette.accent.copy(alpha = .20f)) {
                            Text("重修", Modifier.padding(horizontal = 7.dp, vertical = 2.dp), color = Color(0xFFA04F5A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("⌾ ${course.room.ifBlank { "地点待定" }}   ·   ${course.weeks}周", color = Color(0xFF71888B), fontSize = 14.sp)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Surface(shape = CircleShape, color = Color(0xFFF0F5F5)) {
                        Icon(Icons.Outlined.MoreHoriz, "更多", Modifier.padding(10.dp), tint = Color(0xFF6D8588))
                    }
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("编辑课程") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                        onClick = { menuOpen = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("删除课程") },
                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
            Spacer(Modifier.width(11.dp))
        }
    }
}

private fun periodTime(start: Int, end: Int): Pair<String, String> {
    val times = mapOf(
        1 to "08:40", 2 to "10:05", 3 to "10:25", 4 to "11:50",
        5 to "14:30", 6 to "15:55", 7 to "16:05", 8 to "17:30",
        9 to "19:30", 10 to "20:55", 11 to "21:00", 12 to "21:40"
    )
    return (times[start] ?: "第${start}节") to (times[end] ?: "第${end}节")
}

private fun greetingForRedesign(): String = when (LocalTime.now().hour) {
    in 5..10 -> "早上"
    in 11..13 -> "中午"
    in 14..18 -> "下午"
    else -> "晚上"
}

private fun seasonalSentence(): String = when (LocalDate.now().monthValue) {
    in 3..5 -> "把今天轻轻放进春风里"
    in 6..8 -> "把今天轻轻放进海风里"
    in 9..11 -> "把今天轻轻夹进秋天里"
    else -> "把今天轻轻藏进雪光里"
}
