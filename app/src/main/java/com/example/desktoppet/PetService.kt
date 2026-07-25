package com.example.desktoppet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat

class PetService : Service() {

    companion object {
        const val ACTION_START = "com.example.desktoppet.action.START"
        const val ACTION_STOP = "com.example.desktoppet.action.STOP"
        const val CHANNEL_ID = "desktop_pet_channel"
        const val NOTIFICATION_ID = 1001
        const val DOUBAO_PACKAGE = "com.larus.nova"

        var isRunning = false
            private set
    }

    private lateinit var windowManager: WindowManager
    private var petView: PetImageView? = null
    private lateinit var petState: PetState

    // 屏幕尺寸
    private var screenWidth = 0
    private var screenHeight = 0
    private var petDisplayWidth = 0
    private var petDisplayHeight = 0

    // 巡边位置更新
    private val mainHandler = Handler(mainLooper)
    private var edgeUpdateRunnable: Runnable? = null
    private var longPressRunnable: Runnable? = null
    private var isLongPressed = false

    // 默认位置（屏幕中下方靠右）
    private val homeX: Int get() = (screenWidth * 0.7f).toInt()
    private val homeY: Int get() = (screenHeight * 0.55f).toInt()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        getScreenSize()
        petState = PetState()
        createNotificationChannel()
    }

    private fun getScreenSize() {
        try {
            val display = windowManager.defaultDisplay
            val metrics = DisplayMetrics()
            display.getRealMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
            Log.d("PetService", "Screen: ${screenWidth}x${screenHeight}")
        } catch (e: Exception) {
            screenWidth = 1080
            screenHeight = 2400
            Log.e("PetService", "Failed to get screen size", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!isRunning) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    showPet()
                    isRunning = true
                }
            }
            ACTION_STOP -> {
                hidePet()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                isRunning = false
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        hidePet()
        isRunning = false
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "桌面宠物",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "桌面宠物运行状态"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, PetService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("桌面宠物")
            .setContentText("长按可以打开豆包APP · 10秒不理我就溜达去啦~")
            .setSmallIcon(R.drawable.ic_pet_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, "关闭", stopPendingIntent)
            .build()
    }

    // ================================================================
    // 显示/隐藏
    // ================================================================

    private fun showPet() {
        try {
            val petImageView = PetImageView(this, petState)

            // 将 280dp 转换为像素（确保窗口可见尺寸）
            val petPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 280f,
                resources.displayMetrics
            ).toInt()

            // 居中放置（避免跑出屏幕边缘）
            val centerX = (screenWidth - petPx) / 2
            val centerY = (screenHeight / 3) - (petPx / 2)  // 上方1/3区域

            val layoutParams = WindowManager.LayoutParams(
                petPx,
                petPx,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = centerX.coerceAtLeast(0)
                y = centerY.coerceAtLeast(0)
            }

            // 设置豆包回调
            petState.onLaunchApp = { openDoubao() }

            // 设置巡边回调
            petState.onStartEdgeWalk = { startEdgeWalkLoop() }
            petState.onEdgePositionNeeded = { side, progress ->
                updateEdgePosition(side, progress)
            }
            petState.onReturnHome = { startReturnHomeLoop() }

            setupTouchListener(petImageView, layoutParams)

            windowManager.addView(petImageView, layoutParams)
            petView = petImageView
            petImageView.startAnimation()

            // 获取宠物实际尺寸
            petImageView.post {
                petDisplayWidth = petImageView.measuredWidth
                petDisplayHeight = petImageView.measuredHeight
                Log.d("PetService", "Pet size: ${petDisplayWidth}x${petDisplayHeight}")
            }

            // 欢迎语
            mainHandler.postDelayed({
                val welcomes = listOf("主人来啦~","长按可以找豆包玩哦！","终于见到你啦！","抱抱~","10秒不理我我就溜达啦~")
                petState.onMessage?.invoke(welcomes.random())
            }, 800)

            Toast.makeText(this, "宠物已出现在桌面！", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e("PetService", "No overlay permission", e)
            Toast.makeText(this, "❌ 没有悬浮窗权限，请在设置中开启", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("PetService", "Failed to show pet", e)
            Toast.makeText(this, "❌ 显示宠物失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun hidePet() {
        stopEdgeWalkLoop()
        petView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.e("PetService", "Failed to remove pet view", e)
            }
            petView = null
        }
    }

    // ================================================================
    // 巡边位置更新
    // ================================================================

    private fun startEdgeWalkLoop() {
        stopEdgeWalkLoop()
        edgeUpdateRunnable = Runnable {
            updateEdgeWalkPosition()
        }
        mainHandler.post(edgeUpdateRunnable!!)
    }

    private fun stopEdgeWalkLoop() {
        edgeUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
        edgeUpdateRunnable = null
    }

    private fun updateEdgeWalkPosition() {
        val state = petState
        if (state.edgePhase == PetState.EdgePhase.NONE) return

        val view = petView ?: return
        val params = view.layoutParams as WindowManager.LayoutParams

        when (state.edgePhase) {
            PetState.EdgePhase.WALK_TO_LEFT -> {
                params.x -= 8
                if (params.x <= 0) {
                    params.x = 0
                    state.startClimbingEdge()
                }
                windowManager.updateViewLayout(view, params)
            }
            PetState.EdgePhase.CLIMBING_EDGE -> {
                state.advanceEdge()
            }
            else -> {}
        }

        if (state.edgePhase != PetState.EdgePhase.NONE &&
            state.edgePhase != PetState.EdgePhase.RETURN_HOME) {
            edgeUpdateRunnable = Runnable { updateEdgeWalkPosition() }
            mainHandler.postDelayed(edgeUpdateRunnable!!, 50)
        }
    }

    private fun updateEdgePosition(side: Int, progress: Float) {
        val view = petView ?: return
        val params = view.layoutParams as WindowManager.LayoutParams
        val pw = petDisplayWidth.coerceAtLeast(1)
        val ph = petDisplayHeight.coerceAtLeast(1)

        when (side) {
            0 -> {
                params.x = 0
                params.y = ((screenHeight - ph).toFloat() * (1f - progress)).toInt()
            }
            1 -> {
                params.x = ((screenWidth - pw).toFloat() * progress).toInt()
                params.y = 0
            }
            2 -> {
                params.x = screenWidth - pw
                params.y = ((screenHeight - ph).toFloat() * progress).toInt()
            }
            3 -> {
                params.x = ((screenWidth - pw).toFloat() * (1f - progress)).toInt()
                params.y = screenHeight - ph
            }
        }
        windowManager.updateViewLayout(view, params)
    }

    private fun startReturnHomeLoop() {
        stopEdgeWalkLoop()
        edgeUpdateRunnable = Runnable {
            returnToHomeStep()
        }
        mainHandler.post(edgeUpdateRunnable!!)
    }

    private fun returnToHomeStep() {
        val view = petView ?: return
        val params = view.layoutParams as WindowManager.LayoutParams

        val done = petState.advanceReturn()
        params.x = (params.x + (homeX - params.x) * 0.12f).toInt()
        params.y = (params.y + (homeY - params.y) * 0.12f).toInt()
        windowManager.updateViewLayout(view, params)

        if (!done) {
            edgeUpdateRunnable = Runnable { returnToHomeStep() }
            mainHandler.postDelayed(edgeUpdateRunnable!!, 30)
        } else {
            params.x = homeX
            params.y = homeY
            windowManager.updateViewLayout(view, params)
            edgeUpdateRunnable = null
        }
    }

    // ================================================================
    // 打开豆包
    // ================================================================

    private fun openDoubao() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(DOUBAO_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val msgs = listOf("找豆包玩~","去找豆包咯！","让豆包陪你~","叫豆包来！")
                petState.onMessage?.invoke(msgs.random())
                startActivity(intent)
            } else {
                petState.onMessage?.invoke("还没装豆包呢~去应用商店下载吧！")
            }
        } catch (e: Exception) {
            petState.onMessage?.invoke("打不开豆包...")
        }
    }

    // ================================================================
    // 触摸监听
    // ================================================================

    private fun setupTouchListener(view: PetImageView, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    isLongPressed = false

                    longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                    longPressRunnable = Runnable {
                        isLongPressed = true
                        petState.onLaunchApp?.invoke()
                    }
                    mainHandler.postDelayed(longPressRunnable!!, 500)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isDragging = true
                        if (!isLongPressed) {
                            longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                        }
                        params.x = initialX + deltaX
                        params.y = initialY + deltaY
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { mainHandler.removeCallbacks(it) }

                    if (isLongPressed) {
                        // 长按已完成
                    } else if (!isDragging) {
                        view.performClick()
                    } else {
                        val drops = listOf("放这里啦~","嘿嘿新家~","这儿不错~","换个位置~","好耶！")
                        mainHandler.postDelayed({
                            petState.onMessage?.invoke(drops.random())
                        }, 300)
                    }
                    isLongPressed = false
                    true
                }
                else -> false
            }
        }
    }
}
