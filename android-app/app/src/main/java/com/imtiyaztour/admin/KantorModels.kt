package com.imtiyaztour.admin
import com.google.firebase.firestore.DocumentId

data class Kantor(
    @DocumentId val id: String = "",
    val nama: String = "",
    val alamat: String = "",
    val telepon: String = "",
    val urutan: Long = 0L
)
