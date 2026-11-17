# Omicron

Omicron 是一个面向 Android 的独立本地 AI 助手项目。

## 目标设备
- iQOO Z9：12 GB RAM / 256 GB 存储
- Android 原生 Kotlin 应用
- 中文聊天界面
- APK 由 GitHub Actions 云端自动构建

## 技术路线
- 本地推理：llama.cpp Android
- 模型候选：Qwen2.5-3B-Instruct GGUF（后续实测选择量化版本）
- 模型文件由用户下载并导入设备
- 不依赖在线 AI API；完成模型集成后支持离线对话

## 当前版本 0.1.0
已建立 Android 项目骨架、基础聊天界面和自动构建工作流。
当前界面尚未连接模型，发送消息只会显示明确的占位提示，不会伪装成 AI 回答。

## 获取 APK
1. 打开仓库的 Actions 页面。
2. 进入最新的 Build Omicron APK 工作流。
3. 等待任务成功。
4. 在 Artifacts 下载 Omicron-debug-apk。

## 开发阶段
1. 完成基础工程与 APK 云端构建。
2. 集成 llama.cpp Android 推理库。
3. 增加 GGUF 模型导入与文件管理。
4. 接入中文模型并实现真正的离线推理。
5. 增加聊天记录与本地设置。
