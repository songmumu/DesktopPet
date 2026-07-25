package com.example.desktoppet

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.sin

/**
 * 宠物绘制视图
 * 使用 Canvas 绘制可爱的桌面宠物
 */
class PetDrawView(
    private val context: Context,
    private val petState: PetState
) : View(context) {
    
    // 宠物尺寸
    private val petSize = 200
    
    // 颜色配置
    private val bodyColor = Color.parseColor("#FFB6C1")      // 粉色身体
    private val faceColor = Color.parseColor("#FFE4E1")      // 浅粉色脸部
    private val eyeColor = Color.parseColor("#2F4F4F")       // 深灰色眼睛
    private val cheekColor = Color.parseColor("#FF69B4")     // 粉红色腮红
    private val noseColor = Color.parseColor("#FF1493")      // 深粉色鼻子
    
    // 画笔
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bodyColor
        style = Paint.Style.FILL
    }
    
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = faceColor
        style = Paint.Style.FILL
    }
    
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = eyeColor
        style = Paint.Style.FILL
    }
    
    private val cheekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cheekColor
        style = Paint.Style.FILL
        alpha = 100
    }
    
    private val nosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = noseColor
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 40f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    
    // 动画相关
    private var bounceOffset = 0f
    private var walkOffset = 0f
    
    // 心情图标路径
    private val heartPath = Path()
    private val angryPath = Path()
    
    init {
        // 设置视图大小
        layoutParams = FrameLayout.LayoutParams(petSize, petSize)
        
        // 监听状态变化
        petState.onStateChange = {
            invalidate()
        }
    }
    
    /**
     * 启动动画
     */
    fun startAnimation() {
        // 弹跳动画
        val bounceAnimator = ValueAnimator.ofFloat(0f, 10f, 0f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                bounceOffset = animation.animatedValue as Float
                invalidate()
            }
        }
        bounceAnimator.start()
        
        // 走路动画
        val walkAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                walkOffset = animation.animatedValue as Float
            }
        }
        walkAnimator.start()
    }
    
    /**
     * 停止动画
     */
    fun stopAnimation() {
        // 动画会随视图销毁自动停止
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f + bounceOffset
        
        // 根据动作调整位置
        val actionOffset = when (petState.action) {
            PetState.Action.WALK_LEFT -> -walkOffset * 5
            PetState.Action.WALK_RIGHT -> walkOffset * 5
            else -> 0f
        }
        
        drawBody(canvas, centerX + actionOffset, centerY)
        drawFace(canvas, centerX + actionOffset, centerY)
        drawExpression(canvas, centerX + actionOffset, centerY)
    }
    
    /**
     * 绘制身体
     */
    private fun drawBody(canvas: Canvas, centerX: Float, centerY: Float) {
        // 主体（椭圆形身体）
        val bodyRect = RectF(
            centerX - 80, centerY - 60,
            centerX + 80, centerY + 60
        )
        canvas.drawOval(bodyRect, bodyPaint)
        
        // 耳朵
        val earSize = 35f
        // 左耳
        canvas.drawCircle(centerX - 60, centerY - 70, earSize, bodyPaint)
        canvas.drawCircle(centerX - 60, centerY - 70, earSize * 0.6f, facePaint)
        // 右耳
        canvas.drawCircle(centerX + 60, centerY - 70, earSize, bodyPaint)
        canvas.drawCircle(centerX + 60, centerY - 70, earSize * 0.6f, facePaint)
        
        // 小爪子
        val pawY = centerY + 50
        canvas.drawCircle(centerX - 40, pawY, 20f, facePaint)
        canvas.drawCircle(centerX + 40, pawY, 20f, facePaint)
    }
    
    /**
     * 绘制脸部
     */
    private fun drawFace(canvas: Canvas, centerX: Float, centerY: Float) {
        // 脸部背景
        val faceRect = RectF(
            centerX - 60, centerY - 40,
            centerX + 60, centerY + 40
        )
        canvas.drawOval(faceRect, facePaint)
        
        // 眼睛
        drawEyes(canvas, centerX, centerY - 10)
        
        // 腮红
        canvas.drawCircle(centerX - 45, centerY + 5, 12f, cheekPaint)
        canvas.drawCircle(centerX + 45, centerY + 5, 12f, cheekPaint)
        
        // 鼻子
        canvas.drawCircle(centerX, centerY + 10, 8f, nosePaint)
        
        // 嘴巴（根据心情变化）
        drawMouth(canvas, centerX, centerY + 25)
    }
    
    /**
     * 绘制眼睛
     */
    private fun drawEyes(canvas: Canvas, centerX: Float, centerY: Float) {
        val eyeOffset = 25f
        val eyeSize = 12f
        
        when (petState.expression) {
            PetState.Expression.HAPPY, PetState.Expression.HEART -> {
                // 开心的眯眯眼
                canvas.drawArc(
                    centerX - eyeOffset - eyeSize, centerY - eyeSize,
                    centerX - eyeOffset + eyeSize, centerY + eyeSize,
                    0f, 180f, false, eyePaint
                )
                canvas.drawArc(
                    centerX + eyeOffset - eyeSize, centerY - eyeSize,
                    centerX + eyeOffset + eyeSize, centerY + eyeSize,
                    0f, 180f, false, eyePaint
                )
                
                // 爱心眼特效
                if (petState.expression == PetState.Expression.HEART) {
                    drawHeart(canvas, centerX - eyeOffset, centerY, 15f)
                    drawHeart(canvas, centerX + eyeOffset, centerY, 15f)
                }
            }
            PetState.Expression.ANGRY -> {
                // 生气眼（斜线）
                canvas.drawCircle(centerX - eyeOffset, centerY, eyeSize, eyePaint)
                canvas.drawCircle(centerX + eyeOffset, centerY, eyeSize, eyePaint)
                // 眉毛
                canvas.drawLine(
                    centerX - eyeOffset - 15, centerY - 25,
                    centerX - eyeOffset + 15, centerY - 15,
                    eyePaint.apply { strokeWidth = 4f }
                )
                canvas.drawLine(
                    centerX + eyeOffset - 15, centerY - 15,
                    centerX + eyeOffset + 15, centerY - 25,
                    eyePaint
                )
            }
            PetState.Expression.SLEEP -> {
                // 睡觉眼（横线）
                canvas.drawLine(
                    centerX - eyeOffset - 10, centerY,
                    centerX - eyeOffset + 10, centerY,
                    eyePaint.apply { strokeWidth = 3f }
                )
                canvas.drawLine(
                    centerX + eyeOffset - 10, centerY,
                    centerX + eyeOffset + 10, centerY,
                    eyePaint
                )
            }
            PetState.Expression.SURPRISE -> {
                // 惊讶眼（大圆）
                canvas.drawCircle(centerX - eyeOffset, centerY, eyeSize * 1.5f, eyePaint)
                canvas.drawCircle(centerX + eyeOffset, centerY, eyeSize * 1.5f, eyePaint)
            }
            else -> {
                // 普通眼
                canvas.drawCircle(centerX - eyeOffset, centerY, eyeSize, eyePaint)
                canvas.drawCircle(centerX + eyeOffset, centerY, eyeSize, eyePaint)
                // 高光
                canvas.drawCircle(centerX - eyeOffset + 3, centerY - 3, 4f, 
                    Paint().apply { color = Color.WHITE })
                canvas.drawCircle(centerX + eyeOffset + 3, centerY - 3, 4f,
                    Paint().apply { color = Color.WHITE })
            }
        }
    }
    
    /**
     * 绘制嘴巴
     */
    private fun drawMouth(canvas: Canvas, centerX: Float, centerY: Float) {
        when (petState.expression) {
            PetState.Expression.HAPPY, PetState.Expression.HEART -> {
                // 微笑
                canvas.drawArc(
                    centerX - 15, centerY - 5,
                    centerX + 15, centerY + 10,
                    0f, 180f, false,
                    Paint().apply {
                        color = noseColor
                        style = Paint.Style.STROKE
                        strokeWidth = 3f
                    }
                )
            }
            PetState.Expression.ANGRY -> {
                // 生气嘴（倒三角）
                canvas.drawArc(
                    centerX - 12, centerY - 10,
                    centerX + 12, centerY + 5,
                    180f, 180f, false,
                    Paint().apply {
                        color = noseColor
                        style = Paint.Style.STROKE
                        strokeWidth = 3f
                    }
                )
            }
            PetState.Expression.POUT -> {
                // 嘟嘴
                canvas.drawCircle(centerX, centerY, 10f, nosePaint)
            }
            PetState.Expression.SLEEP -> {
                // 睡觉（张嘴）
                canvas.drawCircle(centerX, centerY, 8f, nosePaint)
                // Z 字符
                canvas.drawText("z", centerX + 30, centerY - 20, textPaint.apply { textSize = 30f })
            }
            else -> {
                // 普通嘴
                canvas.drawArc(
                    centerX - 10, centerY - 3,
                    centerX + 10, centerY + 7,
                    0f, 180f, false,
                    Paint().apply {
                        color = noseColor
                        style = Paint.Style.STROKE
                        strokeWidth = 2f
                    }
                )
            }
        }
    }
    
    /**
     * 绘制表情特效
     */
    private fun drawExpression(canvas: Canvas, centerX: Float, centerY: Float) {
        when (petState.mood) {
            PetState.Mood.EXCITED -> {
                // 兴奋特效（闪光）
                if (petState.animationFrame % 10 < 5) {
                    drawSparkle(canvas, centerX - 70, centerY - 50)
                    drawSparkle(canvas, centerX + 70, centerY - 50)
                }
            }
            PetState.Mood.ANGRY -> {
                // 生气特效（怒气符号）
                if (petState.animationFrame % 8 < 4) {
                    drawAngryMark(canvas, centerX + 70, centerY - 60)
                }
            }
            else -> {}
        }
    }
    
    /**
     * 绘制爱心
     */
    private fun drawHeart(canvas: Canvas, x: Float, y: Float, size: Float) {
        val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF69B4")
            style = Paint.Style.FILL
        }
        
        path.reset()
        path.moveTo(x, y + size / 4)
        path.cubicTo(x, y, x - size, y, x - size, y + size / 4)
        path.cubicTo(x - size, y + size / 2, x, y + size, x, y + size * 1.2f)
        path.cubicTo(x, y + size, x + size, y + size / 2, x + size, y + size / 4)
        path.cubicTo(x + size, y, x, y, x, y + size / 4)
        
        canvas.drawPath(path, heartPaint)
    }
    
    /**
     * 绘制闪光
     */
    private fun drawSparkle(canvas: Canvas, x: Float, y: Float) {
        val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD700")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        val size = 10f
        // 十字闪光
        canvas.drawLine(x - size, y, x + size, y, sparklePaint)
        canvas.drawLine(x, y - size, x, y + size, sparklePaint)
        canvas.drawLine(x - size * 0.5f, y - size * 0.5f, x + size * 0.5f, y + size * 0.5f, sparklePaint)
        canvas.drawLine(x + size * 0.5f, y - size * 0.5f, x - size * 0.5f, y + size * 0.5f, sparklePaint)
    }
    
    /**
     * 绘制怒气符号
     */
    private fun drawAngryMark(canvas: Canvas, x: Float, y: Float) {
        val angryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF0000")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        
        // 十字符号
        canvas.drawLine(x - 8, y - 8, x + 8, y + 8, angryPaint)
        canvas.drawLine(x - 8, y + 8, x + 8, y - 8, angryPaint)
    }
    
    companion object {
        private val path = Path()
    }
}
