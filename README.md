# ikun新媒体制作中心

AI 图像/视频生成工作台（Capacitor 安卓壳）。五平台：OpenRouter / CherryIN / AIHubMix / 随想 / AkileAI。

## 本地构建

```bash
npm ci
npx cap sync android
cd android
KS_PATH=/workspace/ikun-release.keystore KS_PASS=ikun2026 KS_ALIAS=ikun ./gradlew assembleRelease
# 产物：android/app/build/outputs/apk/release/app-release.apk
```

## GitHub Actions

推送即自动构建（或 Actions 页面手动 Run workflow）。出正式签名包需配置仓库 Secrets：

- `KS_BASE64`：keystore 文件的 base64（`base64 -w0 ikun-release.keystore`）
- `KS_PASS`：签名密码（`ikun2026`）

未配置时自动降级 debug 签名（可安装，但升级需固定同一签名）。
