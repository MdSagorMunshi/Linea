package com.ryanshelby.linea.smartfilter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedDeque
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartFilterUndoManager @Inject constructor() {

    private val undoStack = ConcurrentLinkedDeque<UndoRecord>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _latestUndoRecord = MutableStateFlow<UndoRecord?>(null)
    val latestUndoRecord: StateFlow<UndoRecord?> = _latestUndoRecord.asStateFlow()

    fun pushUndo(record: UndoRecord) {
        undoStack.push(record)
        // Keep at most 25 undo steps to prevent unbounded memory growth
        while (undoStack.size > 25) {
            undoStack.removeLast()
        }
        _canUndo.value = true
        _latestUndoRecord.value = record
    }

    fun popUndo(): UndoRecord? {
        val record = if (undoStack.isNotEmpty()) undoStack.pop() else null
        _canUndo.value = undoStack.isNotEmpty()
        _latestUndoRecord.value = undoStack.peek()
        return record
    }

    fun peekUndo(): UndoRecord? {
        return undoStack.peek()
    }

    fun clear() {
        undoStack.clear()
        _canUndo.value = false
        _latestUndoRecord.value = null
    }

    fun getHistory(): List<UndoRecord> {
        return undoStack.toList()
    }
}

