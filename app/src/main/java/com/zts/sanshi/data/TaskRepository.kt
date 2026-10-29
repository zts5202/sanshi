package com.zts.sanshi.data

import com.zts.sanshi.domain.AddOutcome
import com.zts.sanshi.domain.BoardSnapshot
import com.zts.sanshi.domain.PoolItem
import com.zts.sanshi.domain.ReminderSettings
import com.zts.sanshi.domain.RepeatRule
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observe(): Flow<BoardSnapshot>

    suspend fun refreshForToday()

    suspend fun add(text: String, repeat: RepeatRule = RepeatRule.None): AddOutcome

    suspend fun update(id: String, text: String, repeat: RepeatRule? = null): Boolean

    suspend fun setDone(id: String, done: Boolean)

    suspend fun delete(id: String, stopSeries: Boolean = false)

    suspend fun rollToTomorrow(id: String)

    suspend fun moveToPool(id: String)

    suspend fun addPool(text: String, repeat: RepeatRule = RepeatRule.None): Boolean

    suspend fun updatePool(id: String, text: String, repeat: RepeatRule? = null): Boolean

    suspend fun deletePool(id: String)

    suspend fun promotePoolToToday(id: String): AddOutcome

    suspend fun disposeAllRoll()

    suspend fun disposeAllDelete()

    suspend fun disposeOneRoll(id: String)

    suspend fun disposeOnePool(id: String)

    suspend fun disposeOneDelete(id: String)

    suspend fun admitWaiting(seriesId: String): AddOutcome

    suspend fun setReminder(settings: ReminderSettings)
}
