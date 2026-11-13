package com.remindme.app

import android.content.Context

class Prefs(c: Context) {
    private val p = c.getSharedPreferences("s", Context.MODE_PRIVATE)

    var intEnabled: Boolean
        get() = p.getBoolean("ie", false)
        set(v) = p.edit().putBoolean("ie", v).apply()
    var intStart: Int
        get() = p.getInt("is", 9 * 60)
        set(v) = p.edit().putInt("is", v).apply()
    var intEnd: Int
        get() = p.getInt("in", 21 * 60)
        set(v) = p.edit().putInt("in", v).apply()
    var intEvery: Int
        get() = p.getInt("iv", 60)
        set(v) = p.edit().putInt("iv", v).apply()
    var intText: String
        get() = p.getString("it", "Время сделать перерыв") ?: ""
        set(v) = p.edit().putString("it", v).apply()

    var rndEnabled: Boolean
        get() = p.getBoolean("re", false)
        set(v) = p.edit().putBoolean("re", v).apply()
    var rndStart: Int
        get() = p.getInt("rs", 9 * 60)
        set(v) = p.edit().putInt("rs", v).apply()
    var rndEnd: Int
        get() = p.getInt("rn", 21 * 60)
        set(v) = p.edit().putInt("rn", v).apply()
    var rndCount: Int
        get() = p.getInt("rc", 5)
        set(v) = p.edit().putInt("rc", v).apply()
    var rndGap: Int
        get() = p.getInt("rg", 30)
        set(v) = p.edit().putInt("rg", v).apply()
    var rndTexts: String
        get() = p.getString("rt", "Выпрями спину\nВыпей воды\nПосмотри вдаль 20 секунд") ?: ""
        set(v) = p.edit().putString("rt", v).apply()

    /** "" = звук по умолчанию, "none" = без звука, иначе uri */
    var sound: String
        get() = p.getString("snd", "") ?: ""
        set(v) = p.edit().putString("snd", v).apply()
    var vibrate: Boolean
        get() = p.getBoolean("vib", true)
        set(v) = p.edit().putBoolean("vib", v).apply()
    var paused: Boolean
        get() = p.getBoolean("pz", false)
        set(v) = p.edit().putBoolean("pz", v).apply()
    /** 0 авто, 1 светлая, 2 тёмная */
    var theme: Int
        get() = p.getInt("th", 0)
        set(v) = p.edit().putInt("th", v).apply()
}
