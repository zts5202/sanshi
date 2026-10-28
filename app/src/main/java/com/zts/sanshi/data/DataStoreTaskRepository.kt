package com.zts.sanshi.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zts.sanshi.domain.AddOutcome
import com.zts.sanshi.domain.MAX_TASKS
import com.zts.sanshi.domain.Task
import com.zts.sanshi.domain.TodaySnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sanshiDataStore: DataStore<Preferences> by preferencesDataStore(name = "sanshi_today")

private val DateKey = stringPreferencesKey("today_date")
private val ItemsKey = stringPreferencesKey("items_json")

fun taskRepository(context: Context): TaskRepository {
    return DataStoreTaskRepository(
        dataStore = context.applicationContext.sanshiDataStore,
        clock = SystemDayClock,
        ids = UuidTaskIds,
    )
}

class DataStoreTaskRepository(
    private val dataStore: DataStore<Preferences>,
    private val clock: DayClock,
    private val ids: TaskIds,
) : TaskRepository {

    override fun observe(): Flow<TodaySnapshot> = dataStore.data.map { prefs ->
        val today = clock.today()
        TodaySnapshot(date = today, tasks = tasksForToday(prefs, today.toString()))
    }

    override suspend fun refreshForToday() {
        val today = clock.today().toString()
        dataStore.edit { prefs ->
            if (prefs[DateKey] != today) {
                prefs[DateKey] = today
                prefs[ItemsKey] = TaskCodec.encode(emptyList())
            } else if (prefs[ItemsKey] == null) {
                prefs[ItemsKey] = TaskCodec.encode(emptyList())
            }
        }
    }

    override suspend fun add(text: String): AddOutcome {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return AddOutcome.RejectedEmpty
        val id = ids.newId()
        var outcome = AddOutcome.RejectedFull
        dataStore.edit { prefs ->
            val tasks = rolledTasks(prefs)
            outcome = if (tasks.size >= MAX_TASKS && tasks.none { it.id == id }) {
                AddOutcome.RejectedFull
            } else {
                AddOutcome.Added
            }
            val next = when {
                tasks.any { it.id == id } -> tasks
                tasks.size >= MAX_TASKS -> tasks
                else -> tasks + Task(id = id, text = trimmed, done = false)
            }
            prefs[ItemsKey] = TaskCodec.encode(next)
        }
        return outcome
    }

    override suspend fun update(id: String, text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        var updated = false
        dataStore.edit { prefs ->
            val tasks = rolledTasks(prefs)
            updated = tasks.any { it.id == id }
            val next = tasks.map { task ->
                if (task.id == id) task.copy(text = trimmed) else task
            }
            prefs[ItemsKey] = TaskCodec.encode(next)
        }
        return updated
    }

    override suspend fun setDone(id: String, done: Boolean) {
        dataStore.edit { prefs ->
            val tasks = rolledTasks(prefs).map { task ->
                if (task.id == id) task.copy(done = done) else task
            }
            prefs[ItemsKey] = TaskCodec.encode(tasks)
        }
    }

    override suspend fun delete(id: String) {
        dataStore.edit { prefs ->
            val tasks = rolledTasks(prefs).filterNot { it.id == id }
            prefs[ItemsKey] = TaskCodec.encode(tasks)
        }
    }

    private fun rolledTasks(prefs: MutablePreferences): List<Task> {
        val today = clock.today().toString()
        val tasks = tasksForToday(prefs, today)
        prefs[DateKey] = today
        return tasks
    }

    private fun tasksForToday(prefs: Preferences, today: String): List<Task> {
        if (prefs[DateKey] != today) return emptyList()
        return TaskCodec.decode(prefs[ItemsKey])
    }
}
