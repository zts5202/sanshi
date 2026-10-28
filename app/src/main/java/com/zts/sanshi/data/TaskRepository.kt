package com.zts.sanshi.data

import com.zts.sanshi.domain.AddOutcome
import com.zts.sanshi.domain.TodaySnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Today's list only. A date change drops yesterday; nothing is kept to look back on.
 */
interface TaskRepository {
    fun observe(): Flow<TodaySnapshot>

    suspend fun refreshForToday()

    suspend fun add(text: String): AddOutcome

    suspend fun update(id: String, text: String): Boolean

    suspend fun setDone(id: String, done: Boolean)

    suspend fun delete(id: String)
}
