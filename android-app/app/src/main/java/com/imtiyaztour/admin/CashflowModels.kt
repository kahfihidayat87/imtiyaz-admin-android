package com.imtiyaztour.admin

import com.google.firebase.firestore.DocumentId

enum class CashflowTipe(val label: String) {
    MASUK("Masuk"),
    KELUAR("Keluar");

    companion object {
        fun fromValue(v: String?): CashflowTipe = values().firstOrNull { it.name == v } ?: MASUK
    }
}

enum class CashflowAkun(val label: String) {
    KAS_OPERASIONAL("Kas Operasional"),
    KAS_JAMAAH("Kas Jamaah");

    companion object {
        fun fromValue(v: String?): CashflowAkun = values().firstOrNull { it.name == v } ?: KAS_OPERASIONAL
    }
}

object CashflowKategori {
    val masuk = listOf("DP / Uang Muka", "Pelunasan", "Pendaftaran", "Lainnya (Masuk)")
    
    val keluarJamaah = listOf(
        "Tiket Domestik", "Tiket Internasional", "Visa", "Hotel", 
        "Handling", "Fee Muthawwif", "Fee TL", "Siskopatuh", 
        "Cek Kesehatan", "Lainnya (Jamaah)"
    )
    
    val keluarOperasional = listOf(
        "Gaji Karyawan", "Fee Marketing", "Biaya Iklan", "Pajak", 
        "Perlengkapan", "Operasional Kantor", "Lainnya (Operasional)"
    )

    fun getBy(akun: CashflowAkun, tipe: CashflowTipe): List<String> {
        return if (tipe == CashflowTipe.MASUK) {
            masuk
        } else {
            if (akun == CashflowAkun.KAS_OPERASIONAL) keluarOperasional else keluarJamaah
        }
    }
}

data class Cashflow(
    @DocumentId val id: String = "",
    val tipe: String = CashflowTipe.MASUK.name,
    val akun: String = CashflowAkun.KAS_OPERASIONAL.name,
    val nominal: Long = 0L,
    val kategori: String = "",
    val tanggal: Long = System.currentTimeMillis(),
    val keterangan: String = "",
    val created_by: String = "",
    val created_at: Long = 0L,
    val updated_at: Long = 0L
) {
    fun tipeEnum(): CashflowTipe = CashflowTipe.fromValue(tipe)
    fun akunEnum(): CashflowAkun = CashflowAkun.fromValue(akun)
}
