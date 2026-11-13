package com.remindme.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat

object Notifier {
    fun show(c: Context, title: String, text: String, id: Int) {
        val p = Prefs(c)
        val nm = c.getSystemService(NotificationManager::class.java)
        // Звук канала нельзя менять после создания — делаем новый канал под каждый звук
        val chId = "r_${p.sound.hashCode()}_${p.vibrate}"
        if (nm.getNotificationChannel(chId) == null) {
            nm.notificationChannels.filter { it.id.startsWith("r_") }.forEach { nm.deleteNotificationChannel(it.id) }
            val ch = NotificationChannel(chId, "Напоминания", NotificationManager.IMPORTANCE_HIGH)
            val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
            when (p.sound) {
                "none" -> ch.setSound(null, null)
                "" -> ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), attrs)
                else -> ch.setSound(Uri.parse(p.sound), attrs)
            }
            ch.enableVibration(p.vibrate)
            nm.createNotificationChannel(ch)
        }
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(c, chId)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .build()
        try { nm.notify(id, n) } catch (_: SecurityException) {}
    }
}
