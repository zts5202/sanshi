package com.zts.sanshi.ui

import com.zts.sanshi.domain.MAX_TASKS
import com.zts.sanshi.domain.Task

data class EditorRequest(
    val taskId: String?,
    val initialText: String,
)

data class TodayUiState(
    val dateLabel: String = "",
    val tasks: List<Task> = emptyList(),
    val fullHintDismissed: Boolean = false,
    val celebrationGeneration: Int = 0,
    val editor: EditorRequest? = null,
    val pendingDeleteId: String? = null,
) {
    val isFull: Boolean get() = tasks.size >= MAX_TASKS
    val showFullHint: Boolean get() = isFull && !fullHintDismissed
    val allDone: Boolean get() = tasks.isNotEmpty() && tasks.all { it.done }
}
