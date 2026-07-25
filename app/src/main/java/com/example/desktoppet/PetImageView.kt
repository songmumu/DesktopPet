package com.example.desktoppet

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import kotlin.math.sin

/**
 * 宠物视图：
 * - 底层：宠物照片（透明背景，alpha 二值化）
 * - 上层：SpeechBubbleView（带三角尖的气泡）
 * - 动画：待机弹跳 + 行走帧 + 原地跳/摇摆
 */
class PetImageView(
    private val context: Context,
    private val petState: PetState
) : FrameLayout(context) {

    private val petImage: ImageView
    private val bubbleView: SpeechBubbleView
    private val bubbleText: TextView

    private val petSize = 280
    private var bounceOffset = 0f
    private var walkOffset = 0f

    // 7 帧行走动画
    private val walkFrames = intArrayOf(
        R.drawable.pet_walk_1, R.drawable.pet_walk_2, R.drawable.pet_walk_3,
        R.drawable.pet_walk_4, R.drawable.pet_walk_5, R.drawable.pet_walk_6,
        R.drawable.pet_walk_7
    )
    private var lastWalkFrameIndex = -1
    private val WALK_FRAME_RATE = 5

    init {
        setBackgroundColor(Color.TRANSPARENT)

        // === 底层：宠物照片 ===
        petImage = ImageView(context).apply {
            layoutParams = LayoutParams(petSize, petSize).apply {
                gravity = Gravity.BOTTOM or Gravity.START
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(getExpressionDrawable())
        }
        addView(petImage)

        // === 上层：气泡视图（气泡 = 圆角矩形 + 底部三角尖，尖指向宠物）===
        val bubbleW = (petSize * 0.82).toInt()
        bubbleView = SpeechBubbleView(context).apply {
            layoutParams = LayoutParams(bubbleW, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.START
                leftMargin = 10
                topMargin = -50  // 气泡底部（三角尖起点）在宠物头顶上方
            }
            visibility = View.GONE
        }
        addView(bubbleView)

        bubbleText = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setPadding(24, 14, 24, 36)  // 底部留 36dp 给三角尖
            }
            textSize = 13f
            setTextColor(Color.parseColor("#5D4037"))
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
        }
        bubbleView.addView(bubbleText)

        petState.onStateChange = { updateImage() }
        petState.onMessage = { msg -> showBubble(msg) }
        startAnimation()
    }

    private fun getExpressionDrawable(): Int {
        val action = petState.action
        if (action == PetState.Action.WALK_LEFT || action == PetState.Action.WALK_RIGHT) {
            return walkFrames[(petState.animationFrame / WALK_FRAME_RATE) % walkFrames.size]
        }
        return when (petState.expression) {
            PetState.Expression.HAPPY   -> R.drawable.pet_happy
            PetState.Expression.HEART    -> R.drawable.pet_shy
            PetState.Expression.ANGRY    -> R.drawable.pet_excited
            PetState.Expression.POUT     -> R.drawable.pet_kiss
            PetState.Expression.SLEEP    -> R.drawable.pet_sleep
            PetState.Expression.SURPRISE -> R.drawable.pet_surprise
            PetState.Expression.SMILE    -> R.drawable.pet_shy
            PetState.Expression.NORMAL   -> when (petState.mood) {
                PetState.Mood.HAPPY  -> R.drawable.pet_happy
                PetState.Mood.EXCITED -> R.drawable.pet_excited
                PetState.Mood.SLEEPY -> R.drawable.pet_sleep
                PetState.Mood.ANGRY  -> R.drawable.pet_excited
                PetState.Mood.BORED  -> R.drawable.pet_idle
            }
        }
    }

    private fun updateImage() {
        petImage.setImageResource(getExpressionDrawable())
    }

    private fun showBubble(message: String) {
        bubbleText.text = message
        bubbleView.visibility = View.VISIBLE
        bubbleView.alpha = 0f
        bubbleView.translationY = 8f

        bubbleView.animate().alpha(1f).translationY(0f).setDuration(200).start()

        // 抖动
        bubbleView.animate().rotation(-3f).setDuration(80)
            .withEndAction {
                bubbleView.animate().rotation(3f).setDuration(80)
                    .withEndAction {
                        bubbleView.animate().rotation(0f).setDuration(80).start()
                    }.start()
            }.start()

        bubbleView.removeCallbacks(hideRunnable)
        bubbleView.postDelayed(hideRunnable, 3000)
    }

    private val hideRunnable = Runnable {
        bubbleView.animate().alpha(0f).translationY(8f).setDuration(200)
            .withEndAction { bubbleView.visibility = View.GONE }
            .start()
    }

    private fun startAnimation() {
        ValueAnimator.ofFloat(0f, 10f, 0f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                bounceOffset = anim.animatedValue as Float
                applyAnimation()
            }
            start()
        }
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { anim -> walkOffset = anim.animatedValue as Float }
            start()
        }
    }

    private fun applyAnimation() {
        val action = petState.action
        val isWalking = action == PetState.Action.WALK_LEFT || action == PetState.Action.WALK_RIGHT

        // 行走帧切换
        if (isWalking) {
            val frame = (petState.animationFrame / WALK_FRAME_RATE) % walkFrames.size
            if (frame != lastWalkFrameIndex) {
                lastWalkFrameIndex = frame
                petImage.setImageResource(walkFrames[frame])
            }
        } else if (lastWalkFrameIndex >= 0) {
            lastWalkFrameIndex = -1
        }

        // 统一弹跳位移
        val bounceY = -bounceOffset
        petImage.translationY = bounceY
        bubbleView.translationY = bounceY

        // 动作动画
        when (action) {
            PetState.Action.WALK_LEFT -> {
                petImage.rotation = -3f + sin(walkOffset * Math.PI.toFloat() * 2) * 5
                petImage.scaleX = -1f
                petImage.scaleY = 1f
            }
            PetState.Action.WALK_RIGHT -> {
                petImage.rotation = 3f + sin(walkOffset * Math.PI.toFloat() * 2) * 5
                petImage.scaleX = 1f
                petImage.scaleY = 1f
            }
            PetState.Action.JUMP -> {
                val phase = (bounceOffset / 10f) % 1f
                val arc = sin(phase * Math.PI).toFloat()
                petImage.translationY = bounceY - arc * 30
                bubbleView.translationY = bounceY - arc * 30
            }
            PetState.Action.DANCE -> {
                petImage.rotation = sin(bounceOffset * 0.3f * Math.PI.toFloat()) * 15
            }
            else -> {
                petImage.rotation = sin(bounceOffset * 0.5f) * 2
                petImage.scaleX = 1f
                petImage.scaleY = if (petState.expression == PetState.Expression.HEART) 1.1f else 1f
            }
        }
    }
}

