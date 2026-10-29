package com.zts.sanshi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zts.sanshi.domain.HistoryDay
import com.zts.sanshi.domain.formatChineseDate
import com.zts.sanshi.ui.theme.Bg
import com.zts.sanshi.ui.theme.Blue
import com.zts.sanshi.ui.theme.Border
import com.zts.sanshi.ui.theme.EmptyIcon
import com.zts.sanshi.ui.theme.Ink
import com.zts.sanshi.ui.theme.InkMuted
import com.zts.sanshi.ui.theme.Surface as SurfaceTone
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun HistoryScreen(
    state: TodayUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDay: (HistoryDay) -> Unit,
    onCloseDay: () -> Unit,
) {
    val today = LocalDate.now()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDays = state.history.filter { !it.date.isBefore(weekStart) && !it.date.isAfter(today) }
    val weekDone = weekDays.sumOf { it.doneCount }
    val weekTotal = weekDays.sumOf { it.totalCount }
    val monthDays = state.history.filter { it.date.year == today.year && it.date.month == today.month }
    val monthDone = monthDays.sumOf { it.doneCount }
    val monthCompleteDays = monthDays.count { it.isCompleteDay }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(horizontal = 20.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回", tint = Ink)
            }
            Text(Copy.History, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, Copy.Settings, tint = Ink, modifier = Modifier.size(20.dp))
            }
        }
        StatCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${state.streak.current}", color = Ink, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
                Text(" 天", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
            }
            Text(Copy.StreakSubtitle, color = InkMuted, fontSize = 13.sp)
            if (state.streak.current == 0 && state.streak.previous > 0) {
                Text(Copy.StreakPrev.format(state.streak.previous), color = InkMuted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        StatCard {
            Text(Copy.WeekRate, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val pct = if (weekTotal == 0) 0f else weekDone.toFloat() / weekTotal
                Canvas(Modifier.size(48.dp)) {
                    drawArc(
                        color = Border,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = Blue,
                        startAngle = -90f,
                        sweepAngle = 360f * pct,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                Spacer(Modifier.width(12.dp))
                if (weekTotal == 0) {
                    Text(Copy.WeekEmpty, color = InkMuted, fontSize = 15.sp)
                } else {
                    Text("${(pct * 100).toInt()}% · $weekDone/$weekTotal 格", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        StatCard {
            Text(
                "本月完成 $monthDone 格 · 打卡 $monthCompleteDays 天",
                color = Ink,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(20.dp))
        if (state.history.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.History, null, tint = EmptyIcon, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text(Copy.HistoryEmptyTitle, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(Copy.HistoryEmptySubtitle, color = InkMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.history, key = { it.date.toString() }) { day ->
                    SurfaceHistoryRow(day = day, onClick = { onOpenDay(day) })
                }
            }
        }
    }
    state.selectedHistory?.let { day ->
        HistoryDaySheet(day = day, onClose = onCloseDay)
    }
}

@Composable
private fun StatCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun SurfaceHistoryRow(day: HistoryDay, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(formatChineseDate(day.date), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text("${day.doneCount}/${day.totalCount}", color = InkMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            day.items.forEach { item ->
                Box(
                    Modifier
                        .size(10.dp)
                        .background(if (item.done) Blue else Color.Transparent, RoundedCornerShape(2.dp))
                        .then(
                            if (!item.done) Modifier.background(Color.Transparent) else Modifier,
                        ),
                ) {
                    if (!item.done) {
                        Box(Modifier.fillMaxSize().background(Color.Transparent))
                        // border via draw - simplify: EmptyIcon fill
                        Box(Modifier.fillMaxSize().background(EmptyIcon.copy(alpha = 0.35f), RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDaySheet(day: HistoryDay, onClose: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheet, containerColor = Color.White) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(formatChineseDate(day.date), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            day.items.forEach { item ->
                Text(
                    text = item.text,
                    color = if (item.done) InkMuted else Ink,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text(Copy.Close, color = Ink, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

