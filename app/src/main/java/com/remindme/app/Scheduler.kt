package com.remindme.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar
import kotlin.random.Random

object Scheduler {
    const val INT = 1
    const val RND = 2

    fun rescheduleAll(c: Context) { schedule(c, INT); schedule(c, RND) }

    fun schedule(c: Context, type: Int) {
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = pending(c, type)
        am.cancel(pi)
        val p = Prefs(c)
        val on = if (type == INT) p.intEnabled else p.rndEnabled
        if (!on) return
        val t = if (type == INT) nextInterval(p) else nextRandom(p)
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
    }

    private fun pending(c: Context, type: Int) = PendingIntent.getBroadcast(
        c, type, Intent(c, AlarmReceiver::class.java).putExtra("type", type),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun day(offset: Int): Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, offset)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    private fun at(d: Calendar, min: Int) = d.timeInMillis + min * 60_000L

    fun nextInterval(p: Prefs, now: Long = System.currentTimeMillis()): Long {
        val n = p.intEvery.coerceAtLeast(1)
        val s = p.intStart
        var e = p.intEnd; if (e < s) e += 1440
        for (off in -1..1) {
            val d = day(off)
            var m = s
            while (m <= e) { val t = at(d, m); if (t > now) return t; m += n }
        }
        return now + n * 60_000L
    }

    private fun randomTimes(p: Prefs, d: Calendar): List<Long> {
        val s = p.rndStart
        var e = p.rndEnd; if (e <= s) e += 1440
        val k = p.rndCount.coerceIn(1, 200)
        val gap = p.rndGap.coerceAtLeast(0)
        val span = (e - s - (k - 1) * gap).coerceAtLeast(0)
        val r = Random(d.timeInMillis + s * 7L + k * 31L + gap)
        val offs = List(k) { r.nextInt(span + 1) }.sorted()
        return offs.mapIndexed { i, o -> at(d, s + o + i * gap) }
    }

    fun nextRandom(p: Prefs, now: Long = System.currentTimeMillis()): Long {
        for (off in -1..2) randomTimes(p, day(off)).firstOrNull { it > now }?.let { return it }
        return now + 3_600_000L
    }
}
