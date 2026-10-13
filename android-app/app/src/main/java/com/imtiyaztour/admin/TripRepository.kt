package com.imtiyaztour.admin

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Fase 3 D3: Repository untuk semua akses Firestore terkait Trip.
 * Path: trips/{tripId}/...
 */
object TripRepository {
    private const val TAG = "TripRepository"
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private val tripsCol get() = db.collection("trips")

    // ============ TRIP ============
    fun observeTrips(): Flow<List<Trip>> = callbackFlow {
        val reg = tripsCol
            .orderBy("tanggal_berangkat", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "observeTrips error: ${err.message}")
                    trySend(emptyList()); return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(Trip::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun getTrip(id: String): Trip? {
        return try {
            tripsCol.document(id).get().await().toObject(Trip::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "getTrip error: ${e.message}"); null
        }
    }

    suspend fun createTrip(trip: Trip): String {
        val ref = tripsCol.document()
        val data = mapOf(
            "nama" to trip.nama,
            "tanggal_berangkat" to trip.tanggal_berangkat,
            "tanggal_pulang" to trip.tanggal_pulang,
            "jumlah_jamaah" to trip.jumlah_jamaah,
            "status" to trip.status,
            "catatan" to trip.catatan,
            "created_at" to System.currentTimeMillis(),
            "updated_at" to System.currentTimeMillis()
        )
        ref.set(data).await()
        return ref.id
    }

    suspend fun updateTrip(id: String, fields: Map<String, Any>) {
        val data = fields.toMutableMap()
        data["updated_at"] = System.currentTimeMillis()
        tripsCol.document(id).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun deleteTrip(id: String) {
        // delete hotel + manifest sub-collections dulu (Firestore tidak cascade)
        try {
            getHotels(id).forEach { h -> deleteHotel(id, h.id) }
            getManifest(id).forEach { m -> tripsCol.document(id).collection("manifest").document(m.id).delete().await() }
        } catch (e: Exception) {
            Log.w(TAG, "cleanup sub-collections error: ${e.message}")
        }
        tripsCol.document(id).delete().await()
    }

    // ============ HOTEL ============
    fun observeHotels(tripId: String): Flow<List<Hotel>> = callbackFlow {
        val reg = tripsCol.document(tripId).collection("hotels")
            .orderBy("checkin", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { it.toObject(Hotel::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun getHotels(tripId: String): List<Hotel> {
        return try {
            tripsCol.document(tripId).collection("hotels").get().await()
                .documents.mapNotNull { it.toObject(Hotel::class.java) }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addHotel(tripId: String, hotel: Hotel): String {
        val ref = tripsCol.document(tripId).collection("hotels").document()
        val data = mapOf(
            "nama" to hotel.nama,
            "tipe" to hotel.tipe,
            "checkin" to hotel.checkin,
            "checkout" to hotel.checkout,
            "jumlah_lantai" to hotel.jumlah_lantai,
            "catatan" to hotel.catatan
        )
        ref.set(data).await()
        return ref.id
    }

    suspend fun updateHotel(tripId: String, hotelId: String, fields: Map<String, Any>) {
        tripsCol.document(tripId).collection("hotels").document(hotelId)
            .set(fields, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun deleteHotel(tripId: String, hotelId: String) {
        // delete rooms dulu
        try {
            getRooms(tripId, hotelId).forEach { r ->
                tripsCol.document(tripId).collection("hotels").document(hotelId)
                    .collection("rooms").document(r.id).delete().await()
            }
        } catch (e: Exception) { }
        tripsCol.document(tripId).collection("hotels").document(hotelId).delete().await()
    }

    // ============ ROOM ============
    fun observeRooms(tripId: String, hotelId: String): Flow<List<Room>> = callbackFlow {
        val reg = tripsCol.document(tripId).collection("hotels").document(hotelId)
            .collection("rooms")
            .orderBy("lantai", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { it.toObject(Room::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun getRooms(tripId: String, hotelId: String): List<Room> {
        return try {
            tripsCol.document(tripId).collection("hotels").document(hotelId)
                .collection("rooms").get().await()
                .documents.mapNotNull { it.toObject(Room::class.java) }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addRoom(tripId: String, hotelId: String, room: Room): String {
        val ref = tripsCol.document(tripId).collection("hotels").document(hotelId).collection("rooms").document()
        val data = mapOf(
            "nomor" to room.nomor,
            "lantai" to room.lantai,
            "tipe" to room.tipe,
            "kapasitas" to room.kapasitas,
            "gender_lock" to room.gender_lock,
            "kategori_prioritas" to room.kategori_prioritas,
            "fasilitas" to room.fasilitas,
            "penghuni" to emptyList<Map<String, Any>>()
        )
        ref.set(data).await()
        return ref.id
    }

    /** Batch add — untuk form "generate 10 kamar double lantai 1-3". */
    suspend fun addRoomsBatch(tripId: String, hotelId: String, rooms: List<Room>): Int {
        val col = tripsCol.document(tripId).collection("hotels").document(hotelId).collection("rooms")
        val batch = db.batch()
        rooms.forEach { room ->
            val ref = col.document()
            batch.set(ref, mapOf(
                "nomor" to room.nomor,
                "lantai" to room.lantai,
                "tipe" to room.tipe,
                "kapasitas" to room.kapasitas,
                "gender_lock" to room.gender_lock,
                "kategori_prioritas" to room.kategori_prioritas,
                "fasilitas" to room.fasilitas,
                "penghuni" to emptyList<Map<String, Any>>()
            ))
        }
        batch.commit().await()
        return rooms.size
    }

    suspend fun updateRoomPenghuni(tripId: String, hotelId: String, roomId: String, penghuni: List<Penghuni>) {
        val data = penghuni.map {
            mapOf("jamaah_id" to it.jamaah_id, "nama" to it.nama, "usia" to it.usia, "gender" to it.gender)
        }
        tripsCol.document(tripId).collection("hotels").document(hotelId)
            .collection("rooms").document(roomId)
            .set(mapOf("penghuni" to data), com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun clearAllRoomPenghuni(tripId: String, hotelId: String) {
        val rooms = getRooms(tripId, hotelId)
        rooms.forEach { r -> updateRoomPenghuni(tripId, hotelId, r.id, emptyList()) }
    }

    /** Simpan hasil auto-assign ke semua kamar sekaligus (batch). */
    suspend fun saveAssignment(tripId: String, hotelId: String, result: AssignResult, roomsById: Map<String, Room>) {
        val batch = db.batch()
        val col = tripsCol.document(tripId).collection("hotels").document(hotelId).collection("rooms")

        // Clear semua dulu
        roomsById.keys.forEach { roomId ->
            batch.set(col.document(roomId),
                mapOf("penghuni" to emptyList<Map<String, Any>>()),
                com.google.firebase.firestore.SetOptions.merge())
        }

        // Assign
        result.assignment.forEach { (roomId, jamaahList) ->
            val data = jamaahList.map {
                mapOf("jamaah_id" to it.id, "nama" to it.nama, "usia" to it.usia, "gender" to it.gender)
            }
            batch.set(col.document(roomId),
                mapOf("penghuni" to data),
                com.google.firebase.firestore.SetOptions.merge())
        }

        batch.commit().await()
    }

    // ============ MANIFEST ============
    fun observeManifest(tripId: String): Flow<List<JamaahManifest>> = callbackFlow {
        val reg = tripsCol.document(tripId).collection("manifest")
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { it.toObject(JamaahManifest::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun getManifest(tripId: String): List<JamaahManifest> {
        return try {
            tripsCol.document(tripId).collection("manifest").get().await()
                .documents.mapNotNull { it.toObject(JamaahManifest::class.java) }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addOrUpdateManifest(tripId: String, m: JamaahManifest) {
        val data = mapOf(
            "nama" to m.nama,
            "usia" to m.usia,
            "gender" to m.gender,
            "keluarga_group" to m.keluarga_group.ifBlank { m.id },
            "kebutuhan_khusus" to m.kebutuhan_khusus,
            "preferensi_kamar" to m.preferensi_kamar,
            "pembayaran_status" to m.pembayaran_status,
            "sisa_bayar" to m.sisa_bayar
        )
        tripsCol.document(tripId).collection("manifest").document(m.id)
            .set(data, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun saveManifestBatch(tripId: String, list: List<JamaahManifest>): Int {
        if (list.isEmpty()) return 0
        val col = tripsCol.document(tripId).collection("manifest")
        // Firestore batch limit 500 — chunk
        var count = 0
        list.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { m ->
                val ref = col.document(m.id)
                batch.set(ref, mapOf(
                    "nama" to m.nama,
                    "usia" to m.usia,
                    "gender" to m.gender,
                    "keluarga_group" to m.keluarga_group.ifBlank { m.id },
                    "kebutuhan_khusus" to m.kebutuhan_khusus,
                    "preferensi_kamar" to m.preferensi_kamar,
                    "pembayaran_status" to m.pembayaran_status,
                    "sisa_bayar" to m.sisa_bayar
                ), com.google.firebase.firestore.SetOptions.merge())
            }
            batch.commit().await()
            count += chunk.size
        }
        return count
    }

    suspend fun updateManifestField(tripId: String, jamaahId: String, fields: Map<String, Any>) {
        tripsCol.document(tripId).collection("manifest").document(jamaahId)
            .set(fields, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun deleteManifest(tripId: String, jamaahId: String) {
        tripsCol.document(tripId).collection("manifest").document(jamaahId).delete().await()
    }
}
