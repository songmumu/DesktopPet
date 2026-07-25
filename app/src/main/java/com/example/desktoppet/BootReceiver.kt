package com.example.desktoppet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 开机自启动接收器
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "系统启动完成，准备启动宠物服务")
            
            // 检查是否有悬浮窗权限
            if (android.provider.Settings.canDrawOverlays(context)) {
                // 有权限，自动启动服务
                val serviceIntent = Intent(context, PetService::class.java).apply {
                    action = PetService.ACTION_START
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                
                Log.d(TAG, "宠物服务已启动")
            } else {
                Log.d(TAG, "没有悬浮窗权限，无法自动启动")
            }
        }
    }
}
