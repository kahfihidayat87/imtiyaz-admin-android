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

/**
 * Fase 3 D3 (rev2): Generate kamar berdasarkan JUMLAH KAMAR (bukan lantai).
 * Tipe: single (1) · double (2) · triple (3) · quad (4) · quint (5)
 * Lantai + nomor kamar auto-distribute berdasarkan "kamar per lantai".
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
    var tipe by remember { mutableStateOf("double") }
    var genderLock by remember { mutableStateOf<String?>(null) }
    var jumlahKamar by remember { mutableStateOf("") }
    var kamarPerLantai by remember { mutableStateOf("10") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val tipeList = listOf("single", "double", "triple", "quad", "quint")
    val totalKamar = jumlahKamar.toIntOrNull() ?: 0
    val kpl = kamarPerLantai.toIntOrNull() ?: 10
    val kapasitas = roomCapacityFor(tipe)
    val lantaiCount = if (kpl > 0 && totalKamar > 0) {
        (totalKamar + kpl - 1) / kpl
    } else 0
    val totalPax = totalKamar * kapasitas

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate Kamar", fontWeight = FontWeight.Bold, color = AdminPrimary) },
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
            // ============ TIPE KAMAR ============
            Text("Tipe Kamar", fontSize = 12.sp, color = AdminTextGray)
            Spacer(Modifier.height(6.dp))
            // Row 1: single, double, triple
            Row {
                tipeList.take(3).forEach { t ->
                    FilterChip(
                        selected = tipe == t,
                        onClick = { tipe = t },
                        label = { Text("$t (${roomCapacityFor(t)})", fontSize = 11.sp) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            // Row 2: quad, quint
            Row {
                tipeList.drop(3).forEach { t ->
                    FilterChip(
                        selected = tipe == t,
                        onClick = { tipe = t },
                        label = { Text("$t (${roomCapacityFor(t)})", fontSize = 11.sp) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ============ GENDER ============
            Text("Gender Kamar", fontSize = 12.sp, color = AdminTextGray)
            Spacer(Modifier.height(6.dp))
            Row {
                listOf(null to "Mixed", "L" to "Pria", "P" to "Wanita").forEach { (g, label) ->
                    FilterChip(
                        selected = genderLock == g,
                        onClick = { genderLock = g },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ============ JUMLAH KAMAR ============
            OutlinedTextField(
                value = jumlahKamar,
                onValueChange = { jumlahKamar = it.filter { c -> c.isDigit() } },
                label = { Text("Jumlah Kamar *") },
                placeholder = { Text("misal: 30") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp), singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = kamarPerLantai,
                onValueChange = { kamarPerLantai = it.filter { c -> c.isDigit() } },
                label = { Text("Kamar per Lantai") },
                placeholder = { Text("default 10") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp), singleLine = true
            )

            // ============ PREVIEW ============
            if (totalKamar > 0) {
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Preview", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("• $totalKamar kamar $tipe ($kapasitas org/kamar)",
                            fontSize = 12.sp, color = Color(0xFF1565C0))
                        Text("• Total kapasitas: $totalPax orang",
                            fontSize = 12.sp, color = Color(0xFF1565C0))
                        Text("• Auto-distribute ke $lantaiCount lantai ($kpl kamar/lantai)",
                            fontSize = 12.sp, color = Color(0xFF1565C0))
                        Text("• Nomor: ${if (lantaiCount > 0) "101-" + (100 + kpl) else "-"} → ${lantaiCount}01-${lantaiCount}${kpl.toString().padStart(2, '0')}",
                            fontSize = 11.sp, color = Color(0xFF1976D2))
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
                    if (totalKamar < 1) { errorMsg = "Jumlah kamar wajib diisi"; return@Button }
                    if (totalKamar > 200) { errorMsg = "Maksimal 200 kamar sekali generate"; return@Button }
                    if (kpl < 1) { errorMsg = "Kamar per lantai minimal 1"; return@Button }

                    isLoading = true; errorMsg = ""
                    scope.launch {
                        try {
                            val rooms = mutableListOf<Room>()
                            var lantai = 1
                            var indexDiLantai = 1
                            for (i in 1..totalKamar) {
                                val nomor = "${lantai}${indexDiLantai.toString().padStart(2, '0')}"
                                rooms.add(Room(
                                    nomor = nomor,
                                    lantai = lantai,
                                    tipe = tipe,
                                    kapasitas = kapasitas,
                                    gender_lock = genderLock
                                ))
                                indexDiLantai++
                                if (indexDiLantai > kpl) {
                                    lantai++
                                    indexDiLantai = 1
                                }
                            }
                            withContext(Dispatchers.IO) {
                                TripRepository.addRoomsBatch(tripId, hotelId, rooms)
                            }
                            onSuccess()
                        } catch (e: Exception) {
                            errorMsg = "Gagal: ${e.message ?: "unknown"}"
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp),
                    color = Color.White, strokeWidth = 2.dp)
                else Text(
                    if (totalKamar > 0) "Generate $totalKamar Kamar" else "Generate Kamar",
                    fontWeight = FontWeight.Bold, color = Color.White
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
