package com.omicron.ai

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var messages: LinearLayout
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 16)
            setBackgroundColor(Color.rgb(247, 248, 250))
        }
        root.addView(TextView(this).apply {
            text = "OMICRON"
            textSize = 25f
            setTextColor(Color.rgb(25, 35, 55))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 12, 4, 18)
        })
        root.addView(TextView(this).apply {
            text = "你的私人本地 AI 助手 · 版本 0.1.0"
            textSize = 14f
            setTextColor(Color.rgb(105, 112, 125))
            setPadding(4, 0, 4, 18)
        })
        val scroll = ScrollView(this)
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 8, 4, 8)
        }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        addMessage("Omicron", "你好！Omicron 基础界面已启动。下一阶段将接入本地模型。目前尚未启用 AI 推理。", false)

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
                    addMessage("Omicron", "已收到消息。当前构建尚未接入 AI 模型，请等待本地推理引擎集成。", false)
                }
            }
        })
        root.addView(composer)
        setContentView(root)
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
            textSize = 16f
            setTextColor(Color.rgb(30, 35, 45))
            setPadding(0, 5, 0, 0)
        })
        messages.addView(card, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, 12) })
    }
}
