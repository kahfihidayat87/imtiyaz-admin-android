package com.imtiyaztour.admin

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object ProspekRepository {

    private const val COL = "prospek"
    private fun col() = FirebaseFirestore.getInstance().collection(COL)

    fun observeAll(): Flow<List<Prospek>> = callbackFlow {
        val listener = col()
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("ProspekRepo", "Firestore listener error: ${err.message}", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(Prospek::class.java) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun observePendingFu(): Flow<List<Prospek>> = callbackFlow {
        val listener = col()
            .whereEqualTo("status", ProspekStatus.FU_DIJADWALKAN.name)
            .whereLessThanOrEqualTo("fu_date", System.currentTimeMillis())
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("ProspekRepo", "Firestore listener error: ${err.message}", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(Prospek::class.java) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun create(p: Prospek, createdBy: String): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val data = mapOf(
            "nama" to p.nama.trim(),
            "kota" to p.kota.trim(),
            "no_wa" to p.no_wa.trim(),
            "umrah_untuk" to p.umrah_untuk,
            "tgl_kantor" to p.tgl_kantor,
            "catatan" to p.catatan.trim(),
            "status" to ProspekStatus.BARU.name,
            "fu_date" to null,
            "fu_notes" to null,
            "created_by" to createdBy,
            "created_at" to now,
            "updated_at" to now
        )
        col().add(data).await().id
    }

    suspend fun updateFuDate(id: String, fuDate: Long): Result<Unit> = runCatching {
        col().document(id).update(
            mapOf(
                "fu_date" to fuDate,
                "status" to ProspekStatus.FU_DIJADWALKAN.name,
                "updated_at" to System.currentTimeMillis()
            )
        ).await()
        Unit
    }

    suspend fun updateStatusFu(id: String, notes: String, sudahFu: Boolean): Result<Unit> = runCatching {
        val newStatus = if (sudahFu) ProspekStatus.SUDAH_FU.name
                        else ProspekStatus.FU_DIJADWALKAN.name
        col().document(id).update(
            mapOf(
                "status" to newStatus,
                "fu_notes" to notes,
                "updated_at" to System.currentTimeMillis()
            )
        ).await()
        Unit
    }

    suspend fun update(id: String, p: Prospek): Result<Unit> = runCatching {
        col().document(id).update(
            mapOf(
                "nama" to p.nama.trim(),
                "kota" to p.kota.trim(),
                "no_wa" to p.no_wa.trim(),
                "umrah_untuk" to p.umrah_untuk,
                "tgl_kantor" to p.tgl_kantor,
                "catatan" to p.catatan.trim(),
                "updated_at" to System.currentTimeMillis()
            )
        ).await()
        Unit
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        col().document(id).delete().await()
        Unit
    }

    suspend fun getById(id: String): Prospek? = runCatching {
        col().document(id).get().await().toObject(Prospek::class.java)
    }.getOrNull()

    suspend fun getFirstDue(): Prospek? = runCatching {
        val snap = col()
            .whereEqualTo("status", ProspekStatus.FU_DIJADWALKAN.name)
            .limit(20)
            .get().await()
        snap.documents
            .mapNotNull { it.toObject(Prospek::class.java) }
            .filter { (it.fu_date ?: 0L) <= System.currentTimeMillis() }
            .minByOrNull { it.fu_date ?: Long.MAX_VALUE }
    }.getOrNull()
}
