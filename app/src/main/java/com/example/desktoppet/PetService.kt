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

    private val mainHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var isLongPressed = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        getScreenSize()
        petState = PetState()
        createNotificationChannel()

        // 调试：列出所有已安装 APP
        listInstalledApps()

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
    // 列出设备上所有已安装 APP（调试用）
    // ================================================================

    private fun listInstalledApps() {
        try {
            val pm = packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .sortedBy { it.loadLabel(pm).toString().lowercase() }

            Log.d(TAG, "=== 已安装的 APP (共 ${apps.size} 个) ===")
            apps.forEach { app ->
                val label = try { app.loadLabel(pm).toString() } catch (_: Exception) { app.packageName }
                Log.d(TAG, "  [${label}] -> ${app.packageName}")
            }
            Log.d(TAG, "====================================")

            // 特别检查豆包
            val doubaoFound = apps.any { it.packageName == DOUBAO_PACKAGE }
            Log.d(TAG, "豆包($DOUBAO_PACKAGE) 是否安装: $doubaoFound")

            if (doubaoFound) {
                val doubaoIntent = pm.getLaunchIntentForPackage(DOUBAO_PACKAGE)
                Log.d(TAG, "豆包 LaunchIntent: $doubaoIntent")
            }
        } catch (e: Exception) {
            Log.e(TAG, "列APP失败", e)
        }
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
            Log.d(TAG, "Pet shown. Size=${petPx}px. Gravity=CENTER.")
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

                    longPressRunnable = Runnable { isLongPressed = true; petState.onLaunchApp?.invoke() }
                    mainHandler.postDelayed(longPressRunnable!!, 500)
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - startX
                    val deltaY = event.rawY - startY
                    if (Math.sqrt((deltaX*deltaX + deltaY*deltaY).toDouble()) > MOVE_THRESHOLD) {
                        if (!isLongPressed) longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                        isDragging = true
                        val lp = view.layoutParams as WindowManager.LayoutParams
                        // 修正边界：宠物完全在屏幕内
                        lp.x = (initialX + deltaX).toInt().coerceIn(0, screenWidth - lp.width)
                        lp.y = (initialY + deltaY).toInt().coerceIn(0, screenHeight - lp.height)
                        windowManager.updateViewLayout(view, lp)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                    when {
                        isLongPressed -> {}  // 长按已触发了
                        isDragging -> petState.onMessage?.invoke(listOf("放这里~","好吧~","就这儿了！","挪一下~","可以了~").random())
                        else -> {
                            val now = System.currentTimeMillis()
                            val dx = Math.abs(event.rawX - lastTapX).toInt()
                            val dy = Math.abs(event.rawY - lastTapY).toInt()
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
            val intent = packageManager.getLaunchIntentForPackage(DOUBAO_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                petState.onMessage?.invoke(listOf("找豆包玩咯~","来啦来啦~","去找豆包啦！","豆包~我来了！").random())
            } else {
                petState.onMessage?.invoke("还没装豆包呢~去应用商店下载吧！")
                Log.e(TAG, "豆包未安装: $DOUBAO_PACKAGE")
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