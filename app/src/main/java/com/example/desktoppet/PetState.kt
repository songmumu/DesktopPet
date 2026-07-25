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
        DANCE,      // 跳舞
        WALK_EDGE   // 沿边框行走
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

    // 巡边状态机
    enum class EdgePhase {
        NONE,           // 正常状态
        WALK_TO_LEFT,   // 正在走到左边框
        CLIMBING_EDGE,  // 沿边框行走中
        RETURN_HOME     // 返回到屏幕中下方
    }

    // 当前状态
    var mood: Mood = Mood.HAPPY
        private set

    var action: Action = Action.IDLE
        private set

    var expression: Expression = Expression.NORMAL
        private set

    // 巡边状态
    var edgePhase: EdgePhase = EdgePhase.NONE
        private set
    var edgeSide: Int = 0          // 0=左  1=上  2=右  3=底（顺时针）
        private set
    var edgeProgress: Float = 0f   // 当前边进度 0~1
        private set
    var returnProgress: Float = 1f // 回家进度 1→0
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

    // 打开豆包回调
    var onLaunchApp: (() -> Unit)? = null

    // 巡边相关回调
    var onStartEdgeWalk: (() -> Unit)? = null
    var onEdgePositionNeeded: ((edgeSide: Int, edgeProgress: Float) -> Unit)? = null
    var onReturnHome: (() -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var idleJob: Job? = null
    private var animationJob: Job? = null
    private var edgeMonitorJob: Job? = null

    // 上次互动时间戳
    var lastInteractionTime: Long = System.currentTimeMillis()
        private set

    init {
        startIdleBehavior()
        startAnimation()
        startEdgeWalkMonitor()
    }

    // ================================================================
    // 点击/触摸互动
    // ================================================================

    /**
     * 记录互动
     */
    private fun recordInteraction() {
        lastInteractionTime = System.currentTimeMillis()
    }

    /**
     * 单击事件
     */
    fun onTap() {
        recordInteraction()

        // 如果正在巡边 → 回家
        if (edgePhase == EdgePhase.CLIMBING_EDGE || edgePhase == EdgePhase.WALK_TO_LEFT) {
            startReturnHome()
            return
        }

        // 正常点击反应
        when (Random.nextInt(12)) {
            0 -> { expression = Expression.HAPPY; mood = Mood.HAPPY; affection = (affection + 5).coerceIn(0, 100); showMessage("好开心~") }
            1 -> { expression = Expression.POUT; showMessage("哼~") }
            2 -> { expression = Expression.SURPRISE; showMessage("哎呀！") }
            3 -> { expression = Expression.HEART; mood = Mood.HAPPY; affection = (affection + 10).coerceIn(0, 100); showMessage("喜欢你！") }
            4 -> { expression = Expression.SMILE; showMessage("你好呀~") }
            5 -> { expression = Expression.SURPRISE; showMessage("嘿嘿~") }
            6 -> { expression = Expression.HEART; showMessage("要抱抱！") }
            7 -> { expression = Expression.SMILE; showMessage("今天开心~") }
            8 -> { expression = Expression.POUT; showMessage("摸我干嘛~") }
            9 -> { expression = Expression.SURPRISE; showMessage("吓我一跳！") }
            10 -> { expression = Expression.HAPPY; showMessage("再摸一下嘛~") }
            11 -> { expression = Expression.SMILE; showMessage("嘿嘿你最好了！") }
        }
        onStateChange?.invoke()

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
        recordInteraction()

        if (edgePhase != EdgePhase.NONE) {
            startReturnHome()
            return
        }

        expression = Expression.HEART
        mood = Mood.EXCITED
        affection = (affection + 15).coerceIn(0, 100)
        val msgs = listOf("超开心！","太喜欢了！","心都要化了~","好幸福呀！")
        showMessage(msgs.random())
        onStateChange?.invoke()

        scope.launch {
            delay(3000)
            expression = Expression.NORMAL
            mood = Mood.HAPPY
            onStateChange?.invoke()
        }
    }

    // ================================================================
    // 巡边行走
    // ================================================================

    /**
     * 开始巡边流程：先向左走到边框
     */
    fun startWalkToLeftEdge() {
        recordInteraction() // 重新计时，防止重复触发
        edgePhase = EdgePhase.WALK_TO_LEFT
        action = Action.WALK_LEFT
        facingRight = false
        expression = Expression.NORMAL
        mood = Mood.HAPPY
        showMessage("走走~")
        onStateChange?.invoke()
        onStartEdgeWalk?.invoke()
    }

    /**
     * 到达左边框，开始沿边框行进
     */
    fun startClimbingEdge() {
        edgePhase = EdgePhase.CLIMBING_EDGE
        edgeSide = 0  // 从左边开始
        edgeProgress = 0f
        action = Action.WALK_EDGE
        onStateChange?.invoke()
    }

    /**
     * 前进边框进度（由 Service 定时调用）
     * 每步增加 0.01，走完一条边约 50 步
     */
    fun advanceEdge() {
        if (edgePhase != EdgePhase.CLIMBING_EDGE) return

        edgeProgress += 0.01f
        if (edgeProgress >= 1f) {
            // 切换到下一条边
            edgeProgress = 0f
            edgeSide = (edgeSide + 1) % 4
            showMessage(when (edgeSide) {
                0 -> "上来啦~"
                1 -> "转~"
                2 -> "这边~"
                else -> "绕一圈~"
            })
        }
        onEdgePositionNeeded?.invoke(edgeSide, edgeProgress)
    }

    /**
     * 触摸后返回家中
     */
    fun startReturnHome() {
        edgePhase = EdgePhase.RETURN_HOME
        returnProgress = 1f
        val msgs = listOf("回来啦~","嘿嘿~","还是这里舒服~","不转啦！","陪你玩~")
        showMessage(msgs.random())
        action = Action.WALK_RIGHT
        facingRight = true
        onStateChange?.invoke()
        onReturnHome?.invoke()
    }

    /**
     * 回家进度更新
     */
    fun advanceReturn(): Boolean {
        if (edgePhase != EdgePhase.RETURN_HOME) return true
        returnProgress -= 0.05f
        if (returnProgress <= 0f) {
            returnProgress = 0f
            edgePhase = EdgePhase.NONE
            action = Action.IDLE
            expression = Expression.NORMAL
            mood = Mood.HAPPY
            onStateChange?.invoke()
            showMessage("到啦~")
            return true
        }
        return false
    }

    // ================================================================
    // 待机行为
    // ================================================================

    /**
     * 长时间不互动
     */
    fun onIdle() {
        when (Random.nextInt(5)) {
            0 -> {
                mood = Mood.BORED
                expression = Expression.POUT
                val msgs = listOf("好无聊~","有人吗...","为什么不理我~","寂寞...")
                showMessage(msgs.random())
            }
            1 -> {
                mood = Mood.ANGRY
                expression = Expression.ANGRY
                val msgs = listOf("哼！生气了！","再不理我就走啦！","气死我了！")
                showMessage(msgs.random())
            }
            2 -> {
                mood = Mood.SLEEPY
                expression = Expression.SLEEP
                action = Action.SLEEP
                val msgs = listOf("困了...zzZ","好累...先睡了","哈欠~","zzZ")
                showMessage(msgs.random())
            }
            3 -> {
                mood = Mood.BORED
                expression = Expression.SURPRISE
                showMessage("呜~想你了")
            }
            4 -> {
                mood = Mood.BORED
                expression = Expression.POUT
                showMessage("你干嘛去了呀")
            }
        }
        affection = (affection - 10).coerceIn(0, 100)
        onStateChange?.invoke()
    }

    // ================================================================
    // 内部定时器
    // ================================================================

    /**
     * 待机行为循环（每 15 秒随机触发小动作）
     */
    private fun startIdleBehavior() {
        idleJob?.cancel()
        idleJob = scope.launch {
            while (true) {
                delay(15000)

                // 如果已在巡边状态，不再触发
                if (edgePhase != EdgePhase.NONE) continue

                if (mood == Mood.HAPPY || mood == Mood.EXCITED) {
                    // 30% 几率触发无聊
                    if (Random.nextInt(10) < 3) {
                        onIdle()
                    }
                }

                // 随机走动（不互动的间隙）
                if (edgePhase == EdgePhase.NONE && Random.nextBoolean()) {
                    action = if (Random.nextBoolean()) Action.WALK_LEFT else Action.WALK_RIGHT
                    facingRight = action == Action.WALK_RIGHT
                    onStateChange?.invoke()
                    delay(3000)
                    if (edgePhase == EdgePhase.NONE) {
                        action = Action.IDLE
                        onStateChange?.invoke()
                    }
                }

                // 随机卖萌
                if (edgePhase == EdgePhase.NONE && Random.nextBoolean()) {
                    val autoMsg = listOf("嘿嘿~","我是最可爱的！","今天也要加油哦！","主人~在干嘛呢？","看我~看我~","好无聊...陪我玩嘛~")
                    showMessage(autoMsg.random())
                    expression = Expression.SMILE
                    onStateChange?.invoke()
                    delay(3000)
                    if (edgePhase == EdgePhase.NONE) {
                        expression = Expression.NORMAL
                        onStateChange?.invoke()
                    }
                }
            }
        }
    }

    /**
     * 巡边监控（每秒检查是否 10 秒未互动）
     */
    private fun startEdgeWalkMonitor() {
        edgeMonitorJob?.cancel()
        edgeMonitorJob = scope.launch {
            while (true) {
                delay(2000)  // 每 2 秒检查一次
                if (edgePhase != EdgePhase.NONE) continue  // 已经巡边中

                val idleSeconds = (System.currentTimeMillis() - lastInteractionTime) / 1000
                if (idleSeconds >= 10) {
                    startWalkToLeftEdge()
                }
            }
        }
    }

    /**
     * 动画帧
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

    fun stop() {
        idleJob?.cancel()
        animationJob?.cancel()
        edgeMonitorJob?.cancel()
        scope.cancel()
    }

    private fun showMessage(message: String) {
        onMessage?.invoke(message)
    }
}
