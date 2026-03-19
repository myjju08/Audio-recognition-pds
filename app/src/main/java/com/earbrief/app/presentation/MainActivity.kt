package com.earbrief.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.earbrief.app.presentation.navigation.EarBriefNavGraph
import com.earbrief.app.presentation.theme.EarBriefTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EarBriefTheme {
                EarBriefNavGraph()
            }
        }
    }
}
