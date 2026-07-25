package com.example.desktoppet

import android.content.Context
import android.content.SharedPreferences

/**
 * 宠物设置管理
 */
object PetSettings {
    private const val PREFS_NAME = "pet_settings"
    private const val KEY_PET_SIZE = "pet_size"
    private const val KEY_PET_SIZE_DP = "pet_size_dp"
    
    // 预设大小选项（dp）
    val SIZE_OPTIONS = listOf(
        80 to "超小",
        100 to "小",
        120 to "较小",
        150 to "中",
        180 to "较大",
        200 to "大",
        250 to "超大"
    )
    
    const val DEFAULT_SIZE_DP = 120  // 默认改为较小尺寸
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 获取当前宠物大小（dp）
     */
    fun getPetSizeDp(context: Context): Int {
        return getPrefs(context).getInt(KEY_PET_SIZE_DP, DEFAULT_SIZE_DP)
    }
    
    /**
     * 设置宠物大小（dp）
     */
    fun setPetSizeDp(context: Context, sizeDp: Int) {
        getPrefs(context).edit().putInt(KEY_PET_SIZE_DP, sizeDp).apply()
    }
    
    /**
     * 将 dp 转换为像素
     */
    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    
    /**
     * 获取当前宠物大小（像素）
     */
    fun getPetSizePx(context: Context): Int {
        return dpToPx(context, getPetSizeDp(context))
    }
}
