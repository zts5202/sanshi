package com.zts.sanshi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zts.sanshi.domain.RepeatRule
import com.zts.sanshi.ui.theme.Bg
import com.zts.sanshi.ui.theme.Blue
import com.zts.sanshi.ui.theme.Border
import com.zts.sanshi.ui.theme.Danger
import com.zts.sanshi.ui.theme.DisabledFill
import com.zts.sanshi.ui.theme.EmptyIcon
import com.zts.sanshi.ui.theme.Ink
import com.zts.sanshi.ui.theme.InkDisabled
import com.zts.sanshi.ui.theme.InkMuted
import com.zts.sanshi.ui.theme.Surface as SurfaceTone

@Composable
fun PoolScreen(
    state: TodayUiState,
    reduceMotion: Boolean,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onPromote: (String) -> Unit,
    onDelete: (String) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onDismissEditor: () -> Unit,
    onSaveEditor: (String, RepeatRule) -> Unit,
    onClearSnack: () -> Unit,
) {
    val snackHost = remember { SnackbarHostState() }
    LaunchedEffect(state.snack) {
        if (state.snack == SnackKind.Joined) {
            snackHost.showSnackbar(Copy.SnackJoined)
            onClearSnack()
        }
    }
    Scaffold(
        containerColor = Bg,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(snackHost) { Snackbar(it, containerColor = Ink, contentColor = Color.White) }
        },
        bottomBar = {
            Column(
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                if (state.isFull) {
                    Text(
                        Copy.PoolFullHint,
                        color = InkMuted,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceTone, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Surface(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(14.dp),
                    color = Blue,
                    contentColor = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Row(
                        Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(Copy.AddInspiration, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .fillMaxSize(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回", tint = Ink)
                }
                Column {
                    Text(Copy.Inbox, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                    Text(Copy.InboxSubtitle, color = InkMuted, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            if (state.pool.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Outlined.Inbox, null, tint = EmptyIcon, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(Copy.InboxEmptyTitle, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(Copy.InboxEmptySubtitle, color = InkMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.pool, key = { it.id }) { item ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Border),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .background(EmptyIcon, CircleShape),
                                )
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp)
                                        .clickable { onItemClick(item.id) },
                                ) {
                                    Text(item.text, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    val label = item.repeat.label()
                                    if (label.isNotEmpty()) {
                                        Text(label, color = InkMuted, fontSize = 11.sp)
                                    }
                                }
                                TextButton(onClick = { onPromote(item.id) }, enabled = !state.isFull) {
                                    Text(
                                        Copy.JoinToday,
                                        color = if (state.isFull) InkDisabled else Blue,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                IconButton(onClick = { onDelete(item.id) }) {
                                    Icon(Icons.Outlined.Delete, Copy.Delete, tint = InkMuted, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    state.pendingDelete?.takeIf { it.fromPool }?.let {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            containerColor = Color.White,
            title = { Text(Copy.DeleteTitle, color = Ink) },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text(Copy.Delete, color = Danger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text(Copy.Cancel, color = Ink, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
    state.editor?.takeIf { it.forPool }?.let { request ->
        EditorSheet(request = request, onDismiss = onDismissEditor, onSave = onSaveEditor)
    }
}
