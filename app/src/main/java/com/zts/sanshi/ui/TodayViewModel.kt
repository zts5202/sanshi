package com.zts.sanshi.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zts.sanshi.data.TaskRepository
import com.zts.sanshi.data.taskRepository
import com.zts.sanshi.domain.AddOutcome
import com.zts.sanshi.domain.MAX_TASKS
import com.zts.sanshi.domain.TodaySnapshot
import com.zts.sanshi.domain.formatChineseDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TodayViewModel(
    private val repository: TaskRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private var hasSnapshot = false
    private var wasAllDone = false

    init {
        viewModelScope.launch {
            repository.refreshForToday()
            repository.observe().collect { snapshot -> applySnapshot(snapshot) }
        }
    }

    fun onForeground() {
        viewModelScope.launch { repository.refreshForToday() }
    }

    fun onAddClick() {
        if (_uiState.value.isFull) return
        _uiState.update { it.copy(editor = EditorRequest(taskId = null, initialText = "")) }
    }

    fun onTaskClick(id: String) {
        val task = _uiState.value.tasks.find { it.id == id } ?: return
        _uiState.update { it.copy(editor = EditorRequest(taskId = task.id, initialText = task.text)) }
    }

    fun onToggle(id: String, done: Boolean) {
        viewModelScope.launch { repository.setDone(id, done) }
    }

    fun onDeleteClick(id: String) {
        if (_uiState.value.tasks.none { it.id == id }) return
        _uiState.update { it.copy(pendingDeleteId = id) }
    }

    fun onConfirmDelete() {
        val id = _uiState.value.pendingDeleteId ?: return
        _uiState.update { it.copy(pendingDeleteId = null) }
        viewModelScope.launch { repository.delete(id) }
    }

    fun onDismissDelete() {
        _uiState.update { it.copy(pendingDeleteId = null) }
    }

    fun onDismissFullHint() {
        _uiState.update { it.copy(fullHintDismissed = true) }
    }

    fun onDismissEditor() {
        _uiState.update { it.copy(editor = null) }
    }

    fun onSaveEditor(text: String) {
        val editor = _uiState.value.editor ?: return
        viewModelScope.launch {
            val saved = if (editor.taskId == null) {
                repository.add(text) == AddOutcome.Added
            } else {
                repository.update(editor.taskId, text)
            }
            if (saved) {
                _uiState.update { it.copy(editor = null) }
            }
        }
    }

    private fun applySnapshot(snapshot: TodaySnapshot) {
        val allDone = snapshot.tasks.isNotEmpty() && snapshot.tasks.all { it.done }
        val animateEdge = hasSnapshot && allDone && !wasAllDone
        hasSnapshot = true
        wasAllDone = allDone
        _uiState.update { previous ->
            previous.copy(
                dateLabel = formatChineseDate(snapshot.date),
                tasks = snapshot.tasks,
                fullHintDismissed = if (snapshot.tasks.size >= MAX_TASKS) {
                    previous.fullHintDismissed
                } else {
                    false
                },
                celebrationGeneration = previous.celebrationGeneration + if (animateEdge) 1 else 0,
                editor = previous.editor?.takeIf { request ->
                    request.taskId == null || snapshot.tasks.any { it.id == request.taskId }
                },
                pendingDeleteId = previous.pendingDeleteId?.takeIf { id ->
                    snapshot.tasks.any { it.id == id }
                },
            )
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (!modelClass.isAssignableFrom(TodayViewModel::class.java)) {
                        error("Unknown model class ${modelClass.name}")
                    }
                    @Suppress("UNCHECKED_CAST")
                    return TodayViewModel(taskRepository(appContext)) as T
                }
            }
        }
    }
}
