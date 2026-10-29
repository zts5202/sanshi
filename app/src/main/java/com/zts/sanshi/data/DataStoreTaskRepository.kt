package com.zts.sanshi.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zts.sanshi.domain.AddOutcome
import com.zts.sanshi.domain.BoardSnapshot
import com.zts.sanshi.domain.HistoryDay
import com.zts.sanshi.domain.HistoryItem
import com.zts.sanshi.domain.MAX_TASKS
import com.zts.sanshi.domain.PoolItem
import com.zts.sanshi.domain.ReminderSettings
import com.zts.sanshi.domain.RepeatRule
import com.zts.sanshi.domain.StreakInfo
import com.zts.sanshi.domain.Task
import com.zts.sanshi.domain.WaitingSeries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.sanshiDataStore: DataStore<Preferences> by preferencesDataStore(name = "sanshi_today")

private val DateKey = stringPreferencesKey("today_date")
private val ItemsKey = stringPreferencesKey("items_json")
private val PoolKey = stringPreferencesKey("pool_json")
private val PendingKey = stringPreferencesKey("pending_json")
private val WaitingKey = stringPreferencesKey("waiting_json")
private val HistoryKey = stringPreferencesKey("history_json")
private val SeriesKey = stringPreferencesKey("series_json")
private val ReminderEnabledKey = booleanPreferencesKey("reminder_enabled")
private val ReminderHourKey = intPreferencesKey("reminder_hour")
private val ReminderMinuteKey = intPreferencesKey("reminder_minute")
private val StreakCurrentKey = intPreferencesKey("streak_current")
private val StreakPreviousKey = intPreferencesKey("streak_previous")
private val StreakLastCompleteKey = stringPreferencesKey("streak_last_complete")

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

    override fun observe(): Flow<BoardSnapshot> = dataStore.data.map { prefs ->
        val today = clock.today()
        readSnapshot(prefs, today)
    }

    override suspend fun refreshForToday() {
        dataStore.edit { prefs ->
            settleDay(prefs)
        }
    }

    override suspend fun add(text: String, repeat: RepeatRule): AddOutcome {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return AddOutcome.RejectedEmpty
        var outcome = AddOutcome.RejectedFull
        dataStore.edit { prefs ->
            settleDay(prefs)
            val tasks = BoardCodec.decodeTasks(prefs[ItemsKey])
            if (tasks.size >= MAX_TASKS) {
                outcome = AddOutcome.RejectedFull
                return@edit
            }
            val seriesId = if (repeat.isRepeating) ids.newId() else null
            if (seriesId != null) {
                val series = BoardCodec.decodeSeries(prefs[SeriesKey]).toMutableList()
                series.add(StoredSeries(seriesId, trimmed, StoredRepeat.from(repeat), stopped = false))
                prefs[SeriesKey] = BoardCodec.encodeSeries(series)
            }
            val next = tasks + Task(
                id = ids.newId(),
                text = trimmed,
                done = false,
                repeat = repeat,
                seriesId = seriesId,
            )
            prefs[ItemsKey] = BoardCodec.encodeTasks(next)
            outcome = AddOutcome.Added
        }
        return outcome
    }

    override suspend fun update(id: String, text: String, repeat: RepeatRule?): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        var updated = false
        dataStore.edit { prefs ->
            settleDay(prefs)
            val tasks = BoardCodec.decodeTasks(prefs[ItemsKey])
            updated = tasks.any { it.id == id }
            val next = tasks.map { task ->
                if (task.id != id) task
                else {
                    val newRepeat = repeat ?: task.repeat
                    var seriesId = task.seriesId
                    if (newRepeat.isRepeating) {
                        if (seriesId == null) {
                            seriesId = ids.newId()
                            val series = BoardCodec.decodeSeries(prefs[SeriesKey]).toMutableList()
                            series.add(StoredSeries(seriesId!!, trimmed, StoredRepeat.from(newRepeat), false))
                            prefs[SeriesKey] = BoardCodec.encodeSeries(series)
                        } else {
                            val series = BoardCodec.decodeSeries(prefs[SeriesKey]).map {
                                if (it.seriesId == seriesId) it.copy(text = trimmed, repeat = StoredRepeat.from(newRepeat)) else it
                            }
                            prefs[SeriesKey] = BoardCodec.encodeSeries(series)
                        }
                    } else if (seriesId != null && repeat != null && !repeat.isRepeating) {
                        // stop series if user clears repeat
                        val series = BoardCodec.decodeSeries(prefs[SeriesKey]).map {
                            if (it.seriesId == seriesId) it.copy(stopped = true) else it
                        }
                        prefs[SeriesKey] = BoardCodec.encodeSeries(series)
                        seriesId = null
                    }
                    task.copy(text = trimmed, repeat = newRepeat, seriesId = seriesId)
                }
            }
            prefs[ItemsKey] = BoardCodec.encodeTasks(next)
        }
        return updated
    }

    override suspend fun setDone(id: String, done: Boolean) {
        dataStore.edit { prefs ->
            settleDay(prefs)
            val tasks = BoardCodec.decodeTasks(prefs[ItemsKey]).map {
                if (it.id == id) it.copy(done = done) else it
            }
            prefs[ItemsKey] = BoardCodec.encodeTasks(tasks)
            maybeUpdateStreakLive(prefs, tasks)
        }
    }

    override suspend fun delete(id: String, stopSeries: Boolean) {
        dataStore.edit { prefs ->
            settleDay(prefs)
            val tasks = BoardCodec.decodeTasks(prefs[ItemsKey])
            val target = tasks.find { it.id == id }
            prefs[ItemsKey] = BoardCodec.encodeTasks(tasks.filterNot { it.id == id })
            if (stopSeries && target?.seriesId != null) {
                val series = BoardCodec.decodeSeries(prefs[SeriesKey]).map {
                    if (it.seriesId == target.seriesId) it.copy(stopped = true) else it
                }
                prefs[SeriesKey] = BoardCodec.encodeSeries(series)
                val waiting = BoardCodec.decodeWaiting(prefs[WaitingKey]).filterNot { it.seriesId == target.seriesId }
                prefs[WaitingKey] = BoardCodec.encodeWaiting(waiting)
            }
            fillFromWaiting(prefs)
        }
    }

    override suspend fun rollToTomorrow(id: String) {
        dataStore.edit { prefs ->
            settleDay(prefs)
            val tasks = BoardCodec.decodeTasks(prefs[ItemsKey])
            val target = tasks.find { it.id == id } ?: return@edit
            prefs[ItemsKey] = BoardCodec.encodeTasks(tasks.filterNot { it.id == id })
            // stash into a "tomorrow carry" by putting into pending-like buffer stored as pool with marker?
            // Spec: roll to tomorrow occupies tomorrow's slots. We store carry-over in a dedicated key
            // by appending to pending with a special approach: keep a "carry" list under Waiting? Better: CarryKey.
            val carry = BoardCodec.decodeTasks(prefs[CarryKey]).toMutableList()
            carry.add(target.copy(done = false))
            prefs[CarryKey] = BoardCodec.encodeTasks(carry.take(MAX_TASKS * 3))
            fillFromWaiting(prefs)
        }
    }

    override suspend fun moveToPool(id: String) {
        dataStore.edit { prefs ->
            settleDay(prefs)
            val tasks = BoardCodec.decodeTasks(prefs[ItemsKey])
            val target = tasks.find { it.id == id } ?: return@edit
            prefs[ItemsKey] = BoardCodec.encodeTasks(tasks.filterNot { it.id == id })
            val pool = BoardCodec.decodePool(prefs[PoolKey]).toMutableList()
            pool.add(0, PoolItem(id = ids.newId(), text = target.text, repeat = target.repeat))
            prefs[PoolKey] = BoardCodec.encodePool(pool)
            fillFromWaiting(prefs)
        }
    }

    override suspend fun addPool(text: String, repeat: RepeatRule): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        dataStore.edit { prefs ->
            settleDay(prefs)
            val pool = BoardCodec.decodePool(prefs[PoolKey]).toMutableList()
            pool.add(0, PoolItem(id = ids.newId(), text = trimmed, repeat = repeat))
            prefs[PoolKey] = BoardCodec.encodePool(pool)
            if (repeat.isRepeating) {
                val seriesId = ids.newId()
                val series = BoardCodec.decodeSeries(prefs[SeriesKey]).toMutableList()
                series.add(StoredSeries(seriesId, trimmed, StoredRepeat.from(repeat), false))
                prefs[SeriesKey] = BoardCodec.encodeSeries(series)
                // attach series by rewriting last pool item — keep simple: series tracked separately; pool promote will create instance
            }
        }
        return true
    }

    override suspend fun updatePool(id: String, text: String, repeat: RepeatRule?): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        var ok = false
        dataStore.edit { prefs ->
            settleDay(prefs)
            val pool = BoardCodec.decodePool(prefs[PoolKey])
            ok = pool.any { it.id == id }
            prefs[PoolKey] = BoardCodec.encodePool(
                pool.map {
                    if (it.id == id) it.copy(text = trimmed, repeat = repeat ?: it.repeat) else it
                },
            )
        }
        return ok
    }

    override suspend fun deletePool(id: String) {
        dataStore.edit { prefs ->
            settleDay(prefs)
            prefs[PoolKey] = BoardCodec.encodePool(BoardCodec.decodePool(prefs[PoolKey]).filterNot { it.id == id })
        }
    }

    override suspend fun promotePoolToToday(id: String): AddOutcome {
        var outcome = AddOutcome.RejectedFull
        dataStore.edit { prefs ->
            settleDay(prefs)
            val tasks = BoardCodec.decodeTasks(prefs[ItemsKey])
            if (tasks.size >= MAX_TASKS) {
                outcome = AddOutcome.RejectedFull
                return@edit
            }
            val pool = BoardCodec.decodePool(prefs[PoolKey])
            val item = pool.find { it.id == id } ?: return@edit
            val seriesId = if (item.repeat.isRepeating) {
                val sid = ids.newId()
                val series = BoardCodec.decodeSeries(prefs[SeriesKey]).toMutableList()
                series.add(StoredSeries(sid, item.text, StoredRepeat.from(item.repeat), false))
                prefs[SeriesKey] = BoardCodec.encodeSeries(series)
                sid
            } else null
            prefs[PoolKey] = BoardCodec.encodePool(pool.filterNot { it.id == id })
            prefs[ItemsKey] = BoardCodec.encodeTasks(
                tasks + Task(ids.newId(), item.text, false, item.repeat, seriesId),
            )
            outcome = AddOutcome.Added
        }
        return outcome
    }

    override suspend fun disposeAllRoll() {
        dataStore.edit { prefs ->
            settleDay(prefs)
            val pending = BoardCodec.decodeTasks(prefs[PendingKey])
            if (pending.isEmpty()) return@edit
            val carry = BoardCodec.decodeTasks(prefs[CarryKey]).toMutableList()
            val pool = BoardCodec.decodePool(prefs[PoolKey]).toMutableList()
            val room = (MAX_TASKS - carry.size).coerceAtLeast(0)
            pending.forEachIndexed { index, task ->
                if (index < room) {
                    carry.add(task.copy(done = false))
                } else {
                    pool.add(0, PoolItem(ids.newId(), task.text, task.repeat))
                }
            }
            prefs[CarryKey] = BoardCodec.encodeTasks(carry)
            prefs[PoolKey] = BoardCodec.encodePool(pool)
            prefs[PendingKey] = BoardCodec.encodeTasks(emptyList())
            fillFromWaiting(prefs)
        }
    }

    override suspend fun disposeAllDelete() {
        dataStore.edit { prefs ->
            settleDay(prefs)
            prefs[PendingKey] = BoardCodec.encodeTasks(emptyList())
            fillFromWaiting(prefs)
        }
    }

    override suspend fun disposeOneRoll(id: String) {
        dataStore.edit { prefs ->
            settleDay(prefs)
            val pending = BoardCodec.decodeTasks(prefs[PendingKey])
            val target = pending.find { it.id == id } ?: return@edit
            prefs[PendingKey] = BoardCodec.encodeTasks(pending.filterNot { it.id == id })
            val carry = BoardCodec.decodeTasks(prefs[CarryKey]).toMutableList()
            if (carry.size < MAX_TASKS) {
                carry.add(target.copy(done = false))
                prefs[CarryKey] = BoardCodec.encodeTasks(carry)
            } else {
                val pool = BoardCodec.decodePool(prefs[PoolKey]).toMutableList()
                pool.add(0, PoolItem(ids.newId(), target.text, target.repeat))
                prefs[PoolKey] = BoardCodec.encodePool(pool)
            }
            if (BoardCodec.decodeTasks(prefs[PendingKey]).isEmpty()) fillFromWaiting(prefs)
        }
    }

    override suspend fun disposeOnePool(id: String) {
        dataStore.edit { prefs ->
            settleDay(prefs)
            val pending = BoardCodec.decodeTasks(prefs[PendingKey])
            val target = pending.find { it.id == id } ?: return@edit
            prefs[PendingKey] = BoardCodec.encodeTasks(pending.filterNot { it.id == id })
            val pool = BoardCodec.decodePool(prefs[PoolKey]).toMutableList()
            pool.add(0, PoolItem(ids.newId(), target.text, target.repeat))
            prefs[PoolKey] = BoardCodec.encodePool(pool)
            if (BoardCodec.decodeTasks(prefs[PendingKey]).isEmpty()) fillFromWaiting(prefs)
        }
    }

    override suspend fun disposeOneDelete(id: String) {
        dataStore.edit { prefs ->
            settleDay(prefs)
            prefs[PendingKey] = BoardCodec.encodeTasks(
                BoardCodec.decodeTasks(prefs[PendingKey]).filterNot { it.id == id },
            )
            if (BoardCodec.decodeTasks(prefs[PendingKey]).isEmpty()) fillFromWaiting(prefs)
        }
    }

    override suspend fun admitWaiting(seriesId: String): AddOutcome {
        var outcome = AddOutcome.RejectedFull
        dataStore.edit { prefs ->
            settleDay(prefs)
            val tasks = BoardCodec.decodeTasks(prefs[ItemsKey])
            if (tasks.size >= MAX_TASKS) {
                outcome = AddOutcome.RejectedFull
                return@edit
            }
            val waiting = BoardCodec.decodeWaiting(prefs[WaitingKey])
            val item = waiting.find { it.seriesId == seriesId } ?: return@edit
            prefs[WaitingKey] = BoardCodec.encodeWaiting(waiting.filterNot { it.seriesId == seriesId })
            prefs[ItemsKey] = BoardCodec.encodeTasks(
                tasks + Task(ids.newId(), item.text, false, item.repeat, item.seriesId),
            )
            outcome = AddOutcome.Added
        }
        return outcome
    }

    override suspend fun setReminder(settings: ReminderSettings) {
        dataStore.edit { prefs ->
            prefs[ReminderEnabledKey] = settings.enabled
            prefs[ReminderHourKey] = settings.hour
            prefs[ReminderMinuteKey] = settings.minute
        }
    }

    private fun readSnapshot(prefs: Preferences, today: LocalDate): BoardSnapshot {
        val storedDate = prefs[DateKey]
        val tasks = if (storedDate == today.toString()) BoardCodec.decodeTasks(prefs[ItemsKey]) else emptyList()
        val pending = BoardCodec.decodeTasks(prefs[PendingKey])
        return BoardSnapshot(
            date = today,
            tasks = tasks,
            pool = BoardCodec.decodePool(prefs[PoolKey]),
            pendingIncomplete = pending,
            waiting = BoardCodec.decodeWaiting(prefs[WaitingKey]),
            history = BoardCodec.decodeHistory(prefs[HistoryKey]).sortedByDescending { it.date },
            reminder = ReminderSettings(
                enabled = prefs[ReminderEnabledKey] ?: false,
                hour = prefs[ReminderHourKey] ?: 9,
                minute = prefs[ReminderMinuteKey] ?: 0,
            ),
            streak = StreakInfo(
                current = prefs[StreakCurrentKey] ?: 0,
                previous = prefs[StreakPreviousKey] ?: 0,
            ),
        )
    }

    /**
     * Day settlement: archive previous day, queue incomplete for disposition,
     * apply tomorrow-carry into today, spawn recurring into free slots.
     */
    private fun settleDay(prefs: MutablePreferences) {
        val today = clock.today()
        val todayStr = today.toString()
        val stored = prefs[DateKey]
        if (stored == todayStr) {
            if (prefs[ItemsKey] == null) prefs[ItemsKey] = BoardCodec.encodeTasks(emptyList())
            // still try spawn if waiting and free slots (e.g. after delete)
            fillFromWaiting(prefs)
            return
        }
        if (stored != null) {
            val oldTasks = BoardCodec.decodeTasks(prefs[ItemsKey])
            if (oldTasks.isNotEmpty()) {
                val history = BoardCodec.decodeHistory(prefs[HistoryKey]).toMutableList()
                val oldDate = runCatching { LocalDate.parse(stored) }.getOrElse { today.minusDays(1) }
                history.removeAll { it.date == oldDate }
                history.add(
                    HistoryDay(
                        date = oldDate,
                        items = oldTasks.map { HistoryItem(it.text, it.done) },
                    ),
                )
                prefs[HistoryKey] = BoardCodec.encodeHistory(history.sortedByDescending { it.date }.take(400))
                updateStreakOnArchive(prefs, oldDate, oldTasks)
                val incomplete = oldTasks.filter { !it.done }
                if (incomplete.isNotEmpty()) {
                    prefs[PendingKey] = BoardCodec.encodeTasks(incomplete)
                }
            }
        }
        // Apply carry into today's board (up to 3)
        val carry = BoardCodec.decodeTasks(prefs[CarryKey])
        val todayTasks = carry.take(MAX_TASKS)
        val overflow = carry.drop(MAX_TASKS)
        prefs[CarryKey] = BoardCodec.encodeTasks(emptyList())
        if (overflow.isNotEmpty()) {
            val pool = BoardCodec.decodePool(prefs[PoolKey]).toMutableList()
            overflow.forEach { pool.add(0, PoolItem(ids.newId(), it.text, it.repeat)) }
            prefs[PoolKey] = BoardCodec.encodePool(pool)
        }
        prefs[DateKey] = todayStr
        prefs[ItemsKey] = BoardCodec.encodeTasks(todayTasks)
        prefs[WaitingKey] = BoardCodec.encodeWaiting(emptyList())
        spawnRecurring(prefs, today)
    }

    private fun spawnRecurring(prefs: MutablePreferences, today: LocalDate) {
        val series = BoardCodec.decodeSeries(prefs[SeriesKey]).filter { !it.stopped }
        val tasks = BoardCodec.decodeTasks(prefs[ItemsKey]).toMutableList()
        val presentSeries = tasks.mapNotNull { it.seriesId }.toMutableSet()
        val waiting = BoardCodec.decodeWaiting(prefs[WaitingKey]).toMutableList()
        for (s in series) {
            val rule = s.repeat.toDomain()
            if (!rule.matches(today)) continue
            if (s.seriesId in presentSeries) continue
            if (waiting.any { it.seriesId == s.seriesId }) continue
            if (tasks.size < MAX_TASKS) {
                tasks.add(Task(ids.newId(), s.text, false, rule, s.seriesId))
                presentSeries.add(s.seriesId)
            } else {
                waiting.add(WaitingSeries(s.seriesId, s.text, rule))
            }
        }
        prefs[ItemsKey] = BoardCodec.encodeTasks(tasks)
        prefs[WaitingKey] = BoardCodec.encodeWaiting(waiting)
    }

    private fun fillFromWaiting(prefs: MutablePreferences) {
        val tasks = BoardCodec.decodeTasks(prefs[ItemsKey]).toMutableList()
        val waiting = BoardCodec.decodeWaiting(prefs[WaitingKey]).toMutableList()
        while (tasks.size < MAX_TASKS && waiting.isNotEmpty()) {
            val next = waiting.removeAt(0)
            if (tasks.any { it.seriesId == next.seriesId }) continue
            tasks.add(Task(ids.newId(), next.text, false, next.repeat, next.seriesId))
        }
        prefs[ItemsKey] = BoardCodec.encodeTasks(tasks)
        prefs[WaitingKey] = BoardCodec.encodeWaiting(waiting)
    }

    private fun updateStreakOnArchive(prefs: MutablePreferences, date: LocalDate, tasks: List<Task>) {
        if (tasks.isEmpty()) return
        val complete = tasks.all { it.done }
        val last = prefs[StreakLastCompleteKey]
        val current = prefs[StreakCurrentKey] ?: 0
        if (complete) {
            val expectedPrev = date.minusDays(1).toString()
            val next = if (last == expectedPrev || last == date.toString()) current + (if (last == date.toString()) 0 else 1) else 1
            // If last complete was yesterday, continue; if same day re-archive, keep; else reset to 1
            val streak = when {
                last == date.toString() -> current.coerceAtLeast(1)
                last == expectedPrev -> current + 1
                else -> 1
            }
            if (current > 0 && streak == 1 && last != expectedPrev && last != date.toString()) {
                prefs[StreakPreviousKey] = current
            }
            prefs[StreakCurrentKey] = streak
            prefs[StreakLastCompleteKey] = date.toString()
        } else {
            if (current > 0) {
                prefs[StreakPreviousKey] = current
                prefs[StreakCurrentKey] = 0
            }
        }
    }

    private fun maybeUpdateStreakLive(prefs: MutablePreferences, tasks: List<Task>) {
        // Live streak is finalized on archive; optional soft update when today completes — skip to avoid double count
    }

    companion object {
        private val CarryKey = stringPreferencesKey("carry_json")
    }
}
