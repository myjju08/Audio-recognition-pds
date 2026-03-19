package com.earbrief.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.TriggerType
import com.earbrief.app.service.PipelineOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class TriggerLogViewModel @Inject constructor(
    orchestrator: PipelineOrchestrator
) : ViewModel() {
    private val selectedFilter = MutableStateFlow<TriggerType?>(null)
    val filter: StateFlow<TriggerType?> = selectedFilter

    val events: StateFlow<List<TriggerEvent>> = combine(
        orchestrator.recentTriggerEvents,
        selectedFilter
    ) { events, type ->
        if (type == null) events.asReversed() else events.asReversed().filter { it.triggerType == type }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(triggerType: TriggerType?) {
        selectedFilter.value = triggerType
    }
}
