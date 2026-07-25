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
        
        var isRunning = false
            private set
    }
    
    private lateinit var windowManager: WindowManager
    private var petView: PetImageView? = null
    private lateinit var petState: PetState
    
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
            .setContentText("宠物正在桌面上陪伴你")
            .setSmallIcon(R.drawable.ic_pet_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, "关闭", stopPendingIntent)
            .build()
    }
    
    private fun showPet() {
        // 创建宠物图片视图
        val petImageView = PetImageView(this, petState)

        // 设置布局参数
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

        // 添加触摸监听（拖动 + 点击）
        setupTouchListener(petImageView, layoutParams)

        // 添加到窗口
        windowManager.addView(petImageView, layoutParams)
        petView = petImageView

        // 启动动画
        petImageView.startAnimation()

        // 启动欢迎语（延迟 0.8 秒，等宠物出现）
        Handler(mainLooper).postDelayed({
            val welcomes = listOf("主人来啦~","嘿嘿你来了！","终于见到你啦！","抱抱~","想你啦！")
            petState.onMessage?.invoke(welcomes.random())
        }, 800)
    }

    private fun hidePet() {
        petView?.let { view ->
            windowManager.removeView(view)
            petView = null
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
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isDragging = true
                        params.x = initialX + deltaX
                        params.y = initialY + deltaY
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!isDragging) {
                        // 由 PetImageView 内部处理单击/双击
                        view.performClick()
                    } else {
                        // 拖动结束后随机说一句
                        val drops = listOf("放这里啦~","嘿嘿新家~","这儿不错~","换个位置~","好耶！")
                        val dropMsg = drops.random()
                        Handler(mainLooper).postDelayed({
                            petState.onMessage?.invoke(dropMsg)
                        }, 300)
                    }
                    true
                }
                else -> false
            }
        }
    }
}
