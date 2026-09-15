package com.voice2txt.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.voice2txt.app.domain.model.MediaType
import com.voice2txt.app.domain.model.TranscriptionResult
import com.voice2txt.app.ui.screens.HistoryScreen
import com.voice2txt.app.ui.screens.HomeScreen
import com.voice2txt.app.ui.screens.ResultComparisonScreen
import com.voice2txt.app.ui.screens.SettingsScreen
import com.voice2txt.app.ui.screens.TranscribeScreen
import com.voice2txt.app.ui.theme.Voice2TxtTheme
import com.voice2txt.app.ui.viewmodel.HistoryViewModel
import com.voice2txt.app.ui.viewmodel.SettingsViewModel
import com.voice2txt.app.ui.viewmodel.TranscribeUiState
import com.voice2txt.app.ui.viewmodel.TranscribeViewModel

enum class MainTab(val title: String) {
    TRANSCRIBE("转写"),
    HISTORY("历史"),
    SETTINGS("设置")
}

class MainActivity : ComponentActivity() {

    private val transcribeViewModel: TranscribeViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()
        handleIncomingIntent(intent)

        setContent {
            Voice2TxtTheme {
                MainAppContent(
                    transcribeViewModel = transcribeViewModel,
                    historyViewModel = historyViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun handleIncomingIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            val type = intent.type ?: ""
            if (uri != null) {
                val mediaType = if (type.startsWith("video/")) MediaType.VIDEO else MediaType.AUDIO
                transcribeViewModel.transcribeMediaFile(uri, mediaType, "外部导入转录_${System.currentTimeMillis() / 1000}")
            }
        }
    }
}

@Composable
fun MainAppContent(
    transcribeViewModel: TranscribeViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel
) {
    var currentTab by remember { mutableStateOf(MainTab.TRANSCRIBE) }
    var viewingResult by remember { mutableStateOf<TranscriptionResult?>(null) }

    val transcribeState by transcribeViewModel.uiState.collectAsState()
    val isRecording by transcribeViewModel.isRecording.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (transcribeState !is TranscribeUiState.Processing && viewingResult == null) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == MainTab.TRANSCRIBE,
                        onClick = { currentTab = MainTab.TRANSCRIBE },
                        icon = { Icon(Icons.Default.Mic, contentDescription = null) },
                        label = { Text(MainTab.TRANSCRIBE.title) }
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.HISTORY,
                        onClick = { currentTab = MainTab.HISTORY },
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        label = { Text(MainTab.HISTORY.title) }
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.SETTINGS,
                        onClick = { currentTab = MainTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(MainTab.SETTINGS.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // If viewing an inspection result (either just transcribed or opened from history)
        if (viewingResult != null) {
            ResultComparisonScreen(
                result = viewingResult!!,
                onBack = { viewingResult = null },
                onPlayAudioSegment = { startMs, endMs ->
                    transcribeViewModel.audioPlayer.playSegment(startMs, endMs)
                },
                onExport = { format, target ->
                    transcribeViewModel.exportResult(viewingResult!!, format, target)
                },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            when (val state = transcribeState) {
                is TranscribeUiState.Processing -> {
                    TranscribeScreen(
                        stageText = state.stage,
                        progress = state.progress,
                        onCancel = { transcribeViewModel.resetState() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                is TranscribeUiState.Success -> {
                    // Automatically transition to comparison screen
                    viewingResult = state.result
                    transcribeViewModel.resetState()
                }
                else -> {
                    when (currentTab) {
                        MainTab.TRANSCRIBE -> {
                            HomeScreen(
                                onMediaSelected = { uri, mediaType, title ->
                                    transcribeViewModel.transcribeMediaFile(uri, mediaType, title)
                                },
                                onStartRecording = {
                                    if (!isRecording) transcribeViewModel.startLiveRecording()
                                    else transcribeViewModel.stopLiveRecording()
                                },
                                onNavigateToSettings = { currentTab = MainTab.SETTINGS },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        MainTab.HISTORY -> {
                            HistoryScreen(
                                viewModel = historyViewModel,
                                onSelectResult = { viewingResult = it },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        MainTab.SETTINGS -> {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
