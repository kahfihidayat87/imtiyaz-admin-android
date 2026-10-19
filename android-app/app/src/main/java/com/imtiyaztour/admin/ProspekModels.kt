package com.imtiyaztour.admin

import android.net.Uri
import com.google.firebase.firestore.DocumentId

enum class UmrahUntuk(val label: String) {
    DIRI_SENDIRI("Untuk diri sendiri"),
    MENGUMRAHKAN("Mengumrahkan");

    companion object {
        fun fromValue(v: String?): UmrahUntuk =
            values().firstOrNull { it.name == v } ?: DIRI_SENDIRI
        fun labels(): List<String> = values().map { it.label }
        fun fromLabel(label: String): UmrahUntuk =
            values().firstOrNull { it.label == label } ?: DIRI_SENDIRI
    }
}

enum class ProspekStatus(val label: String) {
    BARU("Baru"),
    FU_DIJADWALKAN("FU Dijadwalkan"),
    SUDAH_FU("Sudah di FU");
    companion object {
        fun fromValue(v: String?): ProspekStatus =
            values().firstOrNull { it.name == v } ?: BARU
    }
}

data class Prospek(
    @DocumentId val id: String = "",
    val nama: String = "",
    val kota: String = "",
    val no_wa: String = "",
    val umrah_untuk: String = UmrahUntuk.DIRI_SENDIRI.name,
    val tgl_kantor: Long? = null,
    val catatan: String = "",
    val status: String = ProspekStatus.BARU.name,
    val fu_date: Long? = null,
    val fu_notes: String? = null,
    val created_by: String = "",
    val created_at: Long = 0L,
    val updated_at: Long = 0L
) {
    fun umrahUntukEnum(): UmrahUntuk = UmrahUntuk.fromValue(umrah_untuk)
    fun statusEnum(): ProspekStatus = ProspekStatus.fromValue(status)
    fun waLink(): String {
        val cleaned = no_wa.filter { it.isDigit() }
        val normalized = when {
            cleaned.startsWith("62") -> cleaned
            cleaned.startsWith("0") -> "62" + cleaned.substring(1)
            cleaned.startsWith("8") -> "62" + cleaned
            else -> cleaned
        }
        val text = "Assalamu'alaikum ${nama.ifBlank { "Bapak/Ibu" }}, kami dari Imtiyaz Tour ingin menindaklanjuti rencana umrahnya njih.."
        return "https://wa.me/$normalized?text=${Uri.encode(text)}"
    }
}
