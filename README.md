# Voice2Txt (声文智转) 🎙️📱

> **完全离线、隐私安全的 Android 本地音视频语音转文字应用**  
> 内置 Sherpa-ONNX (SenseVoice / Whisper) 端侧模型，支持原始转录与智能润色双版本对照、分句音频同步试听、多格式（TXT / Markdown / SRT 字幕）自由保存与导出。

---

## 🌟 核心功能特性

### 1. 100% 本地离线语音识别 (Edge ASR)
- **绝无数据外泄**：所有语音和视频文件的转录完全在手机端侧 NPU/CPU 离线计算，不上传任何云端服务器，断网状态下也能流畅运行，彻底保护商业机密与个人隐私。
- **先进模型架构**：采用阿里巴巴开源的 **SenseVoice-Small** 与 OpenAI **Whisper**，支持普通话、英语、粤语、日语、韩语等，识别速度最高可达实时音频的 5~10 倍，低功耗低发热。

### 2. 音视频全格式音轨抽离
- 基于 Android 原生硬解 `MediaExtractor` 与 `MediaCodec` 管道，自动提取并重采样为 16kHz 16-bit 单声道 PCM：
  - **视频格式**：MP4, MKV, MOV, 3GP, AVI 等。
  - **音频格式**：MP3, M4A, AAC, WAV, FLAC, OGG 等。
  - **实时速记**：支持直接通过手机麦克风录音，会议、演讲现场边录边记。

### 3. 双版本对照（原始转录 vs 智能润色）
- **原始转录版 (Raw)**：忠实还原 ASR 输出的原始字词与分句时间戳，便于核对原音细节。
- **智能润色版 (Polished)**：内置端侧自然语言清洗规则：
  - **口语冗余词过滤**：自动清除「那个、然后、就是、就是说、呃、啊、嗯、其实、基本上来说、you know、um、uh」等口头禅与填充词。
  - **结巴与重复词去重**：自动合并「我我我」、「这个这个」等连词卡顿。
  - **数字与单位规范化**：将口语数字转化为标准阿拉伯数字（如「二零二六年」转化为「2026年」，「百分之八十」转化为「80%」）。
  - **智能标点与句末问号修复**：根据语意助词（如「吗、呢、对不对」）自适应修复问号，补齐标点。
  - **自适应段落规整**：根据停顿与语段自动划分段落，大幅改善长篇转录的可读性。
- **逐句双版本对照视图**：提供「润色后」、「原始转录」、「逐句对比」三重视图，支持差异标记高亮，点击任意分句即可**即时播放对应时间段的音频切片**。

### 4. 灵活选择与多格式导出
- **自由选择版本**：可自由勾选「仅保存智能润色版」、「仅保存原始转录版」或「保存双版本对照」。
- **主流格式全覆盖**：
  - **Markdown (.md)**：带有结构化标题、元数据及双版本对比表格，适合导入 Notion、Obsidian 等知识库。
  - **TXT (.txt)**：纯文本无格式文档。
  - **SRT (.srt)**：带精确起止时间轴的标准字幕文件，可直接拖入剪映、PR、Final Cut Pro、Bilibili 中作为视频字幕。
  - **系统分享**：一键调用系统分享面板，直接发送到微信、QQ、网盘或邮件。

### 5. 历史记录与全文检索
- 基于 Jetpack Room 构建本地安全数据库，保留所有转写记录，支持秒级全文关键词搜索。

---

## 🚀 手机下载与安装 (从 GitHub Releases 获取 APK)

本项目已配置完整的 **GitHub Actions 自动化 CI/CD**（见 `.github/workflows/build-release.yml`）。

### 📱 方式一：直接在手机浏览器下载安装（最简便）
1. 在手机浏览器打开你的 GitHub 仓库地址。
2. 进入仓库右侧的 **Releases**（发行版）页面。
3. 找到最新版本，在 **Assets** 列表中点击下载 `voice2txt-release.apk`。
4. 下载完成后点击安装包，根据系统提示允许安装即可在手机上立即体验！

> **说明**：GitHub Actions 构建流程中已配置针对 Release APK 的自动签名，安装时无需手动配置签名密钥。

### 💻 方式二：本地自行编译 APK
如果你希望在电脑本地通过 Android Studio 编译：
```bash
# 克隆仓库
git clone <你的GitHub仓库链接>
cd voice2txt

# 赋予 gradlew 执行权限
chmod +x gradlew

# 编译 Debug APK
./gradlew assembleDebug

# 编译 Release APK
./gradlew assembleRelease
```
编译产物位于 `app/build/outputs/apk/debug/app-debug.apk` 与 `app/build/outputs/apk/release/app-release.apk`。通过数据线执行 `adb install app/build/outputs/apk/debug/app-debug.apk` 即可安装到连接的手机上。

---

## 🛠️ 技术架构

- **编程语言**：Kotlin 2.0+
- **界面框架**：Jetpack Compose (Material 3)
- **多媒体处理**：AndroidX Media3 (MediaExtractor, MediaCodec, ExoPlayer)
- **离线语音识别**：Sherpa-ONNX (v1.10.36 JNI)
- **数据库**：Android Jetpack Room (SQLite)
- **异步处理**：Kotlin Coroutines & Flow
- **架构模式**：Clean Architecture + MVVM

---

## 📁 目录结构

```
voice2txt/
├── .github/
│   └── workflows/
│       └── build-release.yml          # GitHub Actions 自动构建与发布 APK 工作流
├── app/
│   ├── build.gradle.kts               # 应用模块构建脚本与依赖
│   ├── proguard-rules.pro             # 代码混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml        # 清单文件与多媒体权限
│       ├── java/com/voice2txt/app/
│       │   ├── Voice2TxtApplication.kt
│       │   ├── data/                  # Room 数据库与数据持久化
│       │   ├── domain/
│       │   │   ├── model/             # 领域实体（片段、结果、配置）
│       │   │   └── polisher/          # 文本清洗、语气词剔除、数字规范化、智能标点
│       │   ├── audio/                 # MediaExtractor 提取、录音器与 PCM 播放器
│       │   ├── asr/                   # Sherpa-ONNX 离线识别引擎与模型管理
│       │   └── ui/                    # Jetpack Compose UI (双版本对照、播放、历史)
│       └── res/                       # 字符串、色彩、矢量图标与主题
├── gradle/
│   └── libs.versions.toml             # 统一依赖版本控制
├── build.gradle.kts                   # 根项目构建脚本
├── settings.gradle.kts                # 仓库配置
└── README.md
```

---

## 📄 开源许可证

本项目基于 [Apache-2.0 License](LICENSE) 开源。
