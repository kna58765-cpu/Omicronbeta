# Omicron

Omicron 是面向 Android 手机的本地 AI 助手，目标设备为 iQOO Z9（12 GB RAM / 256 GB 存储）。

## 当前版本 0.3.0
- 接入官方 llama.cpp Android 示例中的 JNI 推理库。
- 支持从手机文件选择器导入 GGUF 文件，并复制到应用私有目录。
- 支持加载模型、设置中文系统提示词和流式生成回复。
- 模型推理在设备本地执行，不调用在线 AI API。
- 当前 APK 仅构建 arm64-v8a，适配目标手机。

## 首选模型
Qwen2.5-3B-Instruct GGUF，优先选择 Q4_K_M 量化版本。模型文件不包含在 APK 中，需要单独下载后导入。建议先从 4096 上下文规模的使用预期开始，避免同时运行大型应用造成内存压力。

官方模型仓库：
https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF

## 构建方式
GitHub Actions 云端检出固定版本的官方 llama.cpp 源码，安装 Android NDK 29 与 CMake 3.31.6，构建 Omicron APK。

构建完成后，在 Actions 的 Artifacts 下载 `Omicron-0.3.0-debug-apk`。

## 技术来源
- llama.cpp Android 示例与构建说明：https://github.com/ggml-org/llama.cpp/tree/master/examples/llama.android
- 固定源码提交：7f106b545965d601eb875ec8d2381288ee9b98f9

## 注意
这是首个推理引擎集成版本。云端构建成功后，仍需在真实 iQOO Z9 上验证模型加载、内存占用、发热和生成速度。模型格式、架构或设备内存不足时可能导致加载失败。
