@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.shishi.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shishi.schedule.data.Course
import com.shishi.schedule.data.UserProfile
import java.util.UUID

val courseColors = listOf(
    Color(0xFFD9D4EA), Color(0xFFC9E3E2), Color(0xFFBFDDE5), Color(0xFFE9D8AE),
    Color(0xFFD8DFE5), Color(0xFFE8C8CE), Color(0xFFE1CBE6), Color(0xFFD6E2C8)
)

@Composable
fun PageTitle(title: String, subtitle: String = "") {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        if (subtitle.isNotBlank()) Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f))
    }
}

@Composable
fun CourseCard(course: Course, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(18.dp),
        color = courseColors[course.color.mod(courseColors.size)].copy(alpha = .94f)
    ) {
        Row(Modifier.padding(start = 16.dp, top = 13.dp, bottom = 13.dp, end = 6.dp)) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(course.name, fontWeight = FontWeight.Bold)
                    if (course.isRetake) {
                        Text("重修", color = Color(0xFFA04F5A), style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text(
                    "第${course.startPeriod}-${course.endPeriod}节  ${course.room.ifBlank { "地点待定" }}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("${course.weeks}周${course.teacher.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "删除") }
        }
    }
}

@Composable
fun ProfileDialog(
    initial: UserProfile,
    dismissible: Boolean,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var gender by remember(initial) { mutableStateOf(initial.gender) }
    var theme by remember(initial) { mutableStateOf(initial.theme) }
    AlertDialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        title = { Text(if (initial.name.isBlank()) "欢迎使用诗诗课表" else "个人设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("先告诉我怎么称呼你，资料只存在这台手机。")
                OutlinedTextField(name, { name = it }, label = { Text("姓名或昵称") }, singleLine = true)
                Text("性别")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("女", "男", "其他").forEach {
                        FilterChip(selected = gender == it, onClick = { gender = it }, label = { Text(it) })
                    }
                }
                Text("莫兰迪底色")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palettes.forEach { p ->
                        FilterChip(
                            selected = theme == p.key,
                            onClick = { theme = p.key },
                            label = {
                                Row {
                                    Spacer(Modifier.width(14.dp).height(14.dp).background(p.primary, RoundedCornerShape(20.dp)))
                                    Spacer(Modifier.width(6.dp))
                                    Text(p.key)
                                }
                            }
                        )
                    }
                }
                if (gender == "男") Text("男生模式不会显示经期记录入口。", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                enabled = name.trim().isNotEmpty(),
                onClick = { onSave(UserProfile(name.trim(), gender, theme)); onDismiss() }
            ) { Text("保存") }
        },
        dismissButton = if (dismissible) ({ TextButton(onClick = onDismiss) { Text("取消") } }) else null
    )
}

@Composable
fun CourseEditor(course: Course?, onDismiss: () -> Unit, onSave: (Course) -> Unit) {
    var name by remember(course?.id) { mutableStateOf(course?.name.orEmpty()) }
    var room by remember(course?.id) { mutableStateOf(course?.room.orEmpty()) }
    var teacher by remember(course?.id) { mutableStateOf(course?.teacher.orEmpty()) }
    var weeks by remember(course?.id) { mutableStateOf(course?.weeks ?: "1-20") }
    var day by remember(course?.id) { mutableIntStateOf(course?.day ?: 1) }
    var start by remember(course?.id) { mutableStateOf((course?.startPeriod ?: 1).toString()) }
    var end by remember(course?.id) { mutableStateOf((course?.endPeriod ?: 2).toString()) }
    var retake by remember(course?.id) { mutableStateOf(course?.isRetake ?: false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (course == null) "添加课程" else "编辑课程") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("课程名") }, singleLine = true)
                OutlinedTextField(room, { room = it }, label = { Text("教室") }, singleLine = true)
                OutlinedTextField(teacher, { teacher = it }, label = { Text("老师（可不填）") }, singleLine = true)
                OutlinedTextField(weeks, { weeks = it }, label = { Text("周次，例如 1-16 或 1,3,5") }, singleLine = true)
                Text("星期")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { i, label ->
                        FilterChip(selected = day == i + 1, onClick = { day = i + 1 }, label = { Text(label) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(start, { start = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("开始节") }, singleLine = true)
                    OutlinedTextField(end, { end = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("结束节") }, singleLine = true)
                }
                FilterChip(selected = retake, onClick = { retake = !retake }, label = { Text("标记为重修") })
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && ((start.toIntOrNull() ?: 0) in 1..12) && ((end.toIntOrNull() ?: 0) in 1..12),
                onClick = {
                    onSave(
                        Course(
                            id = course?.id ?: UUID.randomUUID().toString(), name = name.trim(), day = day,
                            startPeriod = start.toInt(), endPeriod = end.toInt(), room = room.trim(),
                            teacher = teacher.trim(), weeks = weeks.trim(), color = course?.color ?: day,
                            isRetake = retake
                        )
                    )
                    onDismiss()
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
