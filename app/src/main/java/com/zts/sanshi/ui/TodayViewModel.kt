package com.zts.sanshi.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zts.sanshi.data.TaskRepository
import com.zts.sanshi.data.taskRepository
import com.zts.sanshi.domain.AddOutcome
import com.zts.sanshi.domain.BoardSnapshot
import com.zts.sanshi.domain.HistoryDay
import com.zts.sanshi.domain.MAX_TASKS
import com.zts.sanshi.domain.ReminderSettings
import com.zts.sanshi.domain.RepeatRule
import com.zts.sanshi.domain.formatChineseDate
import com.zts.sanshi.reminder.ReminderScheduler
import com.zts.sanshi.widget.TodayWidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TodayViewModel(
    private val repository: TaskRepository,
    private val appContext: Context? = null,
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
        _uiState.update {
            it.copy(editor = EditorRequest(forPool = false, initialText = "", initialRepeat = RepeatRule.None))
        }
    }

    fun onAddPoolClick() {
        _uiState.update {
            it.copy(editor = EditorRequest(forPool = true, initialText = "", initialRepeat = RepeatRule.None))
        }
    }

    fun onTaskClick(id: String) {
        val task = _uiState.value.tasks.find { it.id == id } ?: return
        _uiState.update {
            it.copy(
                editor = EditorRequest(
                    taskId = task.id,
                    initialText = task.text,
                    initialRepeat = task.repeat,
                    forPool = false,
                ),
            )
        }
    }

    fun onPoolClick(id: String) {
        val item = _uiState.value.pool.find { it.id == id } ?: return
        _uiState.update {
            it.copy(
                editor = EditorRequest(
                    poolId = item.id,
                    initialText = item.text,
                    initialRepeat = item.repeat,
                    forPool = true,
                ),
            )
        }
    }

    fun onToggle(id: String, done: Boolean) {
        viewModelScope.launch {
            repository.setDone(id, done)
            refreshWidget()
        }
    }

    fun onDeleteClick(id: String) {
        val task = _uiState.value.tasks.find { it.id == id } ?: return
        _uiState.update {
            it.copy(pendingDelete = DeleteRequest(id = id, isSeries = task.seriesId != null && task.repeat.isRepeating))
        }
    }

    fun onDeletePoolClick(id: String) {
        _uiState.update {
            it.copy(pendingDelete = DeleteRequest(id = id, isSeries = false, fromPool = true))
        }
    }

    fun onConfirmDelete(stopSeries: Boolean = false) {
        val req = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            when {
                req.fromPool -> repository.deletePool(req.id)
                req.fromPending -> repository.disposeOneDelete(req.id)
                else -> repository.delete(req.id, stopSeries = stopSeries)
            }
            refreshWidget()
        }
    }

    fun onDismissDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    fun onDismissFullHint() {
        _uiState.update { it.copy(fullHintDismissed = true) }
    }

    fun onDismissEditor() {
        _uiState.update { it.copy(editor = null) }
    }

    fun onSaveEditor(text: String, repeat: RepeatRule) {
        val editor = _uiState.value.editor ?: return
        if (repeat.kind == com.zts.sanshi.domain.RepeatKind.Weekly && repeat.weekDays.isEmpty()) return
        viewModelScope.launch {
            val saved = when {
                editor.forPool && editor.poolId == null -> repository.addPool(text, repeat)
                editor.forPool && editor.poolId != null -> repository.updatePool(editor.poolId, text, repeat)
                editor.taskId == null -> repository.add(text, repeat) == AddOutcome.Added
                else -> repository.update(editor.taskId, text, repeat)
            }
            if (saved) {
                _uiState.update { it.copy(editor = null) }
                refreshWidget()
            }
        }
    }

    fun onPromotePool(id: String) {
        viewModelScope.launch {
            if (repository.promotePoolToToday(id) == AddOutcome.Added) {
                _uiState.update { it.copy(snack = SnackKind.Joined) }
                refreshWidget()
            }
        }
    }

    fun onRollTomorrow(id: String) {
        viewModelScope.launch {
            repository.rollToTomorrow(id)
            _uiState.update { it.copy(snack = SnackKind.Rolled) }
            refreshWidget()
        }
    }

    fun onMoveToPool(id: String) {
        viewModelScope.launch {
            repository.moveToPool(id)
            _uiState.update { it.copy(snack = SnackKind.Pooled) }
            refreshWidget()
        }
    }

    fun onAdmitWaiting(seriesId: String) {
        viewModelScope.launch {
            repository.admitWaiting(seriesId)
            refreshWidget()
        }
    }

    fun onAdmitFirstWaiting() {
        val first = _uiState.value.waiting.firstOrNull() ?: return
        onAdmitWaiting(first.seriesId)
    }

    fun onDisposeAllRoll() {
        viewModelScope.launch {
            repository.disposeAllRoll()
            _uiState.update { it.copy(showDisposition = false, showDisposeEach = false) }
            refreshWidget()
        }
    }

    fun onDisposeAllDeleteAsk() {
        _uiState.update { it.copy(showDeleteAllConfirm = true) }
    }

    fun onDisposeAllDeleteConfirm() {
        viewModelScope.launch {
            repository.disposeAllDelete()
            _uiState.update {
                it.copy(showDeleteAllConfirm = false, showDisposition = false, showDisposeEach = false)
            }
            refreshWidget()
        }
    }

    fun onDismissDeleteAllConfirm() {
        _uiState.update { it.copy(showDeleteAllConfirm = false) }
    }

    fun onDisposeEach() {
        _uiState.update { it.copy(showDisposition = false, showDisposeEach = true) }
    }

    fun onDisposeOneRoll(id: String) {
        viewModelScope.launch {
            repository.disposeOneRoll(id)
            maybeLeaveDisposeEach()
            refreshWidget()
        }
    }

    fun onDisposeOnePool(id: String) {
        viewModelScope.launch {
            repository.disposeOnePool(id)
            maybeLeaveDisposeEach()
            refreshWidget()
        }
    }

    fun onDisposeOneDelete(id: String) {
        viewModelScope.launch {
            repository.disposeOneDelete(id)
            maybeLeaveDisposeEach()
            refreshWidget()
        }
    }

    fun onSetReminder(settings: ReminderSettings) {
        viewModelScope.launch {
            repository.setReminder(settings)
            appContext?.let { ReminderScheduler.apply(it, settings) }
        }
    }

    fun onOpenHistoryDay(day: HistoryDay) {
        _uiState.update { it.copy(selectedHistory = day) }
    }

    fun onCloseHistoryDay() {
        _uiState.update { it.copy(selectedHistory = null) }
    }

    fun clearSnack() {
        _uiState.update { it.copy(snack = null) }
    }

    private fun maybeLeaveDisposeEach() {
        _uiState.update { state ->
            if (state.pendingIncomplete.isEmpty()) {
                state.copy(showDisposeEach = false, showDisposition = false)
            } else state
        }
    }

    private fun applySnapshot(snapshot: BoardSnapshot) {
        val allDone = snapshot.tasks.isNotEmpty() && snapshot.tasks.all { it.done }
        val animateEdge = hasSnapshot && allDone && !wasAllDone
        hasSnapshot = true
        wasAllDone = allDone
        _uiState.update { previous ->
            snapshot.toBaseState(formatChineseDate(snapshot.date), previous).copy(
                celebrationGeneration = previous.celebrationGeneration + if (animateEdge) 1 else 0,
                showDisposeEach = if (snapshot.pendingIncomplete.isEmpty()) false else previous.showDisposeEach,
            )
        }
        refreshWidget()
    }

    private fun refreshWidget() {
        val ctx = appContext ?: return
        TodayWidgetUpdater.requestUpdate(ctx)
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
                    return TodayViewModel(taskRepository(appContext), appContext) as T
                }
            }
        }
    }
}
