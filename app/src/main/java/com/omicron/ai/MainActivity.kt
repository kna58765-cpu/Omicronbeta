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
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {
    private lateinit var messages: LinearLayout
    private lateinit var input: EditText
    private lateinit var modelStatus: TextView
    private lateinit var importButton: Button
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
            text = "你的私人本地 AI 助手 · 版本 0.2.0"
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
            text = "本地模型"
            textSize = 17f
            setTextColor(Color.rgb(35, 45, 60))
        })
        modelStatus = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.rgb(100, 108, 120))
            setPadding(0, 6, 0, 8)
        }
        modelPanel.addView(modelStatus)
        importButton = Button(this).apply {
            text = "导入 GGUF 模型"
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
            "欢迎回来！现在可以导入 GGUF 模型文件并保存在手机本地。推理引擎正在接入中，模型导入成功不代表已经可以开始对话。",
            false
        )

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        input = EditText(this).apply {
            hint = "输入消息…"
            textSize = 16f
            singleLine = true
        }
        composer.addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        composer.addView(Button(this).apply {
            text = "发送"
            setOnClickListener {
                val message = input.text.toString().trim()
                if (message.isNotEmpty()) {
                    addMessage("你", message, true)
                    input.text.clear()
                    addMessage(
                        "Omicron",
                        "我已收到消息，但本版本尚未启用模型推理。请先完成 llama.cpp 引擎接入。",
                        false
                    )
                }
            }
        })
        root.addView(composer)
        setContentView(root)
        refreshModelStatus()
    }

    private fun chooseModel() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream", "application/x-gguf"))
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
        modelStatus.text = "正在导入 $displayName，请保持应用打开…"

        Thread {
            try {
                val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val target = File(modelDirectory, safeName)
                contentResolver.openInputStream(uri).use { inputStream ->
                    requireNotNull(inputStream) { "无法读取所选文件" }
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(1024 * 1024)
                        while (true) {
                            val count = inputStream.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                        output.fd.sync()
                    }
                }
                prefs.edit().putString("model_path", target.absolutePath).apply()
                runOnUiThread {
                    importButton.isEnabled = true
                    refreshModelStatus()
                    addMessage(
                        "Omicron",
                        "模型文件已导入手机本地：$safeName。下一步将让 llama.cpp 读取它并执行推理。",
                        false
                    )
                }
            } catch (error: Exception) {
                runOnUiThread {
                    importButton.isEnabled = true
                    modelStatus.text = "导入失败：${error.message ?: "未知错误"}"
                }
            }
        }.start()
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

    private fun refreshModelStatus() {
        val path = prefs.getString("model_path", null)
        val file = path?.let { File(it) }
        modelStatus.text = if (file != null && file.exists()) {
            "已导入：${file.name}\n文件大小：${formatBytes(file.length())}\n状态：已保存，等待推理引擎接入"
        } else {
            "尚未导入模型。建议使用 Qwen2.5-3B-Instruct 的 GGUF Q4_K_M 量化文件（约 2 GB）。"
        }
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) String.format("%.2f GB", gb)
        else String.format("%.0f MB", bytes / (1024.0 * 1024.0))
    }

    private fun addMessage(sender: String, body: String, isUser: Boolean) {
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
        card.addView(TextView(this).apply {
            text = body
            textSize = 15f
            setTextColor(Color.rgb(30, 35, 45))
            setPadding(0, 5, 0, 0)
        })
        messages.addView(card, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, 12) })
    }

    companion object {
        private const val REQUEST_MODEL = 2101
    }
}
