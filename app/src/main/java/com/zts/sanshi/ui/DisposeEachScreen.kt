package com.zts.sanshi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zts.sanshi.domain.Task
import com.zts.sanshi.ui.theme.Bg
import com.zts.sanshi.ui.theme.Blue
import com.zts.sanshi.ui.theme.Border
import com.zts.sanshi.ui.theme.Danger
import com.zts.sanshi.ui.theme.Ink
import com.zts.sanshi.ui.theme.InkMuted

@Composable
fun DisposeEachScreen(
    pending: List<Task>,
    onBack: () -> Unit,
    onRoll: (String) -> Unit,
    onPool: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回", tint = Ink)
            }
            Text(Copy.DisposeScreenTitle, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(pending, key = { it.id }) { task ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(task.text, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.padding(top = 8.dp))
                        Row {
                            TextButton(onClick = { onRoll(task.id) }) {
                                Text(Copy.RollTomorrow, color = Blue, fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(onClick = { onPool(task.id) }) {
                                Text("回池", color = Ink, fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(onClick = { onDelete(task.id) }) {
                                Text(Copy.Delete, color = Danger, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
