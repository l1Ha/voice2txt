package com.voice2txt.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voice2txt.app.domain.polisher.CustomHotwordsReplacer
import com.voice2txt.app.ui.theme.Blue40
import com.voice2txt.app.ui.theme.Emerald40
import com.voice2txt.app.ui.theme.TextPrimary
import com.voice2txt.app.ui.theme.TextSecondary
import com.voice2txt.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val models by viewModel.models.collectAsState()
    val downloadingId by viewModel.downloadingModelId.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val polishOptions by viewModel.polishOptions.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "设置与功能配置",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section 1: Offline Speech-to-Text Models
        Text(
            text = "离线语音识别模型",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "完全在手机端侧 NPU/CPU 离线运行，0 联网请求，保障隐私安全",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        models.forEach { model ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "语种：${model.language} · 体积：约 ${model.expectedSizeMB} MB",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        if (model.isDownloaded) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Emerald40)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("已就绪", color = Emerald40, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        } else if (downloadingId == model.id) {
                            Text("下载中 ${(downloadProgress * 100).toInt()}%", fontSize = 13.sp, color = Blue40)
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.downloadModel(model) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                Text("下载", fontSize = 13.sp)
                            }
                        }
                    }

                    if (downloadingId == model.id) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = model.description,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: Polishing Rules
        Text(
            text = "智能文本润色与清洗规则",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "自适应去除语气冗余词、口语重复、修复标点与段落",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                RuleSwitchRow(
                    title = "去除口语语气词与冗余词",
                    subtitle = "自动过滤「那个、就是、然后、呃、啊、嗯」等口头禅",
                    checked = polishOptions.removeFillers,
                    onCheckedChange = { viewModel.updatePolishOptions(polishOptions.copy(removeFillers = it)) }
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                RuleSwitchRow(
                    title = "消除结巴与重复词",
                    subtitle = "合并连续重复字词，如「我我我」合并为「我」",
                    checked = polishOptions.removeStuttering,
                    onCheckedChange = { viewModel.updatePolishOptions(polishOptions.copy(removeStuttering = it)) }
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                RuleSwitchRow(
                    title = "规范化数字与年份",
                    subtitle = "将「二零二六年、百分之八十」转为「2026年、80%」",
                    checked = polishOptions.normalizeNumbers,
                    onCheckedChange = { viewModel.updatePolishOptions(polishOptions.copy(normalizeNumbers = it)) }
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                RuleSwitchRow(
                    title = "智能标点与句末问号补全",
                    subtitle = "根据语义停顿与疑问助词修复句逗并补齐标点",
                    checked = polishOptions.restorePunctuation,
                    onCheckedChange = { viewModel.updatePolishOptions(polishOptions.copy(restorePunctuation = it)) }
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                RuleSwitchRow(
                    title = "自动对话角色/发言人识别",
                    subtitle = "长停顿自适应识别并标记「发言人 A / 发言人 B」",
                    checked = polishOptions.autoSpeakerDiarization,
                    onCheckedChange = { viewModel.updatePolishOptions(polishOptions.copy(autoSpeakerDiarization = it)) }
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                RuleSwitchRow(
                    title = "自动段落规整与换行",
                    subtitle = "每2-3句根据语意自适应分段，提升长文阅读体验",
                    checked = polishOptions.autoParagraph,
                    onCheckedChange = { viewModel.updatePolishOptions(polishOptions.copy(autoParagraph = it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 3: Hotwords / Terminology Corrections
        Text(
            text = "专业术语与专有名词纠错",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "内置常见同音词及技术术语映射纠错",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                CustomHotwordsReplacer.DEFAULT_RULES.take(6).forEachIndexed { index, rule ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "「${rule.pattern}」", fontSize = 13.sp, color = TextSecondary)
                        Text(text = "➔", fontSize = 13.sp, color = TextSecondary)
                        Text(text = "「${rule.replacement}」", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Blue40)
                    }
                    if (index < 5) Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RuleSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = subtitle, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
