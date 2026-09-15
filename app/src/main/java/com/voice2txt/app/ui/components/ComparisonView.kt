package com.voice2txt.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voice2txt.app.domain.model.TranscriptionResult
import com.voice2txt.app.ui.theme.TextPrimary
import com.voice2txt.app.ui.theme.TextSecondary

@Composable
fun ComparisonView(
    result: TranscriptionResult,
    onPlayAudioSegment: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDiffHighlights by remember { mutableStateOf(true) }
    val clipboardManager = LocalClipboardManager.current

    val tabs = listOf("智能润色", "原始转录", "逐句对照")

    Column(modifier = modifier.fillMaxSize()) {
        // Tab row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.AutoAwesome
                                    1 -> Icons.Default.TextFields
                                    else -> Icons.Default.Compare
                                },
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(text = title, fontWeight = FontWeight.Medium)
                        }
                    }
                )
            }
        }

        // Action & Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedTab == 2) {
                FilterChip(
                    selected = showDiffHighlights,
                    onClick = { showDiffHighlights = !showDiffHighlights },
                    label = { Text("标记口语语气词", fontSize = 12.sp) }
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            OutlinedButton(
                onClick = {
                    val textToCopy = when (selectedTab) {
                        0 -> result.polishedContent
                        1 -> result.rawContent
                        else -> "${result.polishedContent}\n\n---\n原始转录：\n${result.rawContent}"
                    }
                    clipboardManager.setText(AnnotatedString(textToCopy))
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(
                    text = when (selectedTab) {
                        0 -> "复制润色版"
                        1 -> "复制原始版"
                        else -> "复制完整对照"
                    },
                    fontSize = 13.sp
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Polished full document
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = result.polishedContent.ifBlank { "（无文本）" },
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            color = TextPrimary
                        )
                    }
                }
                1 -> {
                    // Raw full document
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = result.rawContent.ifBlank { "（无文本）" },
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            color = TextSecondary
                        )
                    }
                }
                2 -> {
                    // Sentence-by-sentence comparison list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(result.segments) { segment ->
                            SegmentItemView(
                                segment = segment,
                                onPlayAudio = onPlayAudioSegment,
                                showDiff = showDiffHighlights
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }
}
