package com.imtiyaztour.admin

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object CashflowRepository {
    private const val COL = "cashflow"
    private fun col() = FirebaseFirestore.getInstance().collection(COL)

    fun observeCashflow(akun: String): Flow<List<Cashflow>> = callbackFlow {
        val listener = col()
            .whereEqualTo("akun", akun)
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("CashflowRepo", "Firestore listener error: ${err.message}", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(Cashflow::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun create(p: Cashflow, createdBy: String): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val data = mapOf(
            "tipe" to p.tipe,
            "akun" to p.akun,
            "nominal" to p.nominal,
            "kategori" to p.kategori,
            "tanggal" to p.tanggal,
            "keterangan" to p.keterangan.trim(),
            "created_by" to createdBy,
            "created_at" to now,
            "updated_at" to now
        )
        col().add(data).await().id
    }

    suspend fun update(id: String, p: Cashflow): Result<Unit> = runCatching {
        col().document(id).update(
            mapOf(
                "tipe" to p.tipe,
                "akun" to p.akun,
                "nominal" to p.nominal,
                "kategori" to p.kategori,
                "tanggal" to p.tanggal,
                "keterangan" to p.keterangan.trim(),
                "updated_at" to System.currentTimeMillis()
            )
        ).await()
        Unit
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        col().document(id).delete().await()
        Unit
    }
}
