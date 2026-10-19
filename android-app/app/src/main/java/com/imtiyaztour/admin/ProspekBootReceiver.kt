package com.imtiyaztour.admin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProspekBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != "android.intent.action.QUICKBOOT_POWERON") return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val snap = FirebaseFirestore.getInstance().collection("prospek")
                    .whereEqualTo("status", ProspekStatus.FU_DIJADWALKAN.name)
                    .whereGreaterThan("fu_date", now)
                    .get().await()
                snap.documents.forEach { doc ->
                    val id = doc.id
                    val nama = doc.getString("nama") ?: "Prospek"
                    val fu = doc.getLong("fu_date") ?: 0L
                    if (fu > now) {
                        ProspekReminderScheduler.scheduleExact(context, id, nama, fu)
                    }
                }
                Log.d("ProspekBoot", "Rescheduled ${snap.size()} alarms")
            } catch (e: Exception) {
                Log.e("ProspekBoot", "Error rescheduling", e)
            } finally {
                pending.finish()
            }
        }
    }
}