/**
 * 带三角尖的气泡视图
 * 外观：圆角矩形（上方 3 角）+ 底部三角尖（指向宠物）
 */
class SpeechBubbleView(context: Context) : FrameLayout(context) {

    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8FFFFFF")  // 半透明白底
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val cornerRadius = 18f
    private val triHeight = 16f
    private val triHalfBase = 11f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)

        // 测量子视图（文字）
        measureChildren(widthMeasureSpec, View.MeasureSpec.UNSPECIFIED)
        val childH = if (childCount > 0) getChildAt(0).measuredHeight else 0

        // 加上三角尖高度
        val desiredH = childH + triHeight.toInt() + paddingTop + paddingBottom
        val h = resolveSize(desiredH, heightMeasureSpec)
        setMeasuredDimension(width, h)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val triTop = h - triHeight  // 三角尖起点

        // 圆角矩形（顶部 3 角，底部 2 角为直角连接三角尖）
        val rect = RectF(1f, 1f, w - 1f, triTop)
        val path = Path().apply {
            // 顶左圆角
            moveTo(rect.left + cornerRadius, rect.top)
            arcTo(RectF(rect.left, rect.top, rect.left + cornerRadius * 2, rect.top + cornerRadius * 2),
                  180f, 90f)
            // 顶边到顶右
            lineTo(rect.right - cornerRadius, rect.top)
            arcTo(RectF(rect.right - cornerRadius * 2, rect.top, rect.right, rect.top + cornerRadius * 2),
                  -90f, 90f)
            // 右边到右下
            lineTo(rect.right, triTop)
            // 底边（连接三角尖，直角）
            lineTo(triTop, triTop)
            // 左下到左顶
            lineTo(rect.left, triTop)
            // 左上圆角
            arcTo(RectF(rect.left, rect.top, rect.left + cornerRadius * 2, rect.top + cornerRadius * 2),
                  90f, 90f)
            close()
        }

        canvas.drawPath(path, bubblePaint)
        canvas.drawPath(path, borderPaint)

        // 三角尖（尖向下，指向宠物）
        val triPath = Path().apply {
            val cx = w / 2f
            moveTo(cx - triHalfBase, triTop)
            lineTo(cx, h)
            lineTo(cx + triHalfBase, triTop)
            close()
        }
        canvas.drawPath(triPath, bubblePaint)
        canvas.drawPath(triPath, borderPaint)
    }
}
