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
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class PetService : Service() {

    companion object {
        const val ACTION_START = "com.example.desktoppet.START"
        const val ACTION_STOP = "com.example.desktoppet.STOP"
        const val CHANNEL_ID = "desk_pet_channel"
        const val NOTIFICATION_ID = 1001
        const val DOUBAO_PACKAGE = "com.larus.nova"
        private const val TAG = "PetService"

        @Volatile
        var isRunning = false
            private set
    }

    private lateinit var windowManager: WindowManager
    private lateinit var petState: PetState
    private var petView: View? = null
    private var petImageView: PetImageView? = null

    // 屏幕尺寸
    private var screenWidth = 0
    private var screenHeight = 0

    // 默认位置（屏幕右下方）
    private var homeX = 0
    private var homeY = 0

    // 触摸状态
    private val mainHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var isLongPressed = false
    private var hasWalked = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        getScreenSize()
        homeX = (screenWidth * 0.7f).toInt()
        homeY = (screenHeight * 0.55f).toInt()
        petState = PetState()
        createNotificationChannel()
        Log.d(TAG, "Service created. Screen: ${screenWidth}x${screenHeight}")
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
            .setContentText("长按宠物可以打开豆包APP · 点我陪你玩~")
            .setSmallIcon(R.drawable.ic_pet_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, "关闭", stopPendingIntent)
            .build()
    }

    // ================================================================
    // 显示宠物
    // ================================================================

    private fun showPet() {
        try {
            val petPx = (280 * resources.displayMetrics.density).toInt()
            val centerX = ((screenWidth - petPx) / 2).coerceAtLeast(0)
            val centerY = ((screenHeight / 3) - (petPx / 2)).coerceAtLeast(0)

            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val layoutParams = WindowManager.LayoutParams(
                petPx,
                petPx,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.RGBA_8888
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = centerX
                y = centerY
            }

            val petImgView = PetImageView(this, petState)
            petState.onLaunchApp = { openDoubao() }

            // 先尝试一次 addView，如果失败则说明悬浮窗被 ROM 拦截
            windowManager.addView(petImgView, layoutParams)
            petView = petImgView
            petImageView = petImgView

            // 1秒后移除 FLAG_NOT_TOUCHABLE 以支持触摸
            mainHandler.postDelayed({
                try {
                    val lp = petImgView.layoutParams as WindowManager.LayoutParams
                    lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                    windowManager.updateViewLayout(petImgView, lp)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to enable touch", e)
                }
            }, 1000)

            // 欢迎气泡
            mainHandler.postDelayed({
                val welcomes = listOf("主人来啦~","长按可以找豆包玩哦！","终于见到你啦！","抱抱~","点我陪我玩~")
                petState.onMessage?.invoke(welcomes.random())
            }, 800)

            setupTouchListener(petImgView, layoutParams)

            Log.d(TAG, "Pet shown at ${centerX},${centerY} size=${petPx}")
            Toast2.show(this, "宠物已出现在桌面！")
        } catch (e: SecurityException) {
            Log.e(TAG, "No overlay permission", e)
            Toast2.show(this, "❌ 没有悬浮窗权限，请在设置中开启")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show pet", e)
            Toast2.show(this, "❌ 显示宠物失败: ${e.localizedMessage}")
        }
    }

    private fun hidePet() {
        mainHandler.removeCallbacksAndMessages(null)
        petView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove pet view", e)
            }
            petView = null
            petImageView = null
        }
    }

    // ================================================================
    // 触摸事件（单击/双击/长按唤豆包）
    // ================================================================

    private var lastTapTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f
    private val DOUBLE_TAP_TIMEOUT = 300L
    private val MOVE_THRESHOLD = 15f

    private var isDragging = false
    private var startX = 0f
    private var startY = 0f
    private var initialX = 0
    private var initialY = 0

    private fun setupTouchListener(view: View, params: WindowManager.LayoutParams) {
        view.setOnTouchListener { v, event ->
            val px = event.rawX.toInt()
            val py = event.rawY.toInt()

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    isLongPressed = false
                    startX = event.rawX
                    startY = event.rawY
                    val lp = v.layoutParams as WindowManager.LayoutParams
                    initialX = lp.x
                    initialY = lp.y

                    // 启动长按检测（500ms）
                    val runnable = Runnable {
                        isLongPressed = true
                        petState.onLaunchApp?.invoke()
                    }
                    longPressRunnable = runnable
                    mainHandler.postDelayed(runnable, 500)
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - startX
                    val deltaY = event.rawY - startY
                    val distance = Math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toFloat()

                    if (distance > MOVE_THRESHOLD) {
                        // 取消长按，开始拖拽
                        if (!isLongPressed) {
                            longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                        }
                        isDragging = true
                        val lp = v.layoutParams as WindowManager.LayoutParams
                        lp.x = (initialX + deltaX).toInt()
                        lp.y = (initialY + deltaY).toInt()
                        windowManager.updateViewLayout(v, lp)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    longPressRunnable?.let { mainHandler.removeCallbacks(it) }

                    if (isLongPressed) {
                        // 长按已触发唤豆包，不做其他操作
                    } else if (isDragging) {
                        // 拖拽结束，随机说一句话
                        val dragMsgs = listOf("放这里~","好吧~","就这儿了！","挪一下~","可以了~")
                        petState.onMessage?.invoke(dragMsgs.random())

                        // 更新存放位置
                        val lp = v.layoutParams as WindowManager.LayoutParams
                        homeX = lp.x
                        homeY = lp.y
                    } else {
                        // 单击/双击检测
                        val now = System.currentTimeMillis()
                        val dx = Math.abs(event.rawX - lastTapX)
                        val dy = Math.abs(event.rawY - lastTapY)
                        if (now - lastTapTime < DOUBLE_TAP_TIMEOUT && dx < MOVE_THRESHOLD && dy < MOVE_THRESHOLD) {
                            petState.onDoubleTap()
                            lastTapTime = 0
                        } else {
                            lastTapTime = now
                            lastTapX = event.rawX
                            lastTapY = event.rawY
                            petState.onTap()
                        }
                    }
                }
            }
            true
        }
    }

    // ================================================================
    // 打开豆包 APP
    // ================================================================

    private fun openDoubao() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(DOUBAO_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                petState.onMessage?.invoke(
                    listOf("找豆包玩咯~","来啦来啦~","去找豆包啦！","豆包~我来了！").random()
                )
            } else {
                petState.onMessage?.invoke("还没装豆包呢~去应用商店下载吧！")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Doubao", e)
            petState.onMessage?.invoke("打不开豆包了...")
        }
    }

    // ================================================================
    // 工具方法
    // ================================================================

    private fun getScreenSize() {
        val display = windowManager.defaultDisplay
        val size = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            size.set(metrics.bounds.width(), metrics.bounds.height())
        } else {
            @Suppress("DEPRECATION")
            display.getRealSize(size)
        }
        screenWidth = size.x
        screenHeight = size.y
    }
}

/**
 * 在 Service 中显示 Toast 的工具
 */
object Toast2 {
    private var lastToast: android.widget.Toast? = null
    fun show(context: Context, text: String) {
        lastToast?.cancel()
        lastToast = android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT)
        lastToast?.show()
    }
}
