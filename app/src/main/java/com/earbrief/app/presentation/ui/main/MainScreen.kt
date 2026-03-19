package com.earbrief.app.presentation.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.earbrief.app.engine.stt.SttConnectionState
import com.earbrief.app.presentation.viewmodel.MainViewModel

@Composable
fun MainScreen(
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        ListeningStatusCard(
            listeningState = listeningState,
            vadState = vadState,
            speechProbability = speechProbability,
            sessionDuration = viewModel.getSessionDurationFormatted(),
            onToggle = viewModel::toggleListening,
            onStop = viewModel::stopListening
        )

        Spacer(modifier = Modifier.height(24.dp))

        QuickStatsCard(
            whisperCount = whisperCount,
            sessionDuration = viewModel.getSessionDurationFormatted()
        )

        Spacer(modifier = Modifier.height(24.dp))

        RecentWhispersCard(
            sessionId = sessionId,
            triggerEvents = recentTriggerEvents
        )

        Spacer(modifier = Modifier.height(24.dp))

        TranscriptFeedCard(
            connectionState = sttConnectionState,
            interimTranscript = interimTranscript,
            finalTranscripts = finalTranscripts
        )
    }
}
