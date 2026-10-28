package com.zts.sanshi.domain

/**
 * One thing to do today. At most [MAX_TASKS] exist for the current date.
 */
data class Task(
    val id: String,
    val text: String,
    val done: Boolean,
)

const val MAX_TASKS = 3

enum class AddOutcome {
    Added,
    RejectedEmpty,
    RejectedFull,
}

data class TodaySnapshot(
    val date: java.time.LocalDate,
    val tasks: List<Task>,
)
