package com.zts.sanshi.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.zts.sanshi.domain.AddOutcome
import com.zts.sanshi.domain.MAX_TASKS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class DataStoreTaskRepositoryTest {
    @Test
    fun storesAtMostThreeAndRejectsBlank() = runTest {
        val (repo, _) = open()
        assertEquals(AddOutcome.RejectedEmpty, repo.add("   "))
        assertEquals(AddOutcome.Added, repo.add("一"))
        assertEquals(AddOutcome.Added, repo.add("二"))
        assertEquals(AddOutcome.Added, repo.add("三"))
        assertEquals(AddOutcome.RejectedFull, repo.add("四"))
        assertEquals(listOf("一", "二", "三"), repo.observe().first().tasks.map { it.text })
        assertEquals(MAX_TASKS, repo.observe().first().tasks.size)
    }

    @Test
    fun sameDayDataSurvivesANewRepository() = runTest {
        val (repo, clock) = open()
        repo.add("买牛奶")
        repo.setDone(repo.observe().first().tasks.single().id, true)
        val reopened = DataStoreTaskRepository(repoStore, clock, SequentialIds(start = 20))
        val task = reopened.observe().first().tasks.single()
        assertEquals("买牛奶", task.text)
        assertTrue(task.done)
    }

    @Test
    fun dateChangeArchivesAndQueuesIncomplete() = runTest {
        val (repo, clock) = open()
        repo.add("昨天的事")
        repo.add("另一件")
        val id = repo.observe().first().tasks.first().id
        repo.setDone(id, true)
        clock.date = clock.date.plusDays(1)
        repo.refreshForToday()
        val snap = repo.observe().first()
        assertEquals(clock.date, snap.date)
        assertTrue(snap.tasks.isEmpty())
        assertEquals(1, snap.pendingIncomplete.size)
        assertEquals("另一件", snap.pendingIncomplete.single().text)
        assertTrue(snap.history.any { it.items.any { i -> i.text == "昨天的事" && i.done } })
        repo.disposeAllDelete()
        assertTrue(repo.observe().first().pendingIncomplete.isEmpty())
        assertEquals(AddOutcome.Added, repo.add("今天的第一件"))
        assertEquals(listOf("今天的第一件"), repo.observe().first().tasks.map { it.text })
    }

    @Test
    fun deleteFreesASlotAndBlankUpdateIsRejected() = runTest {
        val (repo, _) = open()
        repo.add("一")
        repo.add("二")
        repo.add("三")
        val first = repo.observe().first().tasks.first()
        assertFalse(repo.update(first.id, "  "))
        assertEquals("一", repo.observe().first().tasks.first().text)
        repo.delete(first.id)
        assertEquals(AddOutcome.Added, repo.add("四"))
        assertEquals(listOf("二", "三", "四"), repo.observe().first().tasks.map { it.text })
    }

    @Test
    fun poolPromoteRespectsCap() = runTest {
        val (repo, _) = open()
        assertTrue(repo.addPool("灵感"))
        repo.add("一")
        repo.add("二")
        repo.add("三")
        val poolId = repo.observe().first().pool.single().id
        assertEquals(AddOutcome.RejectedFull, repo.promotePoolToToday(poolId))
        repo.delete(repo.observe().first().tasks.first().id)
        assertEquals(AddOutcome.Added, repo.promotePoolToToday(poolId))
        assertTrue(repo.observe().first().pool.isEmpty())
    }

    private lateinit var repoStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>

    private fun TestScope.open(): Pair<DataStoreTaskRepository, MutableClock> {
        val dir = File(System.getProperty("java.io.tmpdir"), "sanshi-${System.nanoTime()}")
        dir.mkdirs()
        val clock = MutableClock(LocalDate.of(2026, 10, 28))
        val store = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(dir, "sanshi.preferences_pb") },
        )
        repoStore = store
        return DataStoreTaskRepository(store, clock, SequentialIds()) to clock
    }

    private class MutableClock(var date: LocalDate) : DayClock {
        override fun today(): LocalDate = date
    }

    private class SequentialIds(start: Int = 1) : TaskIds {
        private var n = start
        override fun newId(): String = (n++).toString()
    }
}
