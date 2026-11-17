# Omicron

Omicron 是一个面向 Android 的独立本地 AI 助手项目。

## 目标设备
- iQOO Z9：12 GB RAM / 256 GB 存储
- Android 原生 Kotlin 应用
- 中文聊天界面
- APK 由 GitHub Actions 云端自动构建

## 技术路线
- 本地推理引擎：llama.cpp Android
- 模型候选：Qwen2.5-3B-Instruct GGUF，优先测试 Q4_K_M
- 模型文件由用户通过 Android 文件选择器导入
- 不依赖在线 AI API；完成引擎接入后支持离线推理

## 当前版本 0.2.0
- 启用 Kotlin Android 插件，确保 Activity 编译进 APK。
- 增加 GGUF 文件选择、导入和应用私有目录保存。
- 显示已导入模型的文件名和大小。
- 当前仍未接入 llama.cpp 推理，不能把导入模型误认为已经能聊天。

## 模型建议
Qwen2.5-3B-Instruct GGUF 的 Q4_K_M 文件约 2 GB。模型文件不打包进 APK，需要用户单独下载并导入。首次测试建议从较短上下文开始，避免内存占用过高。

## 获取 APK
打开仓库 Actions 页面，进入最新的 Build Omicron APK 工作流，等待成功后在 Artifacts 下载 `Omicron-debug-apk`。

## 后续阶段
1. 完成模型导入和 APK 编译验证。
2. 集成 llama.cpp Android 推理库。
3. 实现模型加载、上下文长度设置和逐 token 输出。
4. 增加聊天记录与本地设置。
