package com.example.desktoppet

import kotlinx.coroutines.*
import kotlin.random.Random

/**
 * 宠物状态管理
 * 平时安静待机，自动换表情 + 偶尔冒泡
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
    private var idleJob: Job? = null

    init {
        startAnimation()
        startIdleLoop()
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
        scheduleReturnToNormal(2000)
    }

    fun onDoubleTap() {
        expression = Expression.HEART
        mood = Mood.EXCITED
        showMessage(listOf("超开心！","太喜欢了！","心都要化了~","好幸福呀！").random())
        onStateChange?.invoke()
        scheduleReturnToNormal(3000)
    }

    /** 2~3 秒后恢复待机表情 */
    private fun scheduleReturnToNormal(delayMs: Long) {
        scope.launch {
            delay(delayMs)
            expression = Expression.NORMAL
            mood = Mood.HAPPY
            action = Action.IDLE
            onStateChange?.invoke()
        }
    }

    // ================================================================
    // 自动待机行为
    // ================================================================

    /**
     * 待机循环：
     * - 每 5 秒随机换表情（轻微眨眼/微笑）
     * - 每 20~30 秒冒一个气泡
     * - 偶尔做一个小动作（弹一下 / 歪头）
     */
    private fun startIdleLoop() {
        idleJob?.cancel()
        idleJob = scope.launch {
            var tick = 0
            while (true) {
                delay(5000)
                tick++

                // 每 4 个 tick（约 20 秒）冒一个气泡
                if (tick % 4 == 0) {
                    val bubbleMsg = listOf(
                        "嘿嘿~", "今天也好开心呀！", "主人今天忙吗？",
                        "想出去玩~", "我好可爱！", "在干嘛呢~",
                        "抱抱~", "好无聊...", "主人~看我！"
                    )
                    showMessage(bubbleMsg.random())
                }

                // 每个 tick 有 60% 几率换表情
                if (Random.nextFloat() < 0.6f) {
                    expression = when (Random.nextInt(6)) {
                        0 -> Expression.NORMAL
                        1 -> Expression.SMILE
                        2 -> Expression.SURPRISE
                        3 -> Expression.HEART
                        4 -> Expression.POUT
                        else -> Expression.NORMAL
                    }
                    mood = when (expression) {
                        Expression.HEART, Expression.SMILE -> Mood.HAPPY
                        Expression.POUT -> Mood.BORED
                        Expression.SURPRISE -> Mood.EXCITED
                        else -> Mood.HAPPY
                    }
                    onStateChange?.invoke()

                    // 表情保持 2~4 秒后恢复
                    val keepTime = Random.nextLong(2000, 4000)
                    scope.launch {
                        delay(keepTime)
                        if (expression != Expression.NORMAL) {
                            expression = Expression.NORMAL
                            mood = Mood.HAPPY
                            onStateChange?.invoke()
                        }
                    }
                }

                // 每 6 个 tick（约 30 秒）做个小动作
                if (tick % 6 == 0) {
                    action = when (Random.nextInt(3)) {
                        0 -> Action.JUMP
                        1 -> Action.DANCE
                        else -> Action.IDLE
                    }
                    onStateChange?.invoke()
                    scope.launch {
                        delay(1500)
                        action = Action.IDLE
                        onStateChange?.invoke()
                    }
                }
            }
        }
    }

    // ================================================================
    // 动画帧计时
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
        idleJob?.cancel()
        scope.cancel()
    }

    private fun showMessage(message: String) {
        onMessage?.invoke(message)
    }
}