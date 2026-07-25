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
import java.util.*

class PetImageView(context: Context, private val petState: PetState) : FrameLayout(context) {

    private val petImage: ImageView
    private val bubbleContainer: FrameLayout
    private val bubbleText: TextView
    private val bubbleView: SpeechBubbleView

    private val random = Random()
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var idleAnimator: ValueAnimator? = null
    private var bubbleRunnable: Runnable? = null
    private var blinkRunnable: Runnable? = null

    init {
        // 设置透明背景
        setBackgroundColor(Color.TRANSPARENT)

        // 宠物图片
        petImage = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.TRANSPARENT)
        }
        addView(petImage, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // 气泡容器
        bubbleContainer = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            visibility = INVISIBLE
        }
        
        // 气泡背景（带三角尖）
        bubbleView = SpeechBubbleView(context)
        bubbleContainer.addView(bubbleView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        
        // 气泡文字
        bubbleText = TextView(context).apply {
            textSize = 14f
            setTextColor(Color.BLACK)
            setPadding(24, 12, 24, 24)  // 底部留多一点给三角尖
            gravity = Gravity.CENTER
        }
        bubbleContainer.addView(bubbleText, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = 0
            bottomMargin = 12
        })
        
        // 气泡位置：紧贴宠物头顶（topMargin 为负值，向上偏移）
        addView(bubbleContainer, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = (-70 * context.resources.displayMetrics.density).toInt()
        })

        setupCallbacks()
        updatePetImage()
        startIdleAnimations()
    }

    private fun setupCallbacks() {
        petState.onMessage = { msg -> showBubble(msg) }
        petState.onStateChange = { 
            updatePetImage()
            applyAnimation()
        }
    }

    private fun updatePetImage() {
        val resId = when (petState.expression) {
            PetState.Expression.NORMAL -> R.drawable.pet_normal
            PetState.Expression.SMILE -> R.drawable.pet_happy
            PetState.Expression.HAPPY -> R.drawable.pet_happy
            PetState.Expression.ANGRY -> R.drawable.pet_angry
            PetState.Expression.POUT -> R.drawable.pet_shy
            PetState.Expression.SLEEP -> R.drawable.pet_sleepy
            PetState.Expression.SURPRISE -> R.drawable.pet_surprised
            PetState.Expression.HEART -> R.drawable.pet_love
        }
        petImage.setImageResource(resId)
    }

    private fun applyAnimation() {
        when (petState.action) {
            PetState.Action.JUMP -> {
                ValueAnimator.ofFloat(0f, -30f, 0f).apply {
                    duration = 400
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { petImage.translationY = it.animatedValue as Float }
                    start()
                }
            }
            PetState.Action.DANCE -> {
                ValueAnimator.ofFloat(-10f, 10f, -10f, 10f, 0f).apply {
                    duration = 600
                    addUpdateListener { petImage.translationX = it.animatedValue as Float }
                    start()
                }
            }
            PetState.Action.IDLE -> {
                petImage.translationX = 0f
                petImage.translationY = 0f
            }
            else -> {
                petImage.translationX = 0f
                petImage.translationY = 0f
            }
        }
    }

    private fun startIdleAnimations() {
        // 呼吸动画（轻微缩放）
        idleAnimator = ValueAnimator.ofFloat(1f, 1.03f, 1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val scale = it.animatedValue as Float
                petImage.scaleX = scale
                petImage.scaleY = scale
            }
            start()
        }

        // 眨眼动画
        blinkRunnable = object : Runnable {
            override fun run() {
                if (petState.action == PetState.Action.IDLE) {
                    petImage.alpha = 0.3f
                    handler.postDelayed({ petImage.alpha = 1f }, 150)
                }
                handler.postDelayed(this, (3000 + random.nextInt(4000)).toLong())
            }
        }
        handler.postDelayed(blinkRunnable!!, 3000)

        // 自动冒泡
        bubbleRunnable = object : Runnable {
            override fun run() {
                if (bubbleContainer.visibility != VISIBLE && random.nextFloat() < 0.6f) {
                    val msgs = listOf("好无聊呀~","主人~","想你了~","在干嘛呢？","陪陪我嘛~")
                    showBubble(msgs.random())
                }
                handler.postDelayed(this, (20000 + random.nextInt(15000)).toLong())
            }
        }
        handler.postDelayed(bubbleRunnable!!, 20000)
    }

    private fun showBubble(text: String) {
        bubbleText.text = text
        bubbleView.invalidate()
        bubbleContainer.visibility = VISIBLE
        bubbleContainer.alpha = 0f
        bubbleContainer.animate().alpha(1f).setDuration(200).start()

        bubbleRunnable?.let { handler.removeCallbacks(it) }
        handler.postDelayed({
            bubbleContainer.animate().alpha(0f).setDuration(200).withEndAction {
                bubbleContainer.visibility = INVISIBLE
            }.start()
        }, 3000)
        
        bubbleRunnable?.let { handler.postDelayed(it, (20000 + random.nextInt(15000)).toLong()) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        idleAnimator?.cancel()
        blinkRunnable?.let { handler.removeCallbacks(it) }
        bubbleRunnable?.let { handler.removeCallbacks(it) }
    }

    // ================================================================
    // 自定义气泡 View（带三角尖）
    // ================================================================
    
    private inner class SpeechBubbleView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F0FFFFFF")  // 半透明白色
            style = Paint.Style.FILL
        }
        private val path = Path()
        private val cornerRadius = 24f
        private val triangleHeight = 16f
        private val triangleWidth = 24f

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            
            path.reset()
            
            // 圆角矩形主体
            val rectH = h - triangleHeight
            path.addRoundRect(0f, 0f, w, rectH, cornerRadius, cornerRadius, Path.Direction.CW)
            
            // 底部三角尖（指向宠物）
            val cx = w / 2
            path.moveTo(cx - triangleWidth/2, rectH)
            path.lineTo(cx, h)
            path.lineTo(cx + triangleWidth/2, rectH)
            path.close()
            
            canvas.drawPath(path, paint)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            // 测量时预留三角尖空间
            val desiredW = View.MeasureSpec.getSize(widthMeasureSpec)
            val desiredH = (60 * resources.displayMetrics.density).toInt()  // 默认高度
            setMeasuredDimension(desiredW, desiredH)
        }
    }
}
