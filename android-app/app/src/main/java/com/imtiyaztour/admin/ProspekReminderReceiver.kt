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

class ProspekReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prospekId = intent.getStringExtra("prospek_id") ?: return
        val nama = intent.getStringExtra("nama") ?: "Prospek"
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = FirebaseFirestore.getInstance().collection("prospek").document(prospekId).get().await()
                val status = doc.getString("status")
                val fuDate = doc.getLong("fu_date") ?: 0L
                if (status == ProspekStatus.FU_DIJADWALKAN.name && fuDate <= System.currentTimeMillis() + 60_000) {
                    ProspekNotificationHelper.showFuNotification(context, prospekId, nama)
                } else {
                    Log.d("ProspekReminder", "Skip notif for $prospekId status=$status")
                }
            } catch (e: Exception) {
                Log.e("ProspekReminder", "Error checking prospek $prospekId", e)
                ProspekNotificationHelper.showFuNotification(context, prospekId, nama)
            } finally {
                pending.finish()
            }
        }
    }
}
