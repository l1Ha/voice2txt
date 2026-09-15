package com.voice2txt.app.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.voice2txt.app.domain.model.ExportFormat
import com.voice2txt.app.domain.model.ExportTarget
import com.voice2txt.app.domain.model.TranscriptionResult
import com.voice2txt.app.ui.components.ComparisonView
import com.voice2txt.app.ui.theme.TextPrimary
import com.voice2txt.app.ui.theme.TextSecondary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultComparisonScreen(
    result: TranscriptionResult,
    onBack: () -> Unit,
    onPlayAudioSegment: (Long, Long) -> Unit,
    onExport: (ExportFormat, ExportTarget) -> File,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showExportSheet by remember { mutableStateOf(false) }
    var selectedTarget by remember { mutableStateOf(ExportTarget.BOTH_SIDE_BY_SIDE) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.MARKDOWN) }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = result.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "时长: ${result.durationMs / 1000}s · 模型: ${result.modelName}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportSheet = true }) {
                        Icon(Icons.Default.Download, contentDescription = "导出保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        ComparisonView(
            result = result,
            onPlayAudioSegment = onPlayAudioSegment,
            modifier = modifier.padding(innerPadding)
        )

        // Export Bottom Sheet
        if (showExportSheet) {
            ModalBottomSheet(
                onDismissRequest = { showExportSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "选择导出版本与格式",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Target text selector
                    Text(
                        text = "选择文本版本：",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    listOf(
                        ExportTarget.BOTH_SIDE_BY_SIDE to "双版本对照 (推荐，包含润色与原始对照)",
                        ExportTarget.POLISHED to "仅智能润色版 (已去除口语语气词与重复词)",
                        ExportTarget.RAW to "仅原始转录版 (包含原始字句)"
                    ).forEach { (target, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedTarget == target,
                                    onClick = { selectedTarget = target }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTarget == target,
                                onClick = { selectedTarget = target }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Format selector
                    Text(
                        text = "选择导出格式：",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    listOf(
                        ExportFormat.MARKDOWN to "Markdown 文档 (.md)",
                        ExportFormat.TXT to "纯文本 (.txt)",
                        ExportFormat.SRT to "SRT 视频字幕 (.srt)"
                    ).forEach { (format, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedFormat == format,
                                    onClick = { selectedFormat = format }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedFormat == format,
                                onClick = { selectedFormat = format }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val exportedFile = onExport(selectedFormat, selectedTarget)
                                Toast.makeText(
                                    context,
                                    "已保存至：${exportedFile.name}",
                                    Toast.LENGTH_LONG
                                ).show()
                                showExportSheet = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("保存到手机")
                        }

                        Button(
                            onClick = {
                                val exportedFile = onExport(selectedFormat, selectedTarget)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    exportedFile
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = selectedFormat.mimeType
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "分享转录文件"))
                                showExportSheet = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("分享发送")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
