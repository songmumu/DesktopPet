package com.example.desktoppet

import android.animation.ValueAnimator
import android.content.Context
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.sin

/**
 * 宠物图片视图
 * 使用真实照片作为桌面宠物
 * 支持点击、双击、悬停等交互（拖动由 Service 处理）
 */
class PetImageView(
    private val context: Context,
    private val petState: PetState
) : FrameLayout(context) {

    // 图片显示控件
    private val petImage: ImageView

    // 宠物尺寸
    private val petSize = 280

    // 弹跳动画偏移
    private var bounceOffset = 0f

    // 走路动画偏移
    private var walkOffset = 0f

    private var lastTapTime = 0L

    // 心情与照片资源映射
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
                // 根据心情选图
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
        // 创建 ImageView 显示宠物图片
        petImage = ImageView(context).apply {
            layoutParams = LayoutParams(petSize, petSize)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(getExpressionDrawable())
            tag = getExpressionDrawable()
        }
        addView(petImage)

        // 监听状态变化，切换图片
        petState.onStateChange = {
            updateImage()
        }
    }

    /**
     * 更新显示的图片
     */
    private fun updateImage() {
        val newDrawable = getExpressionDrawable()
        if (petImage.tag != newDrawable) {
            petImage.setImageResource(newDrawable)
            petImage.tag = newDrawable
        }
    }

    /**
     * 启动动画
     */
    fun startAnimation() {
        // 弹跳动画
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

        // 走路动画
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                walkOffset = animation.animatedValue as Float
            }
            start()
        }
    }

    /**
     * 应用动画到视图
     */
    private fun applyAnimation() {
        // 弹跳效果
        petImage.translationY = -bounceOffset

        // 走路时的轻微倾斜
        if (petState.action == PetState.Action.WALK_LEFT) {
            petImage.rotation = -3f + sin(walkOffset * Math.PI.toFloat() * 2) * 5
            petImage.scaleX = -1f // 翻转
        } else if (petState.action == PetState.Action.WALK_RIGHT) {
            petImage.rotation = 3f + sin(walkOffset * Math.PI.toFloat() * 2) * 5
            petImage.scaleX = 1f
        } else {
            // 待机时左右轻微摇摆
            petImage.rotation = sin(bounceOffset * 0.5f) * 2
            petImage.scaleX = 1f
        }

        // 点击反馈：放大缩放
        if (petState.expression == PetState.Expression.HEART) {
            petImage.scaleY = 1.1f
        } else {
            petImage.scaleY = 1f
        }
    }

    /**
     * 处理单击/双击
     */
    override fun performClick(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastTapTime < 300) {
            // 双击
            petState.onDoubleTap()
            lastTapTime = 0
        } else {
            // 单击
            petState.onTap()
            lastTapTime = now
        }
        return super.performClick()
    }
}