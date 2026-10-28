package com.zts.sanshi.ui

import com.zts.sanshi.data.TaskRepository
import com.zts.sanshi.domain.AddOutcome
import com.zts.sanshi.domain.Task
import com.zts.sanshi.domain.TodaySnapshot
import com.zts.sanshi.domain.formatChineseDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class TodayViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun coldStartAndForegroundRefreshTheStoredDay() {
        val repo = FakeTaskRepository(LocalDate.of(2026, 10, 28))
        val viewModel = TodayViewModel(repo)
        assertTrue(repo.refreshCalls >= 1)
        val before = repo.refreshCalls
        viewModel.onForeground()
        assertTrue(repo.refreshCalls > before)
    }

    @Test
    fun blocksAFourthItemAndShowsAPersistentHint() {
        val repo = FakeTaskRepository(LocalDate.of(2026, 10, 28))
        val viewModel = TodayViewModel(repo)
        repeat(3) { index -> repo.addBlocking("事项$index") }
        assertTrue(viewModel.uiState.value.isFull)
        assertTrue(viewModel.uiState.value.showFullHint)
        viewModel.onAddClick()
        assertNull(viewModel.uiState.value.editor)
        viewModel.onDismissFullHint()
        assertFalse(viewModel.uiState.value.showFullHint)
        repo.deleteBlocking(repo.snapshot().tasks.first().id)
        assertFalse(viewModel.uiState.value.showFullHint)
        repo.addBlocking("补上")
        repo.addBlocking("再补")
        assertTrue(viewModel.uiState.value.showFullHint)
    }

    @Test
    fun celebrationAnimatesOnlyOnTheEdgeToAllDone() {
        val repo = FakeTaskRepository(
            date = LocalDate.of(2026, 10, 28),
            tasks = listOf(Task("1", "已经做完", done = true)),
        )
        val alreadyDone = TodayViewModel(repo)
        assertTrue(alreadyDone.uiState.value.allDone)
        assertEquals(0, alreadyDone.uiState.value.celebrationGeneration)

        val open = FakeTaskRepository(
            date = LocalDate.of(2026, 10, 28),
            tasks = listOf(Task("1", "还没做", done = false)),
        )
        val viewModel = TodayViewModel(open)
        assertFalse(viewModel.uiState.value.allDone)
        assertEquals(0, viewModel.uiState.value.celebrationGeneration)
        open.setDoneBlocking("1", true)
        assertTrue(viewModel.uiState.value.allDone)
        assertEquals(1, viewModel.uiState.value.celebrationGeneration)
        open.setDoneBlocking("1", false)
        open.setDoneBlocking("1", true)
        assertEquals(2, viewModel.uiState.value.celebrationGeneration)
    }

    @Test
    fun foregroundOnANewDayClearsTheList() {
        val repo = FakeTaskRepository(LocalDate.of(2026, 10, 28))
        val viewModel = TodayViewModel(repo)
        repo.addBlocking("只属于今天")
        assertEquals(1, viewModel.uiState.value.tasks.size)
        repo.date = LocalDate.of(2026, 10, 29)
        viewModel.onForeground()
        assertTrue(viewModel.uiState.value.tasks.isEmpty())
        assertEquals("10月29日 · 周四", viewModel.uiState.value.dateLabel)
        assertEquals(formatChineseDate(LocalDate.of(2026, 10, 29)), viewModel.uiState.value.dateLabel)
    }

    @Test
    fun blankSaveKeepsTheEditorOpen() {
        val repo = FakeTaskRepository(LocalDate.of(2026, 10, 28))
        val viewModel = TodayViewModel(repo)
        viewModel.onAddClick()
        viewModel.onSaveEditor("   ")
        assertEquals("", viewModel.uiState.value.editor?.initialText)
        assertTrue(repo.snapshot().tasks.isEmpty())
        viewModel.onSaveEditor("写下来")
        assertNull(viewModel.uiState.value.editor)
        assertEquals(listOf("写下来"), viewModel.uiState.value.tasks.map { it.text })
    }
}

private class FakeTaskRepository(
    date: LocalDate,
    tasks: List<Task> = emptyList(),
) : TaskRepository {
    var date: LocalDate = date
    var refreshCalls: Int = 0
    private val state = MutableStateFlow(TodaySnapshot(date, tasks))
    private var ids = 0

    override fun observe(): Flow<TodaySnapshot> = state

    override suspend fun refreshForToday() {
        refreshCalls += 1
        if (state.value.date != date) {
            state.value = TodaySnapshot(date, emptyList())
        }
    }

    override suspend fun add(text: String): AddOutcome {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return AddOutcome.RejectedEmpty
        val current = state.value
        if (current.tasks.size >= 3) return AddOutcome.RejectedFull
        state.value = current.copy(
            tasks = current.tasks + Task(id = "id-${++ids}", text = trimmed, done = false),
        )
        return AddOutcome.Added
    }

    override suspend fun update(id: String, text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        var updated = false
        state.value = state.value.copy(
            tasks = state.value.tasks.map { task ->
                if (task.id == id) {
                    updated = true
                    task.copy(text = trimmed)
                } else {
                    task
                }
            },
        )
        return updated
    }

    override suspend fun setDone(id: String, done: Boolean) {
        state.value = state.value.copy(
            tasks = state.value.tasks.map { task ->
                if (task.id == id) task.copy(done = done) else task
            },
        )
    }

    override suspend fun delete(id: String) {
        state.value = state.value.copy(tasks = state.value.tasks.filterNot { it.id == id })
    }

    fun addBlocking(text: String) {
        kotlinx.coroutines.runBlocking { add(text) }
    }

    fun deleteBlocking(id: String) {
        kotlinx.coroutines.runBlocking { delete(id) }
    }

    fun setDoneBlocking(id: String, done: Boolean) {
        kotlinx.coroutines.runBlocking { setDone(id, done) }
    }

    fun snapshot(): TodaySnapshot = kotlinx.coroutines.runBlocking { observe().first() }
}
