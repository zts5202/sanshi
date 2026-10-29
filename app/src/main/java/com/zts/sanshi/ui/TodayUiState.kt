package com.zts.sanshi.ui

import com.zts.sanshi.domain.BoardSnapshot
import com.zts.sanshi.domain.HistoryDay
import com.zts.sanshi.domain.MAX_TASKS
import com.zts.sanshi.domain.PoolItem
import com.zts.sanshi.domain.ReminderSettings
import com.zts.sanshi.domain.RepeatRule
import com.zts.sanshi.domain.StreakInfo
import com.zts.sanshi.domain.Task
import com.zts.sanshi.domain.WaitingSeries

data class EditorRequest(
    val taskId: String? = null,
    val poolId: String? = null,
    val initialText: String = "",
    val initialRepeat: RepeatRule = RepeatRule.None,
    val forPool: Boolean = false,
)

data class DeleteRequest(
    val id: String,
    val isSeries: Boolean,
    val fromPool: Boolean = false,
    val fromPending: Boolean = false,
)

enum class SnackKind { Rolled, Pooled, Joined }

data class TodayUiState(
    val dateLabel: String = "",
    val tasks: List<Task> = emptyList(),
    val pool: List<PoolItem> = emptyList(),
    val pendingIncomplete: List<Task> = emptyList(),
    val waiting: List<WaitingSeries> = emptyList(),
    val history: List<HistoryDay> = emptyList(),
    val reminder: ReminderSettings = ReminderSettings(),
    val streak: StreakInfo = StreakInfo(),
    val fullHintDismissed: Boolean = false,
    val celebrationGeneration: Int = 0,
    val editor: EditorRequest? = null,
    val pendingDelete: DeleteRequest? = null,
    val showDisposition: Boolean = false,
    val showDisposeEach: Boolean = false,
    val showDeleteAllConfirm: Boolean = false,
    val snack: SnackKind? = null,
    val selectedHistory: HistoryDay? = null,
) {
    val isFull: Boolean get() = tasks.size >= MAX_TASKS
    val showFullHint: Boolean get() = isFull && !fullHintDismissed
    val allDone: Boolean get() = tasks.isNotEmpty() && tasks.all { it.done }
    val poolHasItems: Boolean get() = pool.isNotEmpty()
    val tomorrowCarryRoom: Int get() = MAX_TASKS // simplified: carry starts empty each morning after settle
}

fun BoardSnapshot.toBaseState(dateLabel: String, previous: TodayUiState): TodayUiState {
    return previous.copy(
        dateLabel = dateLabel,
        tasks = tasks,
        pool = pool,
        pendingIncomplete = pendingIncomplete,
        waiting = waiting,
        history = history,
        reminder = reminder,
        streak = streak,
        showDisposition = pendingIncomplete.isNotEmpty() && !previous.showDisposeEach,
        fullHintDismissed = if (tasks.size >= MAX_TASKS) previous.fullHintDismissed else false,
        editor = previous.editor?.takeIf { req ->
            when {
                req.poolId != null -> pool.any { it.id == req.poolId }
                req.taskId != null -> tasks.any { it.id == req.taskId } || pendingIncomplete.any { it.id == req.taskId }
                else -> true
            }
        },
        pendingDelete = previous.pendingDelete?.takeIf { req ->
            tasks.any { it.id == req.id } || pool.any { it.id == req.id } || pendingIncomplete.any { it.id == req.id }
        },
    )
}
