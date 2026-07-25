package com.example.desktoppet

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * 宠物设置界面
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var sizeSeekBar: SeekBar
    private lateinit var sizeValueText: TextView
    private lateinit var previewText: TextView
    private lateinit var applyButton: Button
    private lateinit var presetButtonsContainer: LinearLayout

    private var currentSizeDp = PetSettings.DEFAULT_SIZE_DP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        title = "宠物设置"

        initViews()
        loadCurrentSettings()
        setupListeners()
    }

    private fun initViews() {
        sizeSeekBar = findViewById(R.id.sizeSeekBar)
        sizeValueText = findViewById(R.id.sizeValueText)
        previewText = findViewById(R.id.previewText)
        applyButton = findViewById(R.id.applyButton)
        presetButtonsContainer = findViewById(R.id.presetButtonsContainer)

        // 设置 SeekBar 范围
        sizeSeekBar.max = 250
        sizeSeekBar.min = 60
    }

    private fun loadCurrentSettings() {
        currentSizeDp = PetSettings.getPetSizeDp(this)
        sizeSeekBar.progress = currentSizeDp
        updateSizeDisplay()
    }

    private fun setupListeners() {
        // SeekBar 滑动监听
        sizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentSizeDp = progress.coerceIn(60, 300)
                updateSizeDisplay()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 应用按钮
        applyButton.setOnClickListener {
            PetSettings.setPetSizeDp(this, currentSizeDp)
            // 如果服务正在运行，发送广播通知更新大小
            if (PetService.isRunning) {
                sendBroadcast(android.content.Intent(PetService.ACTION_UPDATE_SIZE))
            }
            android.widget.Toast.makeText(this, "设置已保存！重启宠物后生效", android.widget.Toast.LENGTH_SHORT).show()
            finish()
        }

        // 创建预设按钮
        createPresetButtons()
    }

    private fun createPresetButtons() {
        presetButtonsContainer.removeAllViews()

        PetSettings.SIZE_OPTIONS.forEach { (sizeDp, label) ->
            val button = Button(this).apply {
                text = "$label\n${sizeDp}dp"
                textSize = 12f
                setPadding(16, 12, 16, 12)

                // 设置背景样式
                background = ContextCompat.getDrawable(context, android.R.drawable.btn_default)

                setOnClickListener {
                    currentSizeDp = sizeDp
                    sizeSeekBar.progress = sizeDp
                    updateSizeDisplay()
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16
            }

            presetButtonsContainer.addView(button, params)
        }
    }

    private fun updateSizeDisplay() {
        sizeValueText.text = "${currentSizeDp}dp"

        // 计算预览文本大小
        val previewSizeSp = (currentSizeDp / 6f).coerceIn(12f, 48f)
        previewText.textSize = previewSizeSp
        previewText.text = "🐱 宠物预览大小"

        // 更新描述
        val description = when (currentSizeDp) {
            in 0..90 -> "超小 - 像个小图标"
            in 91..110 -> "小 - 比较精致"
            in 111..135 -> "较小 - 推荐尺寸"
            in 136..165 -> "中 - 适中大小"
            in 166..190 -> "较大 - 比较显眼"
            in 191..230 -> "大 - 非常醒目"
            else -> "超大 - 占据屏幕"
        }
        previewText.text = "🐱 $description"
    }
}
