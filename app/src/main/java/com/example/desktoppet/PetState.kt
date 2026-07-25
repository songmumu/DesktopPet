package com.example.desktoppet

import kotlinx.coroutines.*
import kotlin.random.Random

/**
 * 宠物状态管理
 * 平时完全静止待机，只响应触摸互动
 */
class PetState {

    enum class Mood { HAPPY, ANGRY, BORED, SLEEPY, EXCITED }
    enum class Action { IDLE, WALK_LEFT, WALK_RIGHT, SIT, SLEEP, JUMP, DANCE }
    enum class Expression { NORMAL, SMILE, HAPPY, ANGRY, POUT, SLEEP, SURPRISE, HEART }

    var mood: Mood = Mood.HAPPY
        private set
    var action: Action = Action.IDLE
        private set
    var expression: Expression = Expression.NORMAL
        private set
    var animationFrame: Int = 0
        private set
    var facingRight: Boolean = true
        private set

    var onStateChange: (() -> Unit)? = null
    var onMessage: ((String) -> Unit)? = null
    var onLaunchApp: (() -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var animationJob: Job? = null

    init {
        startAnimation()
    }

    // ================================================================
    // 触摸互动
    // ================================================================

    fun onTap() {
        when (Random.nextInt(12)) {
            0  -> { expression = Expression.HAPPY; mood = Mood.HAPPY; showMessage("好开心~") }
            1  -> { expression = Expression.POUT; showMessage("哼~") }
            2  -> { expression = Expression.SURPRISE; showMessage("哎呀！") }
            3  -> { expression = Expression.HEART; mood = Mood.HAPPY; showMessage("喜欢你！") }
            4  -> { expression = Expression.SMILE; showMessage("你好呀~") }
            5  -> { expression = Expression.SURPRISE; showMessage("嘿嘿~") }
            6  -> { expression = Expression.HEART; showMessage("要抱抱！") }
            7  -> { expression = Expression.SMILE; showMessage("今天开心~") }
            8  -> { expression = Expression.POUT; showMessage("摸我干嘛~") }
            9  -> { expression = Expression.SURPRISE; showMessage("吓我一跳！") }
            10 -> { expression = Expression.HAPPY; showMessage("再摸一下嘛~") }
            11 -> { expression = Expression.SMILE; showMessage("嘿嘿你最好了！") }
        }
        onStateChange?.invoke()

        scope.launch {
            delay(2000)
            if (mood != Mood.ANGRY) {
                expression = Expression.NORMAL
                action = Action.IDLE
                onStateChange?.invoke()
            }
        }
    }

    fun onDoubleTap() {
        expression = Expression.HEART
        mood = Mood.EXCITED
        val msgs = listOf("超开心！","太喜欢了！","心都要化了~","好幸福呀！")
        showMessage(msgs.random())
        onStateChange?.invoke()

        scope.launch {
            delay(3000)
            expression = Expression.NORMAL
            mood = Mood.HAPPY
            action = Action.IDLE
            onStateChange?.invoke()
        }
    }

    // ================================================================
    // 内部定时器
    // ================================================================

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

    fun stop() {
        animationJob?.cancel()
        scope.cancel()
    }

    private fun showMessage(message: String) {
        onMessage?.invoke(message)
    }
}