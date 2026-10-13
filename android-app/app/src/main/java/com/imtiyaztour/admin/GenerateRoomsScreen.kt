package com.imtiyaztour.admin

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

/**
 * Fase 3 D3 rev3: Setup Kamar & Jamaah terintegrasi.
 * Admin input:
 *   1. Jumlah jamaah (L / P)
 *   2. Jumlah kamar per tipe (quint/quad/triple/double/single)
 * Klik Generate → kamar dibuat + jamaah placeholder di-assign otomatis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateRoomsScreen(
    tripId: String,
    hotelId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Jamaah
    var paxL by remember { mutableStateOf("") }
    var paxP by remember { mutableStateOf("") }

    // Kamar
    var quint by remember { mutableStateOf("") }
    var quad by remember { mutableStateOf("") }
    var triple by remember { mutableStateOf("") }
    var double by remember { mutableStateOf("") }
    var single by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val nPaxL = paxL.toIntOrNull() ?: 0
    val nPaxP = paxP.toIntOrNull() ?: 0
    val totalPax = nPaxL + nPaxP

    val nQuint = quint.toIntOrNull() ?: 0
    val nQuad = quad.toIntOrNull() ?: 0
    val nTriple = triple.toIntOrNull() ?: 0
    val nDouble = double.toIntOrNull() ?: 0
    val nSingle = single.toIntOrNull() ?: 0

    val totalKamar = nQuint + nQuad + nTriple + nDouble + nSingle
    val totalKapasitas = nQuint * 5 + nQuad * 4 + nTriple * 3 + nDouble * 2 + nSingle * 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Kamar & Jamaah", fontWeight = FontWeight.Bold, color = AdminPrimary) },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AdminPrimary)
                } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            // ==================== JAMAAH ====================
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

            Spacer(Modifier.height(20.dp))

            // ==================== KAMAR ====================
            Text("Jumlah Kamar Dipesan", fontWeight = FontWeight.Bold, color = AdminPrimary, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))

            RoomInputRow("Quint (5 pax)", quint, { quint = it })
            RoomInputRow("Quad (4 pax)", quad, { quad = it })
            RoomInputRow("Triple (3 pax)", triple, { triple = it })
            RoomInputRow("Double (2 pax)", double, { double = it })
            RoomInputRow("Single (1 pax)", single, { single = it })

            Spacer(Modifier.height(8.dp))
            Text("Total: $totalKamar kamar · kapasitas $totalKapasitas pax",
                fontSize = 12.sp, color = AdminTextGray)

            // ==================== PREVIEW ====================
            if (totalPax > 0 && totalKamar > 0) {
                Spacer(Modifier.height(16.dp))
                val sisa = totalKapasitas - totalPax
                val cukup = sisa >= 0
                Card(colors = CardDefaults.cardColors(
                    containerColor = if (cukup) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )) {
                    Column(Modifier.padding(12.dp)) {
                        Text(if (cukup) "✓ Kapasitas cukup" else "✗ Kapasitas kurang",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            color = if (cukup) Color(0xFF0F7A5A) else AdminDanger)
                        Spacer(Modifier.height(4.dp))
                        Text("• Total pax: $totalPax (L: $nPaxL, P: $nPaxP)", fontSize = 12.sp)
                        Text("• Kapasitas: $totalKapasitas pax", fontSize = 12.sp)
                        Text("• Sisa kursi: $sisa", fontSize = 12.sp,
                            color = if (cukup) Color(0xFF0F7A5A) else AdminDanger)
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
                    if (totalKamar < 1) { errorMsg = "Jumlah kamar wajib diisi"; return@Button }
                    if (totalKapasitas < totalPax) {
                        errorMsg = "Kapasitas kurang ${totalPax - totalKapasitas} pax"; return@Button
                    }
                    if (totalKamar > 300) { errorMsg = "Maksimal 300 kamar"; return@Button }

                    isLoading = true; errorMsg = ""
                    scope.launch {
                        try {
                            // 1. Generate room list (dari besar ke kecil, tapi order di Firestore bebas)
                            val newRooms = mutableListOf<Room>()
                            var lantai = 1
                            var idxLantai = 1
                            val kpl = 10  // fixed 10 kamar/lantai
                            fun addRooms(tipe: String, count: Int) {
                                repeat(count) {
                                    val nomor = "${lantai}${idxLantai.toString().padStart(2, '0')}"
                                    newRooms.add(Room(
                                        nomor = nomor, lantai = lantai,
                                        tipe = tipe, kapasitas = roomCapacityFor(tipe)
                                    ))
                                    idxLantai++
                                    if (idxLantai > kpl) { lantai++; idxLantai = 1 }
                                }
                            }
                            addRooms("quint", nQuint)
                            addRooms("quad", nQuad)
                            addRooms("triple", nTriple)
                            addRooms("double", nDouble)
                            addRooms("single", nSingle)

                            // 2. Simpan rooms ke Firestore, dapatkan list dengan ID
                            val savedRooms = withContext(Dispatchers.IO) {
                                val col = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("trips").document(tripId)
                                    .collection("hotels").document(hotelId)
                                    .collection("rooms")
                                val result = mutableListOf<Room>()
                                // batch
                                val batch = com.google.firebase.firestore.FirebaseFirestore.getInstance().batch()
                                newRooms.forEach { r ->
                                    val ref = col.document()
                                    batch.set(ref, mapOf(
                                        "nomor" to r.nomor, "lantai" to r.lantai,
                                        "tipe" to r.tipe, "kapasitas" to r.kapasitas,
                                        "gender_lock" to null, "kategori_prioritas" to null,
                                        "fasilitas" to emptyList<String>(),
                                        "penghuni" to emptyList<Map<String, Any>>()
                                    ))
                                    result.add(r.copy(id = ref.id))
                                }
                                batch.commit().await()
                                result
                            }

                            // 3. Auto-assign
                            val (assignMap, warnings) = RoomAssignEngine.assignByCount(
                                paxL = nPaxL, paxP = nPaxP, rooms = savedRooms
                            )

                            // 4. Simpan assignment ke Firestore (batch) + simpan manifest placeholder
                            withContext(Dispatchers.IO) {
                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val roomsCol = db.collection("trips").document(tripId)
                                    .collection("hotels").document(hotelId).collection("rooms")
                                val manifestCol = db.collection("trips").document(tripId)
                                    .collection("manifest")
                                val batch = db.batch()

                                // Update penghuni per kamar
                                assignMap.forEach { (roomId, list) ->
                                    val data = list.map {
                                        mapOf(
                                            "jamaah_id" to it.id, "nama" to it.nama,
                                            "usia" to it.usia, "gender" to it.gender
                                        )
                                    }
                                    batch.set(roomsCol.document(roomId),
                                        mapOf("penghuni" to data),
                                        com.google.firebase.firestore.SetOptions.merge())
                                }

                                // Manifest placeholder
                                assignMap.values.flatten().forEach { j ->
                                    batch.set(manifestCol.document(j.id), mapOf(
                                        "nama" to j.nama, "usia" to j.usia,
                                        "gender" to j.gender,
                                        "keluarga_group" to j.keluarga_group,
                                        "kebutuhan_khusus" to emptyList<String>(),
                                        "pembayaran_status" to "cicilan",
                                        "sisa_bayar" to 0
                                    ))
                                }

                                // Update trip jumlah_jamaah
                                val tripRef = db.collection("trips").document(tripId)
                                batch.set(tripRef, mapOf(
                                    "jumlah_jamaah" to totalPax,
                                    "updated_at" to System.currentTimeMillis()
                                ), com.google.firebase.firestore.SetOptions.merge())

                                batch.commit().await()
                            }

                            // 5. Warning kalau ada
                            if (warnings.isNotEmpty()) {
                                errorMsg = warnings.joinToString("; ") { it.message }
                                isLoading = false
                                return@launch
                            }

                            onSuccess()
                        } catch (e: Exception) {
                            errorMsg = "Gagal: ${e.message ?: "unknown"}"
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading && totalPax > 0 && totalKamar > 0,
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

@Composable
private fun RoomInputRow(label: String, value: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = value, onValueChange = { onChange(it.filter { c -> c.isDigit() }) },
            placeholder = { Text("0", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(90.dp),
            shape = RoundedCornerShape(8.dp), singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        )
    }
}
