package com.imtiyaztour.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object ProspekNotificationHelper {
    private const val CHANNEL_ID = "prospek_fu"
    private const val CHANNEL_NAME = "Follow Up Prospek"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Pengingat follow up prospek"
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    fun showFuNotification(context: Context, prospekId: String, nama: String) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("prospek_id", prospekId)
        }
        val pi = PendingIntent.getActivity(
            context,
            prospekId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Jamaah Wajib difollow Up Sekarang")
            .setContentText(nama)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(prospekId.hashCode(), notif)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS belum granted
        }
    }
}
