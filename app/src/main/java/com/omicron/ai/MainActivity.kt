package com.omicron.ai

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {
    private lateinit var messages: LinearLayout
    private lateinit var input: EditText
    private lateinit var modelStatus: TextView
    private lateinit var importButton: Button
    private lateinit var sendButton: Button
    private lateinit var engine: InferenceEngine
    private var engineReady = false
    private var modelReady = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val prefs by lazy { getSharedPreferences("omicron_settings", MODE_PRIVATE) }
    private val modelDirectory by lazy { File(filesDir, "models").apply { mkdirs() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 16)
            setBackgroundColor(Color.rgb(247, 248, 250))
        }

        root.addView(TextView(this).apply {
            text = "OMICRON"
            textSize = 26f
            setTextColor(Color.rgb(25, 35, 55))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 10, 4, 4)
        })
        root.addView(TextView(this).apply {
            text = "你的私人本地 AI 助手 · 版本 0.3.0"
            textSize = 14f
            setTextColor(Color.rgb(105, 112, 125))
            setPadding(4, 0, 4, 12)
        })

        val modelPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            setBackgroundColor(Color.WHITE)
        }
        modelPanel.addView(TextView(this).apply {
            text = "本地模型引擎 · llama.cpp"
            textSize = 17f
            setTextColor(Color.rgb(35, 45, 60))
        })
        modelStatus = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.rgb(100, 108, 120))
            setPadding(0, 6, 0, 8)
            text = "正在初始化本地推理引擎…"
        }
        modelPanel.addView(modelStatus)
        importButton = Button(this).apply {
            text = "选择 / 导入 GGUF 模型"
            setOnClickListener { chooseModel() }
        }
        modelPanel.addView(importButton)
        root.addView(modelPanel)

        val scroll = ScrollView(this)
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 12, 4, 8)
        }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        addMessage(
            "Omicron",
            "Omicron 0.3.0 正在接入 llama.cpp。本地模型加载成功后即可尝试离线对话。",
            false
        )

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        input = EditText(this).apply {
            hint = "输入消息…"
            textSize = 16f
            setSingleLine(false)
            minLines = 1
            maxLines = 3
            isEnabled = false
        }
        composer.addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        sendButton = Button(this).apply {
            text = "发送"
            isEnabled = false
            setOnClickListener { sendMessage() }
        }
        composer.addView(sendButton)
        root.addView(composer)
        setContentView(root)

        initializeEngine()
    }

    private fun initializeEngine() {
        scope.launch {
            try {
                engine = withContext(Dispatchers.IO) {
                    AiChat.getInferenceEngine(applicationContext)
                }
                val state = withContext(Dispatchers.IO) {
                    engine.state.first {
                        it is InferenceEngine.State.Initialized ||
                            it is InferenceEngine.State.Error
                    }
                }
                if (state is InferenceEngine.State.Error) throw state.exception
                engineReady = true
                modelStatus.text = "llama.cpp 已初始化。请选择 Qwen2.5-3B-Instruct GGUF 模型。"
                val savedPath = prefs.getString("model_path", null)
                if (savedPath != null && File(savedPath).exists()) {
                    loadModel(savedPath)
                }
            } catch (error: Exception) {
                modelStatus.text = "推理引擎初始化失败：${error.message ?: "未知错误"}"
                addMessage("Omicron", "本地推理引擎初始化失败，请检查构建日志。", false)
            }
        }
    }

    private fun chooseModel() {
        if (!engineReady) {
            addMessage("Omicron", "本地推理引擎仍在初始化，请稍候。", false)
            return
        }
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, REQUEST_MODEL)
    }

    @Deprecated("Using the document picker keeps compatibility with older Android versions.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_MODEL || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val displayName = queryDisplayName(uri) ?: "model.gguf"
        if (!displayName.lowercase().endsWith(".gguf")) {
            addMessage("Omicron", "请选择 .gguf 格式的模型文件。", false)
            return
        }

        importButton.isEnabled = false
        modelReady = false
        modelStatus.text = "正在复制 $displayName 到应用私有存储…"

        scope.launch {
            try {
                val target = withContext(Dispatchers.IO) {
                    val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                    val file = File(modelDirectory, safeName)
                    contentResolver.openInputStream(uri).use { source ->
                        requireNotNull(source) { "无法读取所选文件" }
                        FileOutputStream(file).use { output ->
                            source.copyTo(output, 1024 * 1024)
                            output.fd.sync()
                        }
                    }
                    file
                }
                prefs.edit().putString("model_path", target.absolutePath).apply()
                loadModel(target.absolutePath)
            } catch (error: Exception) {
                modelStatus.text = "模型导入 / 加载失败：${error.message ?: "未知错误"}"
                addMessage("Omicron", "模型没有加载成功。请确认文件完整且为受支持的 GGUF 模型。", false)
                importButton.isEnabled = true
            }
        }
    }

    private suspend fun loadModel(path: String) {
        modelReady = false
        input.isEnabled = false
        sendButton.isEnabled = false
        importButton.isEnabled = false
        modelStatus.text = "正在加载模型到内存，请稍候…"
        withContext(Dispatchers.IO) {
            engine.loadModel(path)
            engine.setSystemPrompt(
                "你是 Omicron，一个运行在用户手机本地的中文 AI 助手。请用清晰、准确、自然的中文回答。"
            )
        }
        modelReady = true
        input.isEnabled = true
        sendButton.isEnabled = true
        importButton.isEnabled = true
        modelStatus.text = "模型已加载：${File(path).name}\n离线推理已就绪。"
        addMessage("Omicron", "模型加载成功！现在可以尝试关闭网络后与我聊天。", false)
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return
        if (!modelReady) {
            addMessage("Omicron", "请先导入并加载 GGUF 模型。", false)
            return
        }

        input.text.clear()
        input.isEnabled = false
        sendButton.isEnabled = false
        addMessage("你", message, true)
        val responseView = addMessage("Omicron", "", false)

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    engine.sendUserPrompt(message, 512).collect { token ->
                        withContext(Dispatchers.Main) {
                            responseView.append(token)
                        }
                    }
                }
            } catch (error: Exception) {
                responseView.text = "生成失败：${error.message ?: "未知错误"}"
            } finally {
                input.isEnabled = true
                sendButton.isEnabled = true
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else null
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    private fun addMessage(sender: String, body: String, isUser: Boolean): TextView {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(if (isUser) Color.rgb(225, 238, 255) else Color.WHITE)
        }
        card.addView(TextView(this).apply {
            text = sender
            textSize = 12f
            setTextColor(Color.rgb(95, 105, 120))
        })
        val content = TextView(this).apply {
            text = body
            textSize = 15f
            setTextColor(Color.rgb(30, 35, 45))
            setPadding(0, 5, 0, 0)
        }
        card.addView(content)
        messages.addView(card, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, 12) })
        return content
    }

    override fun onDestroy() {
        scope.cancel()
        if (::engine.isInitialized) {
            Thread { runCatching { engine.destroy() } }.start()
        }
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_MODEL = 2101
    }
}
