package com.imtiyaztour.admin

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object ProspekReminderScheduler {
    private const val TAG = "ProspekReminder"
    private const val REQ_CODE_BASE = 7000

    fun scheduleExact(context: Context, prospekId: String, nama: String, fuDateMillis: Long) {
        if (fuDateMillis <= System.currentTimeMillis()) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarm not permitted, skip schedule for $prospekId")
            return
        }
        val intent = Intent(context, ProspekReminderReceiver::class.java).apply {
            putExtra("prospek_id", prospekId)
            putExtra("nama", nama)
            putExtra("fu_date", fuDateMillis)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            reqCode(prospekId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fuDateMillis, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, fuDateMillis, pi)
        }
        Log.d(TAG, "Scheduled exact alarm for $prospekId at $fuDateMillis")
    }
    fun cancel(context: Context, prospekId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ProspekReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            reqCode(prospekId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
        Log.d(TAG, "Cancelled alarm for $prospekId")
    }

    private fun reqCode(id: String): Int = REQ_CODE_BASE + (id.hashCode() and 0x7FFFFFFF)
}
