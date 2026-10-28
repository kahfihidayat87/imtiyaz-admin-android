package com.imtiyaztour.admin

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object KantorRepository {
    private val col = FirebaseFirestore.getInstance().collection("kantor_operasional")

    fun observeAll(): Flow<List<Kantor>> = callbackFlow {
        val listener = col.orderBy("urutan", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { it.toObject(Kantor::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun save(k: Kantor): Result<Unit> = runCatching {
        val data = mapOf(
            "nama" to k.nama,
            "alamat" to k.alamat,
            "telepon" to k.telepon,
            "urutan" to k.urutan
        )
        if (k.id.isEmpty()) col.add(data).await()
        else col.document(k.id).set(data).await()
        Unit
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        col.document(id).delete().await()
        Unit
    }
}
