package com.antigravity.somnolog.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.somnolog.SomnoLogApp
import com.antigravity.somnolog.domain.model.AudioEvent
import com.antigravity.somnolog.domain.model.SleepSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel : ViewModel() {

    private val repository = SomnoLogApp.instance.repository

    val allSessions: StateFlow<List<SleepSession>> = repository.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedSessionId = MutableStateFlow<Long?>(null)
    val selectedSessionId: StateFlow<Long?> = _selectedSessionId.asStateFlow()

    val sessionClips: StateFlow<List<AudioEvent>> = _selectedSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getEventsForSession(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectSession(sessionId: Long?) {
        _selectedSessionId.value = sessionId
    }

    fun deleteClip(event: AudioEvent) {
        viewModelScope.launch {
            repository.deleteAudioEvent(event)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            if (_selectedSessionId.value == sessionId) {
                _selectedSessionId.value = null
            }
            repository.deleteSession(sessionId)
        }
    }
}
