package com.zts.sanshi.ui

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.zts.sanshi.data.DataStoreTaskRepository
import com.zts.sanshi.data.DayClock
import com.zts.sanshi.data.TaskIds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun celebrationGenerationIncrementsOnEdgeOnly() = runTest {
        val repo = openRepo(backgroundScope)
        repo.add("一")
        val vm = TodayViewModel(repo)
        awaitTasks(vm)
        val gen0 = vm.uiState.value.celebrationGeneration
        val id = vm.uiState.value.tasks.single().id
        vm.onToggle(id, true)
        awaitDone(vm)
        val gen1 = vm.uiState.value.celebrationGeneration
        assertEquals(gen0 + 1, gen1)
        assertTrue(vm.uiState.value.allDone)
        vm.onToggle(id, true)
        advanceUntilIdle()
        yield()
        assertEquals(gen1, vm.uiState.value.celebrationGeneration)
    }

    @Test
    fun fullBlocksAddEditor() = runTest {
        val repo = openRepo(backgroundScope)
        repo.add("一")
        repo.add("二")
        repo.add("三")
        assertEquals(3, repo.observe().first().tasks.size)
        val vm = TodayViewModel(repo)
        awaitTasks(vm, min = 3)
        assertTrue(vm.uiState.value.isFull)
        vm.onAddClick()
        advanceUntilIdle()
        assertNull(vm.uiState.value.editor)
    }

    private suspend fun TestScope.awaitTasks(vm: TodayViewModel, min: Int = 1) {
        var spins = 0
        while (vm.uiState.value.tasks.size < min && spins < 100) {
            yield()
            advanceUntilIdle()
            spins++
        }
        assertTrue("tasks=${vm.uiState.value.tasks}", vm.uiState.value.tasks.size >= min)
    }

    private suspend fun TestScope.awaitDone(vm: TodayViewModel) {
        var spins = 0
        while (!vm.uiState.value.allDone && spins < 100) {
            yield()
            advanceUntilIdle()
            spins++
        }
        assertTrue(vm.uiState.value.allDone)
    }

    private fun openRepo(scope: CoroutineScope): DataStoreTaskRepository {
        val dir = File(System.getProperty("java.io.tmpdir"), "sanshi-vm-${System.nanoTime()}")
        dir.mkdirs()
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(dir, "prefs.preferences_pb") },
        )
        val clock = object : DayClock {
            override fun today() = LocalDate.of(2026, 10, 28)
        }
        val ids = object : TaskIds {
            private var n = 1
            override fun newId() = (n++).toString()
        }
        return DataStoreTaskRepository(store, clock, ids)
    }
}
