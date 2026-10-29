package com.zts.sanshi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zts.sanshi.domain.RepeatRule
import com.zts.sanshi.domain.Task
import com.zts.sanshi.ui.theme.Bg
import com.zts.sanshi.ui.theme.Blue
import com.zts.sanshi.ui.theme.BluePressed
import com.zts.sanshi.ui.theme.Border
import com.zts.sanshi.ui.theme.Danger
import com.zts.sanshi.ui.theme.DisabledFill
import com.zts.sanshi.ui.theme.EmptyIcon
import com.zts.sanshi.ui.theme.Ink
import com.zts.sanshi.ui.theme.InkDisabled
import com.zts.sanshi.ui.theme.InkMuted
import com.zts.sanshi.ui.theme.LocalReduceMotion
import com.zts.sanshi.ui.theme.Motion
import com.zts.sanshi.ui.theme.SuccessSoft
import com.zts.sanshi.ui.theme.Surface as SurfaceTone
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun SanshiScreen(
    state: TodayUiState,
    reduceMotion: Boolean,
    onAddClick: () -> Unit,
    onTaskClick: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onDeleteClick: (String) -> Unit,
    onConfirmDelete: (Boolean) -> Unit,
    onDismissDelete: () -> Unit,
    onDismissFullHint: () -> Unit,
    onDismissEditor: () -> Unit,
    onSaveEditor: (String, RepeatRule) -> Unit,
    onOpenPool: () -> Unit,
    onOpenHistory: () -> Unit,
    onRollTomorrow: (String) -> Unit,
    onMoveToPool: (String) -> Unit,
    onAdmitWaiting: () -> Unit,
    onDisposeAllRoll: () -> Unit,
    onDisposeEach: () -> Unit,
    onDisposeAllDeleteAsk: () -> Unit,
    onDisposeAllDeleteConfirm: () -> Unit,
    onDismissDeleteAllConfirm: () -> Unit,
    onClearSnack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackHost = remember { SnackbarHostState() }
    LaunchedEffect(state.snack) {
        val kind = state.snack ?: return@LaunchedEffect
        val msg = when (kind) {
            SnackKind.Rolled -> Copy.SnackRolled
            SnackKind.Pooled -> Copy.SnackPooled
            SnackKind.Joined -> Copy.SnackJoined
        }
        snackHost.showSnackbar(msg)
        onClearSnack()
    }
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Bg),
            contentAlignment = Alignment.TopCenter,
        ) {
            Scaffold(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize(),
                containerColor = Bg,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = {
                    SnackbarHost(snackHost) { data ->
                        Snackbar(snackbarData = data, containerColor = Ink, contentColor = Color.White)
                    }
                },
                bottomBar = {
                    AddBar(
                        full = state.isFull,
                        showHint = state.showFullHint,
                        waiting = state.waiting,
                        reduceMotion = reduceMotion,
                        onAddClick = onAddClick,
                        onDismissHint = onDismissFullHint,
                        onAdmitWaiting = onAdmitWaiting,
                    )
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .padding(top = 12.dp),
                ) {
                    Header(
                        dateLabel = state.dateLabel,
                        poolBadge = state.poolHasItems,
                        onOpenPool = onOpenPool,
                        onOpenHistory = onOpenHistory,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(top = 8.dp, bottom = 20.dp),
                    )
                    ListRegion(
                        state = state,
                        reduceMotion = reduceMotion,
                        onTaskClick = onTaskClick,
                        onToggle = onToggle,
                        onDeleteClick = onDeleteClick,
                        onRollTomorrow = onRollTomorrow,
                        onMoveToPool = onMoveToPool,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    )
                }
            }
        }
        if (state.showDisposition) {
            DispositionSheet(
                pending = state.pendingIncomplete,
                onRollAll = onDisposeAllRoll,
                onDisposeEach = onDisposeEach,
                onDeleteAll = onDisposeAllDeleteAsk,
            )
        }
        if (state.showDeleteAllConfirm) {
            AlertDialog(
                onDismissRequest = onDismissDeleteAllConfirm,
                containerColor = Color.White,
                title = {
                    Text(Copy.DeleteAllConfirm, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                },
                confirmButton = {
                    TextButton(onClick = onDisposeAllDeleteConfirm) {
                        Text(Copy.Delete, color = Danger, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDeleteAllConfirm) {
                        Text(Copy.Cancel, color = Ink, fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        }
        state.pendingDelete?.let { req ->
            if (req.isSeries && !req.fromPool) {
                SeriesDeleteDialog(
                    onOnce = { onConfirmDelete(false) },
                    onStop = { onConfirmDelete(true) },
                    onDismiss = onDismissDelete,
                )
            } else {
                DeleteDialog(onConfirm = { onConfirmDelete(false) }, onDismiss = onDismissDelete)
            }
        }
        state.editor?.takeIf { !it.forPool }?.let { request ->
            EditorSheet(request = request, onDismiss = onDismissEditor, onSave = onSaveEditor)
        }
    }
}

@Composable
private fun Header(
    dateLabel: String,
    poolBadge: Boolean,
    onOpenPool: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                text = Copy.AppName,
                modifier = Modifier.semantics { heading() },
                color = Ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 36.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateLabel,
                color = InkMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
            )
        }
        Box {
            IconButton(onClick = onOpenPool, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Outlined.Inbox, contentDescription = Copy.Inbox, tint = Ink, modifier = Modifier.size(20.dp))
            }
            if (poolBadge) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(8.dp)
                        .background(Blue, CircleShape),
                )
            }
        }
        IconButton(onClick = onOpenHistory, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Outlined.History, contentDescription = Copy.History, tint = Ink, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ListRegion(
    state: TodayUiState,
    reduceMotion: Boolean,
    onTaskClick: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onDeleteClick: (String) -> Unit,
    onRollTomorrow: (String) -> Unit,
    onMoveToPool: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var retained by remember { mutableStateOf(state.tasks) }
    if (state.tasks.isNotEmpty() && retained != state.tasks) {
        retained = state.tasks
    }
    val shownTasks = if (state.tasks.isEmpty()) retained else state.tasks
    Column(modifier) {
        AnimatedVisibility(
            visible = state.allDone,
            enter = EnterTransition.None,
            exit = fadeOut(tween(Motion.ms(180, reduceMotion), easing = Motion.ExitEasing)) +
                shrinkVertically(tween(Motion.ms(200, reduceMotion), easing = Motion.ExitEasing)),
        ) {
            Column {
                CelebrationBar(generation = state.celebrationGeneration, reduceMotion = reduceMotion)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        Crossfade(
            targetState = state.tasks.isEmpty(),
            animationSpec = tween(Motion.ms(200, reduceMotion), easing = Motion.EnterEasing),
            label = "emptyCrossfade",
        ) { empty ->
            if (empty) {
                EmptyState(reduceMotion = reduceMotion)
            } else {
                TaskStack(
                    tasks = shownTasks,
                    celebrationGeneration = state.celebrationGeneration,
                    reduceMotion = reduceMotion,
                    onTaskClick = onTaskClick,
                    onToggle = onToggle,
                    onDeleteClick = onDeleteClick,
                    onRollTomorrow = onRollTomorrow,
                    onMoveToPool = onMoveToPool,
                )
            }
        }
    }
}

private class TaskSlot(
    initial: Task,
    val visible: MutableTransitionState<Boolean>,
    val enterIndex: Int,
) {
    var task by mutableStateOf(initial)
}

@Composable
private fun TaskStack(
    tasks: List<Task>,
    celebrationGeneration: Int,
    reduceMotion: Boolean,
    onTaskClick: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onDeleteClick: (String) -> Unit,
    onRollTomorrow: (String) -> Unit,
    onMoveToPool: (String) -> Unit,
) {
    val slots = remember { mutableStateListOf<TaskSlot>() }
    val enterOffset = with(LocalDensity.current) { 12.dp.roundToPx() }
    LaunchedEffect(tasks) {
        val incoming = tasks.associateBy { it.id }
        tasks.forEachIndexed { index, task ->
            val existingIndex = slots.indexOfFirst { it.task.id == task.id }
            if (existingIndex == -1) {
                val visible = MutableTransitionState(false)
                visible.targetState = true
                val slot = TaskSlot(task, visible, index)
                val insertAt = slots.indexOfFirst { existing ->
                    val position = tasks.indexOfFirst { it.id == existing.task.id }
                    position == -1 || position > index
                }
                if (insertAt == -1) slots.add(slot) else slots.add(insertAt, slot)
            } else {
                slots[existingIndex].task = task
                slots[existingIndex].visible.targetState = true
            }
        }
        slots.filter { it.task.id !in incoming.keys }.forEach { slot ->
            slot.visible.targetState = false
        }
    }
    val visibilityKey = slots.joinToString("|") {
        "${it.task.id}:${it.visible.currentState}:${it.visible.targetState}:${it.visible.isIdle}"
    }
    LaunchedEffect(visibilityKey) {
        val finished = slots.filter { !it.visible.currentState && !it.visible.targetState && it.visible.isIdle }
        if (finished.isNotEmpty()) slots.removeAll(finished)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        slots.forEach { slot ->
            val task = slot.task
            key(task.id) {
                AnimatedVisibility(
                    visibleState = slot.visible,
                    enter = fadeIn(
                        tween(
                            durationMillis = Motion.ms(220, reduceMotion),
                            delayMillis = Motion.stagger(slot.enterIndex, reduceMotion),
                            easing = Motion.EnterEasing,
                        ),
                    ) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = Motion.ms(220, reduceMotion),
                            delayMillis = Motion.stagger(slot.enterIndex, reduceMotion),
                            easing = Motion.EnterEasing,
                        ),
                    ) { enterOffset },
                    exit = fadeOut(tween(Motion.ms(180, reduceMotion), easing = Motion.ExitEasing)) +
                        scaleOut(
                            targetScale = 0.9f,
                            animationSpec = tween(Motion.ms(180, reduceMotion), easing = Motion.ExitEasing),
                        ) +
                        shrinkVertically(
                            animationSpec = tween(Motion.ms(200, reduceMotion), easing = Motion.ExitEasing),
                            shrinkTowards = Alignment.Top,
                        ),
                ) {
                    TaskCard(
                        task = task,
                        enterIndex = slot.enterIndex,
                        celebrationGeneration = celebrationGeneration,
                        reduceMotion = reduceMotion,
                        onToggle = { done -> onToggle(task.id, done) },
                        onTextClick = { onTaskClick(task.id) },
                        onRollTomorrow = { onRollTomorrow(task.id) },
                        onMoveToPool = { onMoveToPool(task.id) },
                        onDeleteClick = { onDeleteClick(task.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(reduceMotion: Boolean) {
    val alpha = if (reduceMotion) 1f else BreathingAlpha()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.List,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer { this.alpha = alpha },
            tint = EmptyIcon,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(Copy.EmptyTitle, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(Copy.EmptySubtitle, color = InkMuted, fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp)
    }
}

@Composable
private fun BreathingAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "emptyBreath")
    val alpha by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "emptyAlpha",
    )
    return alpha
}

@Composable
private fun CelebrationBar(generation: Int, reduceMotion: Boolean) {
    val scale = remember(generation) { Animatable(if (generation == 0 || reduceMotion) 1f else 0.96f) }
    val alpha = remember(generation) { Animatable(if (generation == 0 || reduceMotion) 1f else 0f) }
    LaunchedEffect(generation, reduceMotion) {
        if (generation == 0 || reduceMotion) {
            scale.snapTo(1f); alpha.snapTo(1f); return@LaunchedEffect
        }
        scale.snapTo(0.96f); alpha.snapTo(0f)
        coroutineScope {
            launch { alpha.animateTo(1f, tween(Motion.ms(420, false), easing = Motion.EnterEasing)) }
            launch {
                scale.animateTo(1.02f, tween(Motion.ms(210, false), easing = Motion.EnterEasing))
                scale.animateTo(1f, tween(Motion.ms(210, false), easing = Motion.ExitEasing))
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
            .background(SuccessSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Check, null, tint = Blue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(Copy.Celebration, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
    }
}

@Composable
private fun AddBar(
    full: Boolean,
    showHint: Boolean,
    waiting: List<com.zts.sanshi.domain.WaitingSeries>,
    reduceMotion: Boolean,
    onAddClick: () -> Unit,
    onDismissHint: () -> Unit,
    onAdmitWaiting: () -> Unit,
) {
    val density = LocalDensity.current
    val slidePx = with(density) { 8.dp.roundToPx() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
    ) {
        AnimatedVisibility(
            visible = showHint,
            enter = fadeIn(tween(Motion.ms(220, reduceMotion), easing = Motion.EnterEasing)) +
                slideInVertically(tween(Motion.ms(220, reduceMotion), easing = Motion.EnterEasing)) { slidePx },
            exit = fadeOut(tween(Motion.ms(180, reduceMotion), easing = Motion.ExitEasing)) +
                slideOutVertically(tween(Motion.ms(180, reduceMotion), easing = Motion.ExitEasing)) { slidePx },
        ) {
            Column {
                FullHint(onDismiss = onDismissHint)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        if (!showHint && waiting.isNotEmpty()) {
            WaitingHint(
                count = waiting.size,
                full = full,
                onAdmit = onAdmitWaiting,
            )
            Spacer(modifier = Modifier.height(8.dp))
        } else if (showHint && waiting.isNotEmpty()) {
            // merge as subtitle is already in FullHint; optional extra line skipped to keep full priority
        }
        AddButton(enabled = !full, onClick = onAddClick)
    }
}

@Composable
private fun WaitingHint(count: Int, full: Boolean, onAdmit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceTone, RoundedCornerShape(12.dp))
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Repeat, null, tint = InkMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (count <= 1) Copy.WaitingHint else Copy.WaitingHintN.format(count),
            color = InkMuted,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onAdmit, enabled = !full) {
            Text(
                Copy.Join,
                color = if (full) InkDisabled else Blue,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun FullHint(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceTone, RoundedCornerShape(12.dp))
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = Copy.FullHint,
            modifier = Modifier.weight(1f),
            color = InkMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 18.sp,
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Outlined.Close, Copy.DismissHint, tint = InkMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AddButton(enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val target = when {
        !enabled -> DisabledFill
        pressed -> BluePressed
        else -> Blue
    }
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 80, easing = Motion.ExitEasing),
        label = "addButton",
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = RoundedCornerShape(14.dp),
        color = color,
        contentColor = if (enabled) Color.White else InkDisabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics(mergeDescendants = true) {}
            .pressScale(interaction, enabled = enabled),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(Copy.Add, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Ink,
        shape = RoundedCornerShape(16.dp),
        title = { Text(Copy.DeleteTitle, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Ink) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(Copy.Delete, color = Danger, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Copy.Cancel, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

@Composable
private fun SeriesDeleteDialog(onOnce: () -> Unit, onStop: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(Copy.DeleteSeriesTitle, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium) },
        confirmButton = {
            TextButton(onClick = onOnce) {
                Text(Copy.DeleteOnce, color = Blue, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onStop) {
                    Text(Copy.StopSeries, color = Ink, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onDismiss) {
                    Text(Copy.Cancel, color = InkMuted, fontWeight = FontWeight.SemiBold)
                }
            }
        },
    )
}
