package com.zts.sanshi.domain

import java.time.DayOfWeek
import java.time.LocalDate

const val MAX_TASKS = 3

enum class AddOutcome {
    Added,
    RejectedEmpty,
    RejectedFull,
}

enum class RepeatKind {
    None,
    Daily,
    Weekdays,
    Weekly,
}

/**
 * Recurrence rule. [weekDays] uses ISO day numbers 1=Mon … 7=Sun; only meaningful for Weekly.
 */
data class RepeatRule(
    val kind: RepeatKind = RepeatKind.None,
    val weekDays: Set<Int> = emptySet(),
) {
    val isRepeating: Boolean get() = kind != RepeatKind.None

    fun matches(date: LocalDate): Boolean = when (kind) {
        RepeatKind.None -> false
        RepeatKind.Daily -> true
        RepeatKind.Weekdays -> date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY
        RepeatKind.Weekly -> weekDays.contains(date.dayOfWeek.value)
    }

    fun label(): String = when (kind) {
        RepeatKind.None -> ""
        RepeatKind.Daily -> "每天"
        RepeatKind.Weekdays -> "工作日"
        RepeatKind.Weekly -> {
            val map = mapOf(1 to "一", 2 to "二", 3 to "三", 4 to "四", 5 to "五", 6 to "六", 7 to "日")
            val body = weekDays.sorted().joinToString("") { map[it] ?: "" }
            if (body.isEmpty()) "每周" else "每周·$body"
        }
    }

    companion object {
        val None = RepeatRule()
    }
}

data class Task(
    val id: String,
    val text: String,
    val done: Boolean,
    val repeat: RepeatRule = RepeatRule.None,
    /** Stable id of the recurring series; null for one-off tasks. */
    val seriesId: String? = null,
)

data class PoolItem(
    val id: String,
    val text: String,
    val repeat: RepeatRule = RepeatRule.None,
)

data class HistoryItem(
    val text: String,
    val done: Boolean,
)

data class HistoryDay(
    val date: LocalDate,
    val items: List<HistoryItem>,
) {
    val doneCount: Int get() = items.count { it.done }
    val totalCount: Int get() = items.size
    val isCompleteDay: Boolean get() = items.isNotEmpty() && items.all { it.done }
}

/** A recurring series waiting for an empty today slot. */
data class WaitingSeries(
    val seriesId: String,
    val text: String,
    val repeat: RepeatRule,
)

data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 9,
    val minute: Int = 0,
)

data class StreakInfo(
    val current: Int = 0,
    val previous: Int = 0,
)

data class BoardSnapshot(
    val date: LocalDate,
    val tasks: List<Task>,
    val pool: List<PoolItem>,
    val pendingIncomplete: List<Task>,
    val waiting: List<WaitingSeries>,
    val history: List<HistoryDay>,
    val reminder: ReminderSettings,
    val streak: StreakInfo,
) {
    val isFull: Boolean get() = tasks.size >= MAX_TASKS
    val needsDisposition: Boolean get() = pendingIncomplete.isNotEmpty()
}
