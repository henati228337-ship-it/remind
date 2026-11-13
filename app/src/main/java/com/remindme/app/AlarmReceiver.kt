package com.remindme.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val type = i.getIntExtra("type", 0)
        val p = Prefs(c)
        if (!p.paused) {
            if (type == Scheduler.INT) Notifier.show(c, "Напоминание", p.intText, 1)
            else {
                val l = p.rndTexts.lines().filter { it.isNotBlank() }
                Notifier.show(c, "Напоминание", l.randomOrNull() ?: "Время напомнить!", 2)
            }
        }
        Scheduler.schedule(c, type)
    }
}
