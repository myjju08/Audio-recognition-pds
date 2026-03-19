package com.earbrief.app.presentation.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.earbrief.app.R
import com.earbrief.app.engine.stt.SttConnectionState
import com.earbrief.app.presentation.viewmodel.MainViewModel

@Composable
fun MainScreen(
    onOpenLog: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val listeningState by viewModel.listeningState.collectAsState()
    val vadState by viewModel.vadState.collectAsState()
    val speechProbability by viewModel.speechProbability.collectAsState()
    val whisperCount by viewModel.whisperCount.collectAsState()
    val sttConnectionState by viewModel.sttConnectionState.collectAsState()
    val interimTranscript by viewModel.interimTranscript.collectAsState()
    val finalTranscripts by viewModel.finalTranscripts.collectAsState()
    val sessionId by viewModel.sessionId.collectAsState()
    val recentTriggerEvents by viewModel.recentTriggerEvents.collectAsState()

    var showCards by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showCards = true }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.Text(
                    text = if (listeningState == com.earbrief.app.domain.model.ListeningState.LISTENING) {
                        stringResource(R.string.main_greeting_active)
                    } else {
                        stringResource(R.string.main_greeting_idle)
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                )
                androidx.compose.material3.Text(
                    text = stringResource(R.string.main_subtitle),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        items(
            listOf("hero", "stats", "whispers", "transcript"),
            key = { it }
        ) { key ->
            val index = listOf("hero", "stats", "whispers", "transcript").indexOf(key)
            AnimatedVisibility(
                visible = showCards,
                enter = fadeIn(animationSpec = tween(350, delayMillis = index * 90)) +
                    slideInVertically(animationSpec = tween(420, delayMillis = index * 90), initialOffsetY = { it / 6 })
            ) {
                when (key) {
                    "hero" -> ListeningStatusCard(
                        listeningState = listeningState,
                        vadState = vadState,
                        speechProbability = speechProbability,
                        sessionDuration = viewModel.getSessionDurationFormatted(),
                        onToggle = viewModel::toggleListening,
                        onStop = viewModel::stopListening
                    )

                    "stats" -> QuickStatsCard(
                        whisperCount = whisperCount,
                        sessionDuration = viewModel.getSessionDurationFormatted(),
                        sessionId = sessionId
                    )

                    "whispers" -> RecentWhispersCard(
                        sessionId = sessionId,
                        triggerEvents = recentTriggerEvents,
                        onOpenLog = onOpenLog
                    )

                    else -> TranscriptFeedCard(
                        connectionState = sttConnectionState,
                        interimTranscript = interimTranscript,
                        finalTranscripts = finalTranscripts
                    )
                }
            }
        }
    }
}
