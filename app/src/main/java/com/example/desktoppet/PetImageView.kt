package com.example.desktoppet

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import kotlin.math.sin

/**
 * 宠物图片视图
 * - 透明背景照片（运行时去除白色背景）
 * - 头顶气泡文字
 * - 拖动、点击、双击
 * - 弹跳 + 走路动画
 */
class PetImageView(
    private val context: Context,
    private val petState: PetState
) : FrameLayout(context) {

    private val petImage: ImageView
    private val bubbleContainer: FrameLayout
    private val bubbleText: TextView

    private val petSize = 280

    private var bounceOffset = 0f
    private var walkOffset = 0f

    private var lastTapTime = 0L

    // 表情 → 照片资源
    private fun getExpressionDrawable(): Int {
        return when (petState.expression) {
            PetState.Expression.HAPPY -> R.drawable.pet_happy
            PetState.Expression.HEART -> R.drawable.pet_shy
            PetState.Expression.ANGRY -> R.drawable.pet_excited
            PetState.Expression.POUT -> R.drawable.pet_kiss
            PetState.Expression.SLEEP -> R.drawable.pet_sleep
            PetState.Expression.SURPRISE -> R.drawable.pet_surprise
            PetState.Expression.SMILE -> R.drawable.pet_shy
            PetState.Expression.NORMAL -> {
                when (petState.mood) {
                    PetState.Mood.HAPPY -> R.drawable.pet_happy
                    PetState.Mood.EXCITED -> R.drawable.pet_excited
                    PetState.Mood.SLEEPY -> R.drawable.pet_sleep
                    PetState.Mood.ANGRY -> R.drawable.pet_excited
                    PetState.Mood.BORED -> R.drawable.pet_idle
                }
            }
        }
    }

    init {
        // 透明背景
        setBackgroundColor(Color.TRANSPARENT)

        // 1. 图片
        petImage = ImageView(context).apply {
            layoutParams = LayoutParams(petSize, petSize).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.START
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            // 使用 alpha 通道：如果原图透明则显示透明，否则保持原样
            // 因为我们用 ColorMatrix 去除白色背景，所以这里不需要额外设置
            setImageResource(getExpressionDrawable())
            tag = getExpressionDrawable()
        }
        addView(petImage)

        // 2. 气泡（头顶左侧）
        bubbleContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(
                (petSize * 0.85).toInt(),
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                leftMargin = petSize - 30
                topMargin = 0
            }
            visibility = View.GONE
        }
        addView(bubbleContainer)

        bubbleText = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setPadding(24, 14, 24, 24)
            }
            setBackgroundResource(R.drawable.speech_bubble_bg)
            textSize = 13f
            setTextColor(Color.parseColor("#5D4037"))
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        bubbleContainer.addView(bubbleText)

        // 状态变化时更新图片
        petState.onStateChange = {
            updateImage()
        }

        // 消息回调：显示气泡
        petState.onMessage = { message ->
            showBubble(message)
        }
    }

    private fun updateImage() {
        val newDrawable = getExpressionDrawable()
        if (petImage.tag != newDrawable) {
            petImage.setImageResource(newDrawable)
            petImage.tag = newDrawable
        }
    }

    /**
     * 显示气泡，3秒后自动消失
     */
    private fun showBubble(message: String) {
        bubbleText.text = message
        bubbleContainer.visibility = View.VISIBLE
        bubbleContainer.alpha = 0f
        bubbleContainer.translationY = 10f

        // 弹出动画
        bubbleContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(200)
            .start()

        // 抖动一下表示强调
        bubbleContainer.animate()
            .rotation(-3f)
            .setDuration(80)
            .withEndAction {
                bubbleContainer.animate()
                    .rotation(3f)
                    .setDuration(80)
                    .withEndAction {
                        bubbleContainer.animate()
                            .rotation(0f)
                            .setDuration(80)
                            .start()
                    }
                    .start()
            }
            .start()

        // 3 秒后消失
        bubbleContainer.removeCallbacks(hideRunnable)
        bubbleContainer.postDelayed(hideRunnable, 3000)
    }

    private val hideRunnable = Runnable {
        bubbleContainer.animate()
            .alpha(0f)
            .translationY(10f)
            .setDuration(200)
            .withEndAction {
                bubbleContainer.visibility = View.GONE
            }
            .start()
    }

    fun startAnimation() {
        // 弹跳
        ValueAnimator.ofFloat(0f, 10f, 0f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                bounceOffset = animation.animatedValue as Float
                applyAnimation()
            }
            start()
        }

        // 走路
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                walkOffset = animation.animatedValue as Float
            }
            start()
        }
    }

    private fun applyAnimation() {
        // 弹跳（图片 + 气泡一起弹）
        val translateY = -bounceOffset
        petImage.translationY = translateY
        bubbleContainer.translationY = translateY

        // 走路倾斜
        if (petState.action == PetState.Action.WALK_LEFT) {
            petImage.rotation = -3f + sin(walkOffset * Math.PI.toFloat() * 2) * 5
            petImage.scaleX = -1f
        } else if (petState.action == PetState.Action.WALK_RIGHT) {
            petImage.rotation = 3f + sin(walkOffset * Math.PI.toFloat() * 2) * 5
            petImage.scaleX = 1f
        } else {
            petImage.rotation = sin(bounceOffset * 0.5f) * 2
            petImage.scaleX = 1f
        }

        // 爱心时放大
        petImage.scaleY = if (petState.expression == PetState.Expression.HEART) 1.1f else 1f
    }

    override fun performClick(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastTapTime < 300) {
            petState.onDoubleTap()
            lastTapTime = 0
        } else {
            petState.onTap()
            lastTapTime = now
        }
        return super.performClick()
    }
}