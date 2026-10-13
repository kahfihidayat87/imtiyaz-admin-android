package com.imtiyaztour.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoAssignScreen(
    tripId: String,
    hotelId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var rooms by remember { mutableStateOf<List<Room>>(emptyList()) }
    var manifest by remember { mutableStateOf<List<JamaahManifest>>(emptyList()) }
    var result by remember { mutableStateOf<AssignResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    // Load rooms + manifest
    LaunchedEffect(tripId, hotelId) {
        isLoading = true
        try {
            val r = withContext(Dispatchers.IO) { TripRepository.getRooms(tripId, hotelId) }
            val m = withContext(Dispatchers.IO) { TripRepository.getManifest(tripId) }
            rooms = r
            manifest = m
            result = withContext(Dispatchers.IO) { RoomAssignEngine.assign(m, r) }
        } catch (e: Exception) {
            errorMsg = "Gagal load: ${e.message ?: "unknown"}"
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto-Assign Kamar", fontWeight = FontWeight.Bold, color = AdminPrimary) },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AdminPrimary)
                } },
                actions = {
                    if (result != null && !isLoading) {
                        IconButton(onClick = {
                            scope.launch {
                                isLoading = true
                                result = withContext(Dispatchers.IO) {
                                    RoomAssignEngine.assign(manifest, rooms)
                                }
                                isLoading = false
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Rerun", tint = AdminPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (result != null) {
                Surface(shadowElevation = 8.dp) {
                    Row(Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("Batal") }
                        Button(
                            onClick = {
                                isSaving = true
                                scope.launch {
                                    try {
                                        val roomsById = rooms.associateBy { it.id }
                                        withContext(Dispatchers.IO) {
                                            TripRepository.saveAssignment(tripId, hotelId, result!!, roomsById)
                                        }
                                        onSaved()
                                    } catch (e: Exception) {
                                        errorMsg = "Gagal simpan: ${e.message ?: "unknown"}"
                                    }
                                    isSaving = false
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.weight(2f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp),
                                color = Color.White, strokeWidth = 2.dp)
                            else Text("Simpan Assign", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AdminPrimary)
            }
            return@Scaffold
        }
        if (result == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMsg.ifEmpty { "Tidak ada data" }, color = AdminDanger)
            }
            return@Scaffold
        }

        val r = result!!
        val assignedCount = r.assignment.values.sumOf { it.size }

        LazyColumn(
            Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Ringkasan", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            color = Color(0xFF0F7A5A))
                        Spacer(Modifier.height(6.dp))
                        Text("✅ $assignedCount jamaah dapat kamar", fontSize = 12.sp)
                        if (r.unassigned.isNotEmpty()) {
                            Text("⚠ ${r.unassigned.size} jamaah belum dapat kamar",
                                fontSize = 12.sp, color = AdminDanger)
                        }
                        if (r.warnings.isNotEmpty()) {
                            Text("📋 ${r.warnings.size} warning", fontSize = 12.sp, color = Color(0xFFE65100))
                        }
                    }
                }
            }

            // Warnings
            if (r.warnings.isNotEmpty()) {
                item {
                    Text("Warnings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                items(r.warnings) { w ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Warning, contentDescription = null,
                                tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(w.message, fontSize = 12.sp, color = Color(0xFFE65100))
                        }
                    }
                }
            }

            // Per kamar
            item {
                Spacer(Modifier.height(4.dp))
                Text("Pembagian Kamar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            items(rooms.sortedBy { it.nomor }) { room ->
                val penghuni = r.assignment[room.id].orEmpty()
                RoomAssignCard(room, penghuni)
            }

            if (r.unassigned.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Belum Terassign", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = AdminDanger)
                }
                items(r.unassigned) { j ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                        Text("${j.nama} (${j.usia} th, ${j.gender})",
                            modifier = Modifier.padding(10.dp), fontSize = 12.sp,
                            color = AdminDanger)
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun RoomAssignCard(room: Room, penghuni: List<JamaahManifest>) {
    val occupied = penghuni.size
    val bgColor = when {
        occupied == 0 -> Color(0xFFF5F5F5)
        occupied >= room.kapasitas -> Color(0xFFE8F5E9)
        else -> Color(0xFFFFF8E1)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kamar ${room.nomor}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${occupied}/${room.kapasitas}",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = if (occupied >= room.kapasitas) Color(0xFF0F7A5A) else AdminTextGray)
            }
            Text("Lantai ${room.lantai}" + (room.gender_lock?.let { " · $it" } ?: ""),
                fontSize = 10.sp, color = AdminTextGray)
            if (penghuni.isEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Kosong", fontSize = 11.sp, color = AdminTextGray)
            } else {
                Spacer(Modifier.height(4.dp))
                penghuni.forEach { p ->
                    Text("• ${p.nama} (${p.usia} th, ${p.gender})",
                        fontSize = 11.sp, color = Color.DarkGray)
                }
            }
        }
    }
}
