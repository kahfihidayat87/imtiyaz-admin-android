package com.imtiyaztour.admin

import com.google.firebase.firestore.DocumentId

/**
 * Fase 3 D3: Model data untuk fitur Trip & Auto-assign Kamar.
 * Semua disimpan di Firestore, path: trips/{tripId}/...
 */

data class Trip(
    @DocumentId val id: String = "",
    val nama: String = "",
    val tanggal_berangkat: Long = 0L,
    val tanggal_pulang: Long = 0L,
    val jumlah_jamaah: Int = 0,
    val status: String = "planning",   // planning | berlangsung | selesai
    val catatan: String = "",
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis()
)

data class Hotel(
    @DocumentId val id: String = "",
    val nama: String = "",
    val tipe: String = "makkah",       // makkah | madinah
    val checkin: Long = 0L,
    val checkout: Long = 0L,
    val jumlah_lantai: Int = 0,
    val catatan: String = ""
)

data class Room(
    @DocumentId val id: String = "",
    val nomor: String = "",
    val lantai: Int = 1,
    val tipe: String = "double",        // single | double | triple | quad
    val kapasitas: Int = 2,
    val gender_lock: String? = null,    // L | P | null (null = flexible)
    val kategori_prioritas: String? = null, // lansia | keluarga | reguler
    val fasilitas: List<String> = emptyList(),
    val penghuni: List<Penghuni> = emptyList()
)

data class Penghuni(
    val jamaah_id: String = "",
    val nama: String = "",
    val usia: Int = 0,
    val gender: String = ""             // L | P
)

data class JamaahManifest(
    @DocumentId val id: String = "",    // = jamaah_id
    val nama: String = "",
    val usia: Int = 0,
    val gender: String = "",
    val keluarga_group: String = "",    // ID grup keluarga (bisa = jamaah_id kalau solo)
    val kebutuhan_khusus: List<String> = emptyList(), // kursi_roda, diabetes, hipertensi, dll
    val preferensi_kamar: String? = null,  // dekat_tl | lantai_rendah | null
    val pembayaran_status: String = "cicilan",
    val sisa_bayar: Long = 0L
)

// Hasil auto-assign (draft, sebelum disimpan ke rooms)
data class AssignResult(
    val assignment: Map<String, List<JamaahManifest>> = emptyMap(),  // roomId → jamaah
    val unassigned: List<JamaahManifest> = emptyList(),
    val warnings: List<AssignWarning> = emptyList()
)

data class AssignWarning(
    val type: String = "",              // mixed_gender | over_capacity | no_room | capacity_underused
    val room_id: String? = null,
    val jamaah_ids: List<String> = emptyList(),
    val message: String = ""
)

// Helper: label room type → kapasitas default
fun roomCapacityFor(tipe: String): Int = when (tipe.lowercase()) {
    "single" -> 1
    "double" -> 2
    "triple" -> 3
    "quad" -> 4
    "quint" -> 5
    else -> 2
}
