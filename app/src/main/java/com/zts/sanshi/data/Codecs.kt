package com.zts.sanshi.data

import com.zts.sanshi.domain.HistoryDay
import com.zts.sanshi.domain.HistoryItem
import com.zts.sanshi.domain.MAX_TASKS
import com.zts.sanshi.domain.PoolItem
import com.zts.sanshi.domain.RepeatKind
import com.zts.sanshi.domain.RepeatRule
import com.zts.sanshi.domain.Task
import com.zts.sanshi.domain.WaitingSeries
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
internal data class StoredRepeat(
    val kind: String = "none",
    val weekDays: List<Int> = emptyList(),
) {
    fun toDomain(): RepeatRule {
        val k = when (kind.lowercase()) {
            "daily" -> RepeatKind.Daily
            "weekdays" -> RepeatKind.Weekdays
            "weekly" -> RepeatKind.Weekly
            else -> RepeatKind.None
        }
        return RepeatRule(kind = k, weekDays = weekDays.filter { it in 1..7 }.toSet())
    }

    companion object {
        fun from(rule: RepeatRule) = StoredRepeat(
            kind = when (rule.kind) {
                RepeatKind.None -> "none"
                RepeatKind.Daily -> "daily"
                RepeatKind.Weekdays -> "weekdays"
                RepeatKind.Weekly -> "weekly"
            },
            weekDays = rule.weekDays.sorted(),
        )
    }
}

@Serializable
internal data class StoredTask(
    val id: String,
    val text: String,
    val done: Boolean,
    val repeat: StoredRepeat = StoredRepeat(),
    val seriesId: String? = null,
)

@Serializable
internal data class StoredPoolItem(
    val id: String,
    val text: String,
    val repeat: StoredRepeat = StoredRepeat(),
)

@Serializable
internal data class StoredHistoryItem(
    val text: String,
    val done: Boolean,
)

@Serializable
internal data class StoredHistoryDay(
    val date: String,
    val items: List<StoredHistoryItem>,
)

@Serializable
internal data class StoredWaiting(
    val seriesId: String,
    val text: String,
    val repeat: StoredRepeat,
)

@Serializable
internal data class StoredSeries(
    val seriesId: String,
    val text: String,
    val repeat: StoredRepeat,
    val stopped: Boolean = false,
)

internal object BoardCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encodeTasks(tasks: List<Task>): String {
        val stored = tasks.take(MAX_TASKS).map {
            StoredTask(it.id, it.text, it.done, StoredRepeat.from(it.repeat), it.seriesId)
        }
        return json.encodeToString(stored)
    }

    fun decodeTasks(raw: String?): List<Task> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<StoredTask>>(raw)
                .mapNotNull { s ->
                    if (s.id.isBlank() || s.text.isBlank()) null
                    else Task(s.id, s.text, s.done, s.repeat.toDomain(), s.seriesId)
                }
                .take(MAX_TASKS)
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    fun encodePool(items: List<PoolItem>): String =
        json.encodeToString(items.map { StoredPoolItem(it.id, it.text, StoredRepeat.from(it.repeat)) })

    fun decodePool(raw: String?): List<PoolItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<StoredPoolItem>>(raw).mapNotNull { s ->
                if (s.id.isBlank() || s.text.isBlank()) null
                else PoolItem(s.id, s.text, s.repeat.toDomain())
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeHistory(days: List<HistoryDay>): String =
        json.encodeToString(
            days.map { d ->
                StoredHistoryDay(
                    date = d.date.toString(),
                    items = d.items.map { StoredHistoryItem(it.text, it.done) },
                )
            },
        )

    fun decodeHistory(raw: String?): List<HistoryDay> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<StoredHistoryDay>>(raw).mapNotNull { d ->
                val date = runCatching { LocalDate.parse(d.date) }.getOrNull() ?: return@mapNotNull null
                HistoryDay(date, d.items.map { HistoryItem(it.text, it.done) })
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeWaiting(items: List<WaitingSeries>): String =
        json.encodeToString(
            items.map { StoredWaiting(it.seriesId, it.text, StoredRepeat.from(it.repeat)) },
        )

    fun decodeWaiting(raw: String?): List<WaitingSeries> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<StoredWaiting>>(raw).map {
                WaitingSeries(it.seriesId, it.text, it.repeat.toDomain())
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeSeries(items: List<StoredSeries>): String = json.encodeToString(items)

    fun decodeSeries(raw: String?): List<StoredSeries> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }
}

/** Back-compat alias used by older unit tests. */
internal object TaskCodec {
    fun encode(tasks: List<Task>): String = BoardCodec.encodeTasks(tasks)
    fun decode(raw: String?): List<Task> = BoardCodec.decodeTasks(raw)
}
