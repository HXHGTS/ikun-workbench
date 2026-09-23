# ikun新媒体制作中心

AI 图像/视频生成工作台（Capacitor 安卓壳）。五平台：OpenRouter / CherryIN / AIHubMix / 随想 / AkileAI。

## 📥 下载最新 APK（推荐）

> **[⬇ 点击下载最新版 APK](https://github.com/HXHGTS/ikun-workbench/releases/latest/download/app-release.apk)**

该链接永远指向最新 Release 的正式签名包，升级无需卸载旧版。
历史版本见 [Releases 页面](https://github.com/HXHGTS/ikun-workbench/releases)。

## 功能

- 五平台图像/视频生成（OpenRouter / CherryIN / AIHubMix / 随想 / AkileAI，全部按官方接口实测适配）
- Key 与模型选择本地持久化、余额/花费展示、生成历史（IndexedDB）
- 生成期间前台服务 + 常驻进度通知（防杀后台），通知优先级可调，成功/失败结果通知
- 原生 MediaStore 保存（图片进相册 Pictures/ikun/，视频进 Movies/ikun/）
- 磨砂玻璃 UI · 古见同学壁纸

## 构建

### GitHub Actions（推荐）
推送即自动构建并发布 Release（需仓库 Secrets：`KS_BASE64` + `KS_PASS`）。

### 本地构建

```bash
npm ci
npx cap sync android
cd android
KS_PATH=/workspace/ikun-release.keystore KS_PASS=ikun2026 KS_ALIAS=ikun \
  ./gradlew -Pandroid.aapt2FromMavenOverride=/opt/bin/aapt2 assembleRelease
# 产物：android/app/build/outputs/apk/release/app-release.apk
```

未配置 keystore 时自动降级 debug 签名。
