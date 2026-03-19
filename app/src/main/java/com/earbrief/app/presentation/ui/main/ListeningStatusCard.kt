package com.earbrief.app.presentation.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.earbrief.app.domain.model.ListeningState
import com.earbrief.app.domain.model.VadState
import com.earbrief.app.presentation.theme.EarBriefBlue
import com.earbrief.app.presentation.theme.SuccessGreen

@Composable
fun ListeningStatusCard(
    listeningState: ListeningState,
    vadState: VadState,
    speechProbability: Float,
    sessionDuration: String,
    onToggle: () -> Unit,
    onStop: () -> Unit
) {
    val isListening = listeningState == ListeningState.LISTENING
    val isPaused = listeningState == ListeningState.PAUSED

    val buttonColor by animateColorAsState(
        targetValue = when {
            isListening -> EarBriefBlue
            isPaused -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(300),
        label = "buttonColor"
    )

    val pulseScale by animateFloatAsState(
        targetValue = if (isListening && vadState == VadState.SPEECH) 1.08f else 1f,
        animationSpec = tween(200),
        label = "pulse"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(buttonColor)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isListening -> Icons.Default.Headphones
                        isPaused -> Icons.Default.PlayArrow
                        else -> Icons.Default.Headphones
                    },
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (listeningState) {
                    ListeningState.LISTENING -> "듣는 중"
                    ListeningState.PAUSED -> "일시정지"
                    ListeningState.IDLE -> "꺼져 있어요"
                    ListeningState.STOPPING -> "종료 중..."
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (isListening || isPaused) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = sessionDuration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isListening) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (vadState == VadState.SPEECH) SuccessGreen
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (vadState == VadState.SPEECH) "음성 감지 중" else "대기 중",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isListening || isPaused) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (isListening) {
                        IconButton(onClick = onToggle) {
                            Icon(Icons.Default.Pause, contentDescription = "일시정지")
                        }
                    } else {
                        IconButton(onClick = onToggle) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "재개")
                        }
                    }
                    IconButton(onClick = onStop) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "종료",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
