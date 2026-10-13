package com.imtiyaztour.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Fase 3 D3 Flow 2: Input pax saja (L/P), sistem hitung kebutuhan kamar otomatis.
 * Rule (prioritas): quad (4) → triple (3) → double (2) → single (1).
 * Tanpa tipe quint.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateKamarAutoScreen(
    tripId: String,
    hotelId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var paxL by remember { mutableStateOf("") }
    var paxP by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val nL = paxL.toIntOrNull() ?: 0
    val nP = paxP.toIntOrNull() ?: 0
    val totalPax = nL + nP

    val roomsL = hitungKamarOtomatis(nL)
    val roomsP = hitungKamarOtomatis(nP)
    val totalKamar = roomsL.size + roomsP.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto-Generate Kamar", fontWeight = FontWeight.Bold, color = AdminPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AdminPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Jumlah Jamaah", fontWeight = FontWeight.Bold, color = AdminPrimary, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = paxL,
                    onValueChange = { paxL = it.filter { c -> c.isDigit() } },
                    label = { Text("Laki-laki (pax)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp), singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = paxP,
                    onValueChange = { paxP = it.filter { c -> c.isDigit() } },
                    label = { Text("Perempuan (pax)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp), singleLine = true
                )
            }
            Spacer(Modifier.height(6.dp))
            Text("Total: $totalPax pax", fontSize = 12.sp, color = AdminTextGray)

            if (totalPax > 0) {
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Preview Kebutuhan Kamar",
                            fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A), fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Laki-laki ($nL pax): ${formatKebutuhan(roomsL)}",
                            fontSize = 12.sp, color = Color(0xFF0F7A5A))
                        Text("Perempuan ($nP pax): ${formatKebutuhan(roomsP)}",
                            fontSize = 12.sp, color = Color(0xFF0F7A5A))
                        Spacer(Modifier.height(6.dp))
                        Divider(color = Color(0xFF0F7A5A).copy(alpha = 0.3f))
                        Spacer(Modifier.height(6.dp))
                        Text("Total: $totalKamar kamar untuk $totalPax pax",
                            fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A), fontSize = 13.sp)
                    }
                }
            }

            if (errorMsg.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(errorMsg, color = AdminDanger, fontSize = 12.sp)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (totalPax < 1) { errorMsg = "Jumlah jamaah wajib diisi"; return@Button }
                    if (totalKamar > 300) { errorMsg = "Kamar terlalu banyak (>300)"; return@Button }

                    isLoading = true; errorMsg = ""
                    scope.launch {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val roomsCol = db.collection("trips").document(tripId)
                                .collection("hotels").document(hotelId).collection("rooms")
                            val manifestCol = db.collection("trips").document(tripId)
                                .collection("manifest")
                            val tripRef = db.collection("trips").document(tripId)
                            val batch = db.batch()

                            var nomorIdx = 0
                            var idxL = 0
                            var idxP = 0

                            fun nextNomor(): String {
                                nomorIdx++
                                val lantai = (nomorIdx - 1) / 10 + 1
                                val pos = (nomorIdx - 1) % 10 + 1
                                return "$lantai${pos.toString().padStart(2, '0')}"
                            }

                            // Helper: tambahkan kamar + penghuni ke batch
                            fun tambahKamar(tipe: String, gender: String) {
                                val kap = roomCapacityFor(tipe)
                                val nomor = nextNomor()
                                val roomRef = roomsCol.document()
                                val penghuni = mutableListOf<Map<String, Any>>()
                                repeat(kap) {
                                    val jid = if (gender == "L") { idxL++; "jamaah_L_$idxL" }
                                              else { idxP++; "jamaah_P_$idxP" }
                                    val nama = "Jamaah $gender-${if (gender == "L") idxL else idxP}"
                                    penghuni.add(mapOf(
                                        "jamaah_id" to jid, "nama" to nama,
                                        "usia" to 0, "gender" to gender
                                    ))
                                    batch.set(manifestCol.document(jid), mapOf(
                                        "nama" to nama, "usia" to 0,
                                        "gender" to gender,
                                        "keluarga_group" to jid,
                                        "kebutuhan_khusus" to emptyList<String>(),
                                        "pembayaran_status" to "cicilan",
                                        "sisa_bayar" to 0
                                    ))
                                }
                                batch.set(roomRef, mapOf(
                                    "nomor" to nomor,
                                    "lantai" to ((nomorIdx - 1) / 10 + 1),
                                    "tipe" to tipe,
                                    "kapasitas" to kap,
                                    "gender_lock" to gender,
                                    "kategori_prioritas" to null,
                                    "fasilitas" to emptyList<String>(),
                                    "penghuni" to penghuni
                                ))
                            }

                            // Generate kamar L dulu
                            roomsL.forEach { tipe -> tambahKamar(tipe, "L") }
                            // Lanjut kamar P
                            roomsP.forEach { tipe -> tambahKamar(tipe, "P") }

                            // Update jumlah_jamaah di trip
                            batch.set(tripRef, mapOf(
                                "jumlah_jamaah" to totalPax,
                                "updated_at" to System.currentTimeMillis()
                            ), SetOptions.merge())

                            withContext(Dispatchers.IO) { batch.commit().await() }
                            Toast.makeText(context, "Berhasil: $totalKamar kamar dibuat",
                                Toast.LENGTH_SHORT).show()
                            onSuccess()
                        } catch (e: Exception) {
                            errorMsg = "Gagal: ${e.message ?: "unknown"}"
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading && totalPax > 0,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp),
                    color = Color.White, strokeWidth = 2.dp)
                else Text("Generate $totalKamar Kamar & Assign $totalPax Pax",
                    fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Algoritma: quad → triple → double → single */
internal fun hitungKamarOtomatis(pax: Int): List<String> {
    val rooms = mutableListOf<String>()
    var sisa = pax
    while (sisa > 0) {
        when {
            sisa >= 4 -> { rooms.add("quad"); sisa -= 4 }
            sisa == 3 -> { rooms.add("triple"); sisa -= 3 }
            sisa == 2 -> { rooms.add("double"); sisa -= 2 }
            else -> { rooms.add("single"); sisa = 0 }
        }
    }
    return rooms
}

internal fun formatKebutuhan(rooms: List<String>): String {
    if (rooms.isEmpty()) return "0 kamar"
    val grouped = rooms.groupingBy { it }.eachCount()
    val order = listOf("quad", "triple", "double", "single")
    val parts = order.mapNotNull { t ->
        grouped[t]?.let { n -> "$n $t" }
    }
    return parts.joinToString(" + ") + " = ${rooms.size} kamar"
}
