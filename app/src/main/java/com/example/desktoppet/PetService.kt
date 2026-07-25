package com.example.desktoppet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat

class PetService : Service() {
    
    companion object {
        const val ACTION_START = "com.example.desktoppet.action.START"
        const val ACTION_STOP = "com.example.desktoppet.action.STOP"
        const val CHANNEL_ID = "desktop_pet_channel"
        const val NOTIFICATION_ID = 1001

        // 豆包APP包名
        const val DOUBAO_PACKAGE = "com.larus.nova"
        
        var isRunning = false
            private set
    }
    
    private lateinit var windowManager: WindowManager
    private var petView: PetImageView? = null
    private lateinit var petState: PetState

    // 长按检测
    private val mainHandler = Handler(mainLooper)
    private var longPressRunnable: Runnable? = null
    private var isLongPressed = false
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        petState = PetState()
        createNotificationChannel()
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
        val stopIntent = Intent(this, PetService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("桌面宠物")
            .setContentText("长按宠物可以打开豆包APP")
            .setSmallIcon(R.drawable.ic_pet_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, "关闭", stopPendingIntent)
            .build()
    }
    
    private fun showPet() {
        val petImageView = PetImageView(this, petState)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        // 设置长按唤起豆包
        petState.onLaunchApp = {
            openDoubao()
        }

        setupTouchListener(petImageView, layoutParams)

        windowManager.addView(petImageView, layoutParams)
        petView = petImageView
        petImageView.startAnimation()

        // 启动欢迎语
        mainHandler.postDelayed({
            val welcomes = listOf("主人来啦~","长按可以找豆包玩哦！","终于见到你啦！","抱抱~","想你啦！")
            petState.onMessage?.invoke(welcomes.random())
        }, 800)
    }

    private fun hidePet() {
        petView?.let { view ->
            windowManager.removeView(view)
            petView = null
        }
    }
    
    /**
     * 打开豆包APP
     */
    private fun openDoubao() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(DOUBAO_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // 告诉用户要去找豆包了
                val msgs = listOf("找豆包玩~","去找豆包咯！","让豆包陪你~","叫豆包来！")
                petState.onMessage?.invoke(msgs.random())
                startActivity(intent)
            } else {
                // 没安装豆包
                petState.onMessage?.invoke("还没装豆包呢~去应用商店下载吧！")
            }
        } catch (e: Exception) {
            petState.onMessage?.invoke("打不开豆包...")
        }
    }
    
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

                    // 启动长按检测（500ms后触发）
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
                        // 移动时取消长按
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
                    // 取消长按检测
                    longPressRunnable?.let { mainHandler.removeCallbacks(it) }

                    if (isLongPressed) {
                        // 长按已完成，不做其他操作
                    } else if (!isDragging) {
                        // 单击/双击
                        view.performClick()
                    } else {
                        // 拖动结束
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
