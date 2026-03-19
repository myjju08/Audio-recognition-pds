package com.earbrief.app.presentation.ui.triggerlog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.earbrief.app.R
import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.TriggerType
import com.earbrief.app.presentation.viewmodel.TriggerLogViewModel

@Composable
fun TriggerLogScreen(viewModel: TriggerLogViewModel = hiltViewModel()) {
    val events by viewModel.events.collectAsState()
    val filter by viewModel.filter.collectAsState()
    var selected by remember { mutableStateOf<TriggerEvent?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.log_title)) })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LogFilterChip(stringResource(R.string.filter_all), filter == null) { viewModel.setFilter(null) }
                LogFilterChip(stringResource(R.string.filter_schedule), filter == TriggerType.SCHEDULE_GAP) { viewModel.setFilter(TriggerType.SCHEDULE_GAP) }
                LogFilterChip(stringResource(R.string.filter_silence), filter == TriggerType.SILENCE_DETECT) { viewModel.setFilter(TriggerType.SILENCE_DETECT) }
                LogFilterChip(stringResource(R.string.filter_keyword), filter == TriggerType.KEYWORD_INSTANT) { viewModel.setFilter(TriggerType.KEYWORD_INSTANT) }
            }

            if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.log_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(state = rememberLazyListState(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(events, key = { it.id }) { event ->
                        LogEventCard(event = event, onClick = { selected = event })
                    }
                }
            }
        }
    }

    selected?.let { event ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(logTitle(event.triggerType), style = MaterialTheme.typography.titleLarge)
                Text(event.whisperText, style = MaterialTheme.typography.bodyLarge)
                HorizontalDivider()
                Text(stringResource(R.string.log_source_label), style = MaterialTheme.typography.labelLarge)
                Text(event.sourceText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (event.detectedEntities.isNotEmpty()) {
                    HorizontalDivider()
                    Text(stringResource(R.string.log_entities_label), style = MaterialTheme.typography.labelLarge)
                    event.detectedEntities.forEach { entity ->
                        Text("• ${entity.type.name}: ${entity.value}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun LogEventCard(event: TriggerEvent, onClick: () -> Unit) {
    val (icon, color) = when (event.triggerType) {
        TriggerType.SCHEDULE_GAP -> Icons.Default.Event to Color(0xFFF5A623)
        TriggerType.SILENCE_DETECT -> Icons.Default.TipsAndUpdates to Color(0xFF5ECFB1)
        TriggerType.KEYWORD_INSTANT -> Icons.Default.Psychology to Color(0xFF7B8CDE)
        else -> Icons.Default.TipsAndUpdates to Color(0xFF7B8CDE)
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(event.whisperText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(event.sourceText, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun logTitle(type: TriggerType): String = when (type) {
    TriggerType.SCHEDULE_GAP -> stringResource(R.string.filter_schedule)
    TriggerType.SILENCE_DETECT -> stringResource(R.string.filter_silence)
    TriggerType.KEYWORD_INSTANT -> stringResource(R.string.filter_keyword)
    else -> type.name
}
