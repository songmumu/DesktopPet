package com.example.desktoppet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
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

        @Volatile var isRunning = false
            private set
    }

    private lateinit var windowManager: WindowManager
    private lateinit var petState: PetState
    private var petView: View? = null

    private var screenWidth = 0
    private var screenHeight = 0
    private var statusBarHeight = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var isLongPressed = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        getScreenSize()
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
            val channel = NotificationChannel(CHANNEL_ID, "桌面宠物", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "桌面宠物运行状态"; setShowBadge(false) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, PetService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("桌面宠物")
            .setContentText("长按宠物可以打开豆包APP · 点我陪我玩~")
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
            val petSizeDp = 200
            val petPx = (petSizeDp * resources.displayMetrics.density).toInt()

            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val layoutParams = WindowManager.LayoutParams(
                petPx, petPx, windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.RGBA_8888
            ).apply {
                gravity = Gravity.CENTER
                x = 0
                y = (screenHeight * -0.08f).toInt()
            }

            val petImgView = PetImageView(this, petState)
            petState.onLaunchApp = { openDoubao() }

            windowManager.addView(petImgView, layoutParams)
            petView = petImgView

            mainHandler.postDelayed({
                val welcomes = listOf("主人来啦~","长按可以找豆包玩哦！","终于见到你啦！","抱抱~","点我陪我玩~")
                petState.onMessage?.invoke(welcomes.random())
            }, 800)

            setupTouchListener(petImgView)
            Log.d(TAG, "Pet shown. Size=${petPx}px at (${layoutParams.x}, ${layoutParams.y})")
            Toast2.show(this, "宠物已出现在桌面！")
        } catch (e: SecurityException) {
            Log.e(TAG, "No overlay permission", e)
            Toast2.show(this, "没有悬浮窗权限，请在设置中开启")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show pet", e)
            Toast2.show(this, "显示宠物失败: ${e.localizedMessage}")
        }
    }

    private fun hidePet() {
        mainHandler.removeCallbacksAndMessages(null)
        petView?.let { view ->
            try { windowManager.removeView(view) } catch (_: Exception) {}
            petView = null
        }
    }

    // ================================================================
    // 触摸：单击/双击/长按唤豆包/拖拽
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

    private fun setupTouchListener(view: View) {
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    isLongPressed = false
                    startX = event.rawX
                    startY = event.rawY
                    val lp = view.layoutParams as WindowManager.LayoutParams

                    if (lp.gravity == Gravity.CENTER) {
                        val halfW = lp.width / 2
                        val halfH = lp.height / 2
                        initialX = screenWidth / 2 - halfW + lp.x
                        initialY = screenHeight / 2 - halfH + lp.y
                        lp.gravity = Gravity.TOP or Gravity.START
                        lp.x = initialX
                        lp.y = initialY
                        windowManager.updateViewLayout(view, lp)
                    } else {
                        initialX = lp.x
                        initialY = lp.y
                    }

                    longPressRunnable = Runnable { 
                        isLongPressed = true
                        petState.onLaunchApp?.invoke() 
                    }
                    mainHandler.postDelayed(longPressRunnable!!, 500)
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - startX
                    val deltaY = event.rawY - startY
                    if (kotlin.math.sqrt((deltaX*deltaX + deltaY*deltaY).toDouble()) > MOVE_THRESHOLD) {
                        if (!isLongPressed) longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                        isDragging = true
                        val lp = view.layoutParams as WindowManager.LayoutParams
                        
                        // 计算新位置
                        val newX = (initialX + deltaX).toInt()
                        val newY = (initialY + deltaY).toInt()
                        
                        // 边界限制：允许宠物贴边（x 范围 0 ~ screenWidth-lp.width）
                        // y 范围 0 ~ screenHeight-lp.height-statusBarHeight（避开状态栏）
                        lp.x = newX.coerceIn(0, screenWidth - lp.width)
                        lp.y = newY.coerceIn(0, screenHeight - lp.height - statusBarHeight)
                        
                        windowManager.updateViewLayout(view, lp)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                    when {
                        isLongPressed -> {}  // 长按已触发
                        isDragging -> petState.onMessage?.invoke(listOf("放这里~","好吧~","就这儿了！","挪一下~","可以了~").random())
                        else -> {
                            val now = System.currentTimeMillis()
                            val dx = kotlin.math.abs(event.rawX - lastTapX).toInt()
                            val dy = kotlin.math.abs(event.rawY - lastTapY).toInt()
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
            }
            true
        }
    }

    // ================================================================
    // 打开豆包 APP
    // ================================================================

    private fun openDoubao() {
        try {
            // 方法1：直接获取启动 Intent
            var intent = packageManager.getLaunchIntentForPackage(DOUBAO_PACKAGE)
            
            // 方法2：如果没拿到，尝试用 resolveActivity
            if (intent == null) {
                val testIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage(DOUBAO_PACKAGE)
                }
                intent = packageManager.resolveActivity(testIntent, 0)?.activityInfo?.let {
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        setClassName(it.packageName, it.name)
                    }
                }
            }
            
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                petState.onMessage?.invoke(listOf("找豆包玩咯~","来啦来啦~","去找豆包啦！","豆包~我来了！").random())
                Log.d(TAG, "Doubao launched successfully")
            } else {
                petState.onMessage?.invoke("还没装豆包呢~去应用商店下载吧！")
                Log.e(TAG, "豆包未安装或无法启动: $DOUBAO_PACKAGE")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Doubao", e)
            petState.onMessage?.invoke("打不开豆包了...")
        }
    }

    private fun getScreenSize() {
        val size = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            size.set(metrics.bounds.width(), metrics.bounds.height())
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(size)
        }
        screenWidth = size.x
        screenHeight = size.y
        
        // 获取状态栏高度
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        statusBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
        
        Log.d(TAG, "Screen: ${screenWidth}x${screenHeight}, statusBar: $statusBarHeight")
    }
}

object Toast2 {
    private var lastToast: android.widget.Toast? = null
    fun show(ctx: Context, text: String) {
        lastToast?.cancel()
        lastToast = android.widget.Toast.makeText(ctx, text, android.widget.Toast.LENGTH_SHORT)
        lastToast?.show()
    }
}
