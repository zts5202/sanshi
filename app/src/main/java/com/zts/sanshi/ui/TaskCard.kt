package com.zts.sanshi.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zts.sanshi.domain.Task
import com.zts.sanshi.ui.theme.Blue
import com.zts.sanshi.ui.theme.Border
import com.zts.sanshi.ui.theme.Danger
import com.zts.sanshi.ui.theme.Ink
import com.zts.sanshi.ui.theme.InkMuted
import com.zts.sanshi.ui.theme.Motion
import com.zts.sanshi.ui.theme.Shadow
import kotlinx.coroutines.delay

private val CardShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: Task,
    enterIndex: Int,
    celebrationGeneration: Int,
    reduceMotion: Boolean,
    onToggle: (Boolean) -> Unit,
    onTextClick: () -> Unit,
    onRollTomorrow: () -> Unit,
    onMoveToPool: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textInteraction = remember { MutableInteractionSource() }
    val menuInteraction = remember { MutableInteractionSource() }
    var menuOpen by remember { mutableStateOf(false) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(celebrationGeneration) {
        if (celebrationGeneration == 0 || reduceMotion) {
            pop.snapTo(1f)
            return@LaunchedEffect
        }
        delay(enterIndex * 30L)
        pop.animateTo(1.02f, tween(Motion.ms(160, false), easing = Motion.EnterEasing))
        pop.animateTo(1f, tween(Motion.ms(160, false), easing = Motion.ExitEasing))
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .pressScale(textInteraction)
            .shadow(elevation = 2.dp, shape = CardShape, ambientColor = Shadow, spotColor = Shadow)
            .combinedClickable(
                interactionSource = textInteraction,
                indication = null,
                onClick = {},
                onLongClick = { menuOpen = true },
            ),
        shape = CardShape,
        color = Color.White,
        border = BorderStroke(1.dp, Border),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SanshiCheckbox(
                checked = task.done,
                onCheckedChange = onToggle,
                reduceMotion = reduceMotion,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .clickable(
                        interactionSource = textInteraction,
                        indication = ripple(color = Blue.copy(alpha = 0.16f)),
                        role = Role.Button,
                        onClick = onTextClick,
                    ),
            ) {
                TaskLabel(text = task.text, done = task.done, reduceMotion = reduceMotion)
                val label = task.repeat.label()
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        color = InkMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier
                        .pressScale(menuInteraction)
                        .size(48.dp),
                    interactionSource = menuInteraction,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "更多",
                        tint = InkMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (!task.done) {
                        DropdownMenuItem(
                            text = { Text(Copy.RollTomorrow, color = Ink) },
                            onClick = { menuOpen = false; onRollTomorrow() },
                        )
                        DropdownMenuItem(
                            text = { Text(Copy.BackToPool, color = Ink) },
                            onClick = { menuOpen = false; onMoveToPool() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(Copy.Delete, color = Danger) },
                        onClick = { menuOpen = false; onDeleteClick() },
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskLabel(
    text: String,
    done: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val strike by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = tween(Motion.ms(160, reduceMotion), easing = Motion.EnterEasing),
        label = "strike",
    )
    val color by animateColorAsState(
        targetValue = if (done) InkMuted else Ink,
        animationSpec = tween(Motion.ms(160, reduceMotion), easing = Motion.EnterEasing),
        label = "taskColor",
    )
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        modifier = modifier.drawStrike(layout, strike),
        color = color,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 22.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layout = it },
    )
}

private fun Modifier.drawStrike(layout: TextLayoutResult?, strike: Float): Modifier = drawWithContent {
    drawContent()
    val result = layout ?: return@drawWithContent
    if (strike <= 0f) return@drawWithContent
    val stroke = 1.5.dp.toPx()
    for (line in 0 until result.lineCount) {
        val left = result.getLineLeft(line)
        val right = result.getLineRight(line)
        val y = (result.getLineTop(line) + result.getLineBottom(line)) / 2f
        drawLine(
            color = InkMuted,
            start = Offset(left, y),
            end = Offset(left + (right - left) * strike.coerceIn(0f, 1f), y),
            strokeWidth = stroke,
        )
    }
}

@Composable
internal fun SanshiCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    reduceMotion: Boolean,
) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(Motion.ms(200, reduceMotion), easing = Motion.EnterEasing),
        label = "checkDraw",
    )
    Box(
        modifier = Modifier
            .size(44.dp)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val radius = 8.dp.toPx()
            val stroke = 1.5.dp.toPx()
            if (progress > 0f) {
                drawRoundRect(
                    color = Blue.copy(alpha = progress),
                    cornerRadius = CornerRadius(radius, radius),
                )
            }
            if (progress < 1f) {
                val inset = stroke / 2f
                drawRoundRect(
                    color = Border,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = stroke),
                )
            }
            if (progress > 0f) {
                val path = Path().apply {
                    moveTo(size.width * 0.22f, size.height * 0.52f)
                    lineTo(size.width * 0.42f, size.height * 0.72f)
                    lineTo(size.width * 0.78f, size.height * 0.32f)
                }
                val measure = PathMeasure()
                measure.setPath(path, false)
                val segment = Path()
                measure.getSegment(0f, measure.length * progress, segment, true)
                drawPath(
                    path = segment,
                    color = Color.White,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}
