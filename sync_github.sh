#!/bin/bash
set -e

REPO_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$REPO_DIR"

echo "=== 同步 Voice2Txt 至 GitHub (https://github.com/l1Ha/voice2txt) ==="
git add .
if git diff --staged --quiet; then
    echo "没有需要提交的代码变更。"
else
    git commit -m "feat: updates and enhancements for Voice2Txt $(date +'%Y-%m-%d %H:%M')"
fi

echo "正在推送至 GitHub main 分支..."
git push origin main
echo "=== 推送成功！GitHub Actions 已开始自动化构建 ==="
