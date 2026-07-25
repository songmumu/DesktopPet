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
 * - 透明背景照片
 * - 头顶气泡文字
 * - 行走帧动画（8帧走路循环）
 * - 拖动、点击、双击
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

    // 行走动画帧资源 ID 数组（7帧，去除第8帧）
    private val walkFrames = intArrayOf(
        R.drawable.pet_walk_1,
        R.drawable.pet_walk_2,
        R.drawable.pet_walk_3,
        R.drawable.pet_walk_4,
        R.drawable.pet_walk_5,
        R.drawable.pet_walk_6,
        R.drawable.pet_walk_7
    )

    // 上次显示的行走帧索引
    private var lastWalkFrameIndex = -1

    // 行走帧速率：每 N 帧切换一次（animationFrame 每 50ms 递增一次）
// 5 = 250ms/帧，7帧一个循环约 1.75 秒（走路更慢更稳）
    private val WALK_FRAME_RATE = 5

    /**
     * 返回当前边对应的旋转角度（度）
     * 左: +90°  上: 180°  右: -90°  底: 0°
     * 宠物脚踩边框，身体垂直于边框
     */
    private fun getEdgeRotation(): Float {
        return when (petState.edgeSide) {
            0 -> 90f   // 左边：身体横向，头朝右
            1 -> 180f  // 上边：倒立
            2 -> -90f  // 右边：身体横向，头朝左
            else -> 0f // 底边：正常直立
        }
    }

    /**
     * 根据当前状态返回要显示的图片资源
     */
    private fun getExpressionDrawable(): Int {
        // 巡边时也显示行走帧
        if (petState.action == PetState.Action.WALK_EDGE ||
            petState.action == PetState.Action.WALK_LEFT ||
            petState.action == PetState.Action.WALK_RIGHT) {
            val frameIndex = (petState.animationFrame / WALK_FRAME_RATE) % walkFrames.size
            return walkFrames[frameIndex]
        }

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
        // 半透明红色背景（定位调试，确认能看见窗口后改回透明）
        setBackgroundColor(Color.argb(100, 255, 0, 0))

        // 1. 图片
        petImage = ImageView(context).apply {
            layoutParams = LayoutParams(petSize, petSize).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.START
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(getExpressionDrawable())
            tag = getExpressionDrawable()
        }
        addView(petImage)

        // 2. 气泡（头顶）
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

    /**
     * 更新宠物图片
     */
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

        // 抖动表示强调
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
        val isWalking = petState.action == PetState.Action.WALK_LEFT ||
                        petState.action == PetState.Action.WALK_RIGHT ||
                        petState.action == PetState.Action.WALK_EDGE
        val isEdgeWalking = petState.action == PetState.Action.WALK_EDGE &&
                            petState.edgePhase == PetState.EdgePhase.CLIMBING_EDGE

        // 行走时：每帧更新行走关键帧
        if (isWalking) {
            val frameIndex = (petState.animationFrame / WALK_FRAME_RATE) % walkFrames.size
            if (frameIndex != lastWalkFrameIndex) {
                lastWalkFrameIndex = frameIndex
                petImage.setImageResource(walkFrames[frameIndex])
                petImage.tag = walkFrames[frameIndex]
            }
        } else {
            lastWalkFrameIndex = -1
        }

        // 弹跳（图片 + 气泡一起弹）
        val translateY = -bounceOffset
        petImage.translationY = translateY
        bubbleContainer.translationY = translateY

        if (isEdgeWalking) {
            // 沿边框行走：根据边旋转 + 弹跳摆动
            val rotation = getEdgeRotation()
            val swing = sin(walkOffset * Math.PI.toFloat() * 2) * 4f
            petImage.rotation = rotation + swing
            petImage.scaleX = 1f
            petImage.scaleY = 1f
        } else if (petState.action == PetState.Action.WALK_LEFT) {
            petImage.rotation = -3f + sin(walkOffset * Math.PI.toFloat() * 2) * 5
            petImage.scaleX = -1f
            petImage.scaleY = 1f
        } else if (petState.action == PetState.Action.WALK_RIGHT) {
            petImage.rotation = 3f + sin(walkOffset * Math.PI.toFloat() * 2) * 5
            petImage.scaleX = 1f
            petImage.scaleY = 1f
        } else {
            petImage.rotation = sin(bounceOffset * 0.5f) * 2
            petImage.scaleX = 1f
            petImage.scaleY = if (petState.expression == PetState.Expression.HEART) 1.1f else 1f
        }
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