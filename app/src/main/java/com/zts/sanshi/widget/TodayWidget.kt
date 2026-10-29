package com.zts.sanshi.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.zts.sanshi.MainActivity
import com.zts.sanshi.data.taskRepository
import com.zts.sanshi.domain.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tasks = runCatching {
            taskRepository(context).observe().first().tasks
        }.getOrDefault(emptyList())
        provideContent {
            GlanceTheme {
                WidgetContent(tasks)
            }
        }
    }
}

@Composable
private fun WidgetContent(tasks: List<Task>) {
    val ink = ColorProvider(Color(0xFF0A0A0A))
    val muted = ColorProvider(Color(0xFF6B7280))
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color.White)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "今日三事",
            style = TextStyle(color = ink, fontSize = 14.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(GlanceModifier.height(8.dp))
        if (tasks.isEmpty()) {
            Text(
                text = "还没有事",
                style = TextStyle(color = muted, fontSize = 13.sp),
            )
        } else {
            tasks.take(3).forEach { task ->
                Text(
                    text = task.text,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (task.done) muted else ink,
                        fontSize = 13.sp,
                        textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                )
                Spacer(GlanceModifier.height(4.dp))
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

object TodayWidgetUpdater {
    fun requestUpdate(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                TodayWidget().updateAll(context)
            }
        }
    }
}
