package com.imtiyaztour.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
    var kapasitas by remember { mutableStateOf("2") }
    var lantaiAwal by remember { mutableStateOf("1") }
    var lantaiAkhir by remember { mutableStateOf("3") }
    var kamarPerLantai by remember { mutableStateOf("5") }
    var genderLock by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    // Saat tipe berubah, auto-update kapasitas
    LaunchedEffect(tipe) {
        kapasitas = roomCapacityFor(tipe).toString()
    }

    val totalKamar = (lantaiAkhir.toIntOrNull() ?: 0) - (lantaiAwal.toIntOrNull() ?: 0) + 1
    val totalKamars = if (totalKamar > 0) totalKamar * (kamarPerLantai.toIntOrNull() ?: 0) else 0

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
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Tipe Kamar", fontSize = 12.sp, color = AdminTextGray)
            Spacer(Modifier.height(4.dp))
            Row {
                listOf("single", "double", "triple", "quad").forEach { t ->
                    FilterChip(
                        selected = tipe == t,
                        onClick = { tipe = t },
                        label = { Text(t) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Gender", fontSize = 12.sp, color = AdminTextGray)
            Spacer(Modifier.height(4.dp))
            Row {
                listOf(null to "Mixed", "L" to "Pria", "P" to "Wanita").forEach { (g, label) ->
                    FilterChip(
                        selected = genderLock == g,
                        onClick = { genderLock = g },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row {
                OutlinedTextField(
                    value = lantaiAwal, onValueChange = { lantaiAwal = it.filter { c -> c.isDigit() } },
                    label = { Text("Lantai Awal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp), singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = lantaiAkhir, onValueChange = { lantaiAkhir = it.filter { c -> c.isDigit() } },
                    label = { Text("Lantai Akhir") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp), singleLine = true
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = kamarPerLantai, onValueChange = { kamarPerLantai = it.filter { c -> c.isDigit() } },
                label = { Text("Kamar per Lantai") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp), singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Text("Total: $totalKamars kamar akan dibuat",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
            }
            if (errorMsg.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(errorMsg, color = AdminDanger, fontSize = 12.sp)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val l1 = lantaiAwal.toIntOrNull() ?: 0
                    val l2 = lantaiAkhir.toIntOrNull() ?: 0
                    val kpl = kamarPerLantai.toIntOrNull() ?: 0
                    val kap = kapasitas.toIntOrNull() ?: 0
                    if (l1 < 1 || l2 < l1 || kpl < 1) {
                        errorMsg = "Data lantai tidak valid"; return@Button
                    }
                    if (totalKamars > 200) {
                        errorMsg = "Maksimal 200 kamar sekali generate"; return@Button
                    }
                    isLoading = true; errorMsg = ""
                    scope.launch {
                        try {
                            val rooms = mutableListOf<Room>()
                            for (lantai in l1..l2) {
                                for (i in 1..kpl) {
                                    rooms.add(Room(
                                        nomor = "${lantai}${i.toString().padStart(2, '0')}",
                                        lantai = lantai,
                                        tipe = tipe,
                                        kapasitas = kap,
                                        gender_lock = genderLock
                                    ))
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
                else Text("Generate $totalKamars Kamar", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
