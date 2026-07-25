package com.example.desktoppet

import kotlinx.coroutines.*
import kotlin.random.Random

/**
 * 宠物状态管理
 */
class PetState {
    
    // 心情状态
    enum class Mood {
        HAPPY,      // 开心
        ANGRY,      // 生气
        BORED,      // 无聊
        SLEEPY,     // 困倦
        EXCITED     // 兴奋
    }
    
    // 动作状态
    enum class Action {
        IDLE,       // 待机
        WALK_LEFT,  // 向左走
        WALK_RIGHT, // 向右走
        SIT,        // 坐下
        SLEEP,      // 睡觉
        JUMP,       // 跳跃
        DANCE       // 跳舞
    }
    
    // 表情
    enum class Expression {
        NORMAL,     // 普通
        SMILE,      // 微笑
        HAPPY,      // 开心
        ANGRY,      // 生气
        POUT,       // 嘟嘴
        SLEEP,      // 睡觉
        SURPRISE,   // 惊讶
        HEART       // 爱心眼
    }
    
    // 当前状态
    var mood: Mood = Mood.HAPPY
        private set
    
    var action: Action = Action.IDLE
        private set
    
    var expression: Expression = Expression.NORMAL
        private set
    
    // 饥饿度 (0-100)
    var hunger: Int = 50
        private set
    
    // 好感度 (0-100)
    var affection: Int = 50
        private set
    
    // 动画帧
    var animationFrame: Int = 0
        private set
    
    // 是否面向右边
    var facingRight: Boolean = true
        private set
    
    // 移动位置（用于走路动画）
    var offsetX: Float = 0f
        private set
    
    // 状态更新回调
    var onStateChange: (() -> Unit)? = null

    // 消息显示回调（用于气泡文字）
    var onMessage: ((String) -> Unit)? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var idleJob: Job? = null
    private var animationJob: Job? = null
    
    init {
        startIdleBehavior()
        startAnimation()
    }
    
    /**
     * 点击事件
     */
    fun onTap() {
        // 随机反应
        when (Random.nextInt(5)) {
            0 -> {
                expression = Expression.HAPPY
                mood = Mood.HAPPY
                affection = (affection + 5).coerceIn(0, 100)
                showMessage("好开心~")
            }
            1 -> {
                expression = Expression.POUT
                showMessage("哼~")
            }
            2 -> {
                expression = Expression.SURPRISE
                showMessage("哎呀！")
            }
            3 -> {
                expression = Expression.HEART
                mood = Mood.HAPPY
                affection = (affection + 10).coerceIn(0, 100)
                showMessage("喜欢你！")
            }
            4 -> {
                expression = Expression.SMILE
                showMessage("你好呀~")
            }
        }
        
        onStateChange?.invoke()
        
        // 恢复待机
        scope.launch {
            delay(2000)
            if (mood != Mood.ANGRY) {
                expression = Expression.NORMAL
                onStateChange?.invoke()
            }
        }
    }
    
    /**
     * 双击事件
     */
    fun onDoubleTap() {
        expression = Expression.HEART
        mood = Mood.EXCITED
        affection = (affection + 15).coerceIn(0, 100)
        showMessage("超开心！")
        
        onStateChange?.invoke()
        
        scope.launch {
            delay(3000)
            expression = Expression.NORMAL
            mood = Mood.HAPPY
            onStateChange?.invoke()
        }
    }
    
    /**
     * 长时间不互动
     */
    fun onIdle() {
        when (Random.nextInt(3)) {
            0 -> {
                mood = Mood.BORED
                expression = Expression.POUT
                showMessage("好无聊~")
            }
            1 -> {
                mood = Mood.ANGRY
                expression = Expression.ANGRY
                showMessage("不理我！")
            }
            2 -> {
                mood = Mood.SLEEPY
                expression = Expression.SLEEP
                action = Action.SLEEP
                showMessage("困了...")
            }
        }
        
        affection = (affection - 10).coerceIn(0, 100)
        onStateChange?.invoke()
    }
    
    /**
     * 开始待机行为
     */
    private fun startIdleBehavior() {
        idleJob?.cancel()
        idleJob = scope.launch {
            while (true) {
                // 每 30 秒检查一次
                delay(30000)
                
                // 如果待机时间过长，触发无互动事件
                if (mood == Mood.HAPPY || mood == Mood.EXCITED) {
                    onIdle()
                }
                
                // 随机移动
                if (Random.nextBoolean()) {
                    action = if (Random.nextBoolean()) Action.WALK_LEFT else Action.WALK_RIGHT
                    facingRight = action == Action.WALK_RIGHT
                    onStateChange?.invoke()
                    
                    delay(3000)
                    action = Action.IDLE
                    onStateChange?.invoke()
                }
                
                // 随机卖萌
                if (Random.nextBoolean()) {
                    expression = Expression.SMILE
                    onStateChange?.invoke()
                    delay(2000)
                    expression = Expression.NORMAL
                    onStateChange?.invoke()
                }
            }
        }
    }
    
    /**
     * 开始动画
     */
    private fun startAnimation() {
        animationJob?.cancel()
        animationJob = scope.launch {
            while (true) {
                animationFrame = (animationFrame + 1) % 60
                onStateChange?.invoke()
                delay(50)
            }
        }
    }
    
    /**
     * 停止所有协程
     */
    fun stop() {
        idleJob?.cancel()
        animationJob?.cancel()
        scope.cancel()
    }
    
    /**
     * 显示消息（气泡文字）
     */
    private fun showMessage(message: String) {
        onMessage?.invoke(message)
    }
}
