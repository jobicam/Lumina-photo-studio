package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.HomeScreen
import com.example.ui.PhotoEditorScreen
import com.example.ui.SettingsScreen
import com.example.ui.components.StudioMotion
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.LuminaTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.PhotoEditorViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: PhotoEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LuminaTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var visibleStatus by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(uiState.statusMessage) {
                    val msg = uiState.statusMessage
                    if (msg != null) {
                        visibleStatus = msg
                        viewModel.clearStatusMessage()
                        delay(2400L)
                        visibleStatus = null
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = uiState.currentScreen,
                        transitionSpec = {
                            val forward = targetState != AppScreen.HOME
                            val slideIn = slideInHorizontally(
                                animationSpec = StudioMotion.SlideSpring
                            ) { fullWidth -> if (forward) fullWidth / 10 else -fullWidth / 10 }
                            val slideOut = slideOutHorizontally(
                                animationSpec = tween(StudioMotion.FADE_OUT_MS)
                            ) { fullWidth -> if (forward) -fullWidth / 12 else fullWidth / 12 }
                            (fadeIn(tween(220)) + slideIn) togetherWith (fadeOut(tween(StudioMotion.FADE_OUT_MS)) + slideOut)
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "screen_animated_transition"
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

                    StudioStatusBanner(
                        message = visibleStatus,
                        onDismiss = { visibleStatus = null },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(top = 60.dp, start = 20.dp, end = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioStatusBanner(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(180)) + slideInVertically(animationSpec = StudioMotion.SlideSpring) { -it / 2 },
        exit = fadeOut(tween(160)) + slideOutVertically(animationSpec = tween(160)) { -it / 2 },
        modifier = modifier
    ) {
        val text = message ?: ""
        Row(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(StudioSurface.copy(alpha = 0.94f))
                .border(1.dp, AccentCyan.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 14.dp, vertical = 9.dp)
                .testTag("studio_status_banner"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(AccentCyan.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

