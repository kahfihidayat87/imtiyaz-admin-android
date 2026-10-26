package com.imtiyaztour.admin

import com.google.firebase.firestore.DocumentId

data class Kajian(
    @DocumentId val id: String = "",
    val tema: String = "",
    val pemateri: String = "",
    val tanggal: Long = 0L,
    val jam: String = "",
    val kode_gelombang: String = "",
    val created_at: Long = 0L
)
