package com.earbrief.app.presentation.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.earbrief.app.engine.stt.SttConnectionState

@Composable
fun TranscriptFeedCard(
    connectionState: SttConnectionState,
    interimTranscript: String,
    finalTranscripts: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "실시간 전사",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = when (connectionState) {
                    SttConnectionState.CONNECTED -> "Deepgram 연결됨"
                    SttConnectionState.CONNECTING -> "Deepgram 연결 중..."
                    SttConnectionState.RECONNECTING -> "Deepgram 재연결 중..."
                    SttConnectionState.FAILED -> "Deepgram 연결 실패"
                    SttConnectionState.DISCONNECTED -> "Deepgram 연결 대기"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
            )

            if (finalTranscripts.isEmpty() && interimTranscript.isBlank()) {
                Text(
                    text = "청취 중인 대화가 여기에 실시간으로 표시됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                finalTranscripts.takeLast(6).forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (interimTranscript.isNotBlank()) {
                    Text(
                        text = interimTranscript,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
