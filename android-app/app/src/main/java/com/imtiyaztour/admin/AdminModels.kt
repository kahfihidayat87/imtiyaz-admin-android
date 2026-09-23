package com.imtiyaztour.admin

// Response dari /admin-login
data class AdminLoginRequest(val username: String, val password: String)

data class AdminLoginResponse(
    val success: Boolean? = null,
    val admin_id: Int? = null,
    val username: String? = null,
    val nama: String? = null,
    val role: String? = null,
    val kanal_id: String? = null,
    val token: String? = null,
    val error: String? = null
)

// Response dari /admin-dashboard
data class DashboardResponse(
    val success: Boolean? = null,
    val total_jamaah: Int = 0,
    val belum_lunas: Int = 0,
    val bukti_menunggu: Int = 0,
    val skrining_baru: Int = 0,
    val perlu_followup: Int = 0,
    val error: String? = null
)

// Data jamaah di list
data class JamaahSummary(
    val id: Int,
    val nama: String,
    val paket_id: String? = null,
    val kanal_id: String? = null,
    val is_tour_leader: Boolean = false,
    val total_tagihan: Long = 0L,
    val sudah_dibayar: Long = 0L,
    val sisa_tagihan: Long = 0L,
    val status_pembayaran: String = "Belum Lunas",
    val dokumen_lengkap: Int = 0,
    val dokumen_total: Int = 0
)

data class JamaahListResponse(
    val success: Boolean? = null,
    val total: Int = 0,
    val page: Int = 1,
    val per_page: Int = 50,
    val jamaah: List<JamaahSummary> = emptyList(),
    val error: String? = null
)

data class AdminAuth(val admin_id: Int, val token: String)

fun formatRupiah(v: Long): String {
    if (v <= 0L) return "Rp 0"
    val digits = v.toString()
    val sb = StringBuilder()
    for ((i, c) in digits.reversed().withIndex()) {
        if (i > 0 && i % 3 == 0) sb.append('.')
        sb.append(c)
    }
    return "Rp " + sb.reverse().toString()
}


// Response kirim pengumuman
data class AnnouncementResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val item: AnnouncementItem? = null,
    val error: String? = null
)

data class AnnouncementItem(
    val id: String? = null,
    val judul: String? = null,
    val pesan: String? = null,
    val tanggal: Long = 0L,
    val penting: Boolean = false,
    val dikirim_oleh: String? = null
)


data class SimpleResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val error: String? = null
)


data class KanalItem(
    val id: String,
    val nama: String,
    val total_jamaah: Int = 0,
    val ada_tl: Boolean = false
)

data class KanalListResponse(
    val success: Boolean? = null,
    val kanal: List<KanalItem> = emptyList(),
    val error: String? = null
)

data class KanalSaveResponse(
    val success: Boolean? = null,
    val kanal_id: String? = null,
    val nama: String? = null,
    val error: String? = null
)


data class BuktiItem(
    val id: Int,
    val nama: String,
    val paket_id: String? = null,
    val kanal_id: String? = null,
    val total_tagihan: Long = 0L,
    val sudah_dibayar: Long = 0L,
    val sisa_tagihan: Long = 0L,
    val status_pembayaran: String = "",
    val bukti_transfer: String? = null,
    val modified_at: Long = 0L
)

data class BuktiListResponse(
    val success: Boolean? = null,
    val total: Int = 0,
    val list: List<BuktiItem> = emptyList(),
    val error: String? = null
)
