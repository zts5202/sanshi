package com.zts.sanshi.data

import com.zts.sanshi.domain.MAX_TASKS
import com.zts.sanshi.domain.Task
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
internal data class StoredTask(
    val id: String,
    val text: String,
    val done: Boolean,
)

internal object TaskCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(tasks: List<Task>): String {
        val stored = tasks.take(MAX_TASKS).map { StoredTask(it.id, it.text, it.done) }
        return json.encodeToString(stored)
    }

    fun decode(raw: String?): List<Task> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<StoredTask>>(raw)
                .mapNotNull { stored ->
                    if (stored.id.isBlank() || stored.text.isBlank()) {
                        null
                    } else {
                        Task(id = stored.id, text = stored.text, done = stored.done)
                    }
                }
                .take(MAX_TASKS)
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }
}
