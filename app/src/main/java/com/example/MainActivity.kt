package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.HomeScreen
import com.example.ui.PhotoEditorScreen
import com.example.ui.SettingsScreen
import com.example.ui.theme.LuminaTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.PhotoEditorViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: PhotoEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LuminaTheme {
                val uiState by viewModel.uiState.collectAsState()
                Crossfade(
                    targetState = uiState.currentScreen,
                    modifier = Modifier.fillMaxSize(),
                    label = "screen_crossfade"
                ) { screen ->
                    when (screen) {
                        AppScreen.HOME -> HomeScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                        AppScreen.EDITOR -> PhotoEditorScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                        AppScreen.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

