#!/bin/bash
set -e

TAG="${1:-v1.0.0}"
TITLE="${2:-Voice2Txt 正式发布版 $TAG}"
TOKEN="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"
REPO="l1Ha/voice2txt"

echo "=== 正在调用 GitHub API 为 $REPO 创建 Release: $TAG ==="

RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer $TOKEN" \
  https://api.github.com/repos/$REPO/releases \
  -d "{
    \"tag_name\": \"$TAG\",
    \"name\": \"$TITLE\",
    \"body\": \"Voice2Txt Android 本地离线音视频语音转文字应用首发版本。包含 Sherpa-ONNX 端侧模型支持、双版本文本对照、音频同步回放以及多格式导出。\",
    \"draft\": false,
    \"prerelease\": false,
    \"generate_release_notes\": true
  }")

HTTP_STATUS=$(echo "$RESPONSE" | grep "HTTP_STATUS" | cut -d':' -f2)
BODY=$(echo "$RESPONSE" | grep -v "HTTP_STATUS")

if [ "$HTTP_STATUS" -eq 201 ]; then
    echo "=== Release 创建成功！ ==="
    echo "GitHub Actions 将自动触发打包并将编译好的 APK 上传至 Release Assets。"
    echo "访问页面下载安装包：https://github.com/$REPO/releases/tag/$TAG"
else
    echo "创建 Release 返回状态码: $HTTP_STATUS"
    echo "$BODY"
fi
