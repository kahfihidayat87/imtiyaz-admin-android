package com.imtiyaztour.admin

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object KajianRepository {
    private const val COL = "kajian"
    private fun col() = FirebaseFirestore.getInstance().collection(COL)

    fun observeAll(): Flow<List<Kajian>> = callbackFlow {
        val listener = col().orderBy("tanggal", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.toObject(Kajian::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun create(p: Kajian): Result<String> = runCatching {
        val data = mapOf(
            "tema" to p.tema.trim(),
            "pemateri" to p.pemateri.trim(),
            "tanggal" to p.tanggal,
            "jam" to p.jam.trim(),
            "kode_gelombang" to p.kode_gelombang.trim().uppercase(),
            "created_at" to System.currentTimeMillis()
        )
        col().add(data).await().id
    }

    suspend fun update(id: String, k: Kajian): Result<Unit> = runCatching {
    FirebaseFirestore.getInstance().collection("kajian").document(id).update(
        mapOf(
            "tema" to k.tema,
            "pemateri" to k.pemateri,
            "tanggal" to k.tanggal,
            "jam" to k.jam,
            "kode_gelombang" to k.kode_gelombang,
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
