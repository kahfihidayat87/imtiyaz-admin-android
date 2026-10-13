package com.imtiyaztour.admin

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelRoomScreen(
    tripId: String,
    hotel: Hotel,
    onBack: () -> Unit,
    onRoomClick: (Room) -> Unit,
    onGenerateRooms: () -> Unit,
    onAutoAssign: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val rooms by TripRepository.observeRooms(tripId, hotel.id)
        .collectAsState(initial = emptyList())

    var trip by remember { mutableStateOf<Trip?>(null) }
    var editingRoom by remember { mutableStateOf<Room?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    LaunchedEffect(tripId) {
        trip = withContext(Dispatchers.IO) { TripRepository.getTrip(tripId) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(hotel.nama, fontWeight = FontWeight.Bold, color = AdminPrimary,
                            maxLines = 1)
                        Text("${rooms.size} kamar · ${hotel.tipe}",
                            fontSize = 10.sp, color = AdminTextGray)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AdminPrimary)
                } },
                actions = {
                    if (rooms.isNotEmpty()) {
                        IconButton(
                            enabled = !isExporting,
                            onClick = {
                                val t = trip
                                if (t == null) {
                                    Toast.makeText(context, "Tunggu data trip termuat",
                                        Toast.LENGTH_SHORT).show()
                                    return@IconButton
                                }
                                isExporting = true
                                scope.launch {
                                    try {
                                        val file = withContext(Dispatchers.IO) {
                                            RoomListPdfExporter.export(context, t, hotel, rooms)
                                        }
                                        RoomListPdfExporter.share(context, file)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Gagal export: ${e.message}",
                                            Toast.LENGTH_LONG).show()
                                    }
                                    isExporting = false
                                }
                            }
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = AdminPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Share,
                                    contentDescription = "Export PDF", tint = AdminPrimary)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (rooms.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = onAutoAssign,
                        containerColor = Color(0xFF29B6F6),
                        contentColor = Color.White,
                        icon = { Text("⚡", fontSize = 16.sp) },
                        text = { Text("Auto-Assign", fontWeight = FontWeight.Bold) }
                    )
                }
                FloatingActionButton(
                    onClick = onGenerateRooms,
                    containerColor = AdminPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Kamar", tint = Color.White)
                }
            }
        }
    ) { padding ->
        if (rooms.isEmpty()) {
            Column(
                Modifier.padding(padding).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Belum ada kamar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text("Tap + untuk setup kamar & jamaah", fontSize = 12.sp, color = AdminTextGray)
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rooms, key = { it.id }) { room ->
                    RoomCard(room, onClick = { editingRoom = room })
                }
                item { Spacer(Modifier.height(120.dp)) }
            }
        }
    }

    editingRoom?.let { room ->
        EditRoomNumberDialog(
            room = room,
            onDismiss = { editingRoom = null },
            onSave = { newNomor ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            TripRepository.updateRoomNumber(tripId, hotel.id, room.id, newNomor)
                        }
                        editingRoom = null
                        Toast.makeText(context, "Nomor kamar diubah", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun EditRoomNumberDialog(
    room: Room,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nomor by remember { mutableStateOf(room.nomor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Nomor Kamar") },
        text = {
            Column {
                Text("Tipe: ${room.tipe} · Kapasitas: ${room.kapasitas} pax",
                    fontSize = 12.sp, color = AdminTextGray)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = nomor,
                    onValueChange = { nomor = it },
                    label = { Text("Nomor Kamar") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (nomor.isNotBlank()) onSave(nomor.trim()) },
                enabled = nomor.isNotBlank()
            ) { Text("Simpan", color = AdminPrimary, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun RoomCard(room: Room, onClick: () -> Unit) {
    val occupied = room.penghuni.size
    val full = occupied >= room.kapasitas
    val bgColor = when {
        occupied == 0 -> Color.White
        full -> Color(0xFFE8F5E9)
        else -> Color(0xFFFFF8E1)
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kamar ${room.nomor}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${occupied}/${room.kapasitas}",
                    fontSize = 12.sp,
                    color = if (full) Color(0xFF0F7A5A) else AdminTextGray,
                    fontWeight = FontWeight.Bold)
            }
            Text(room.tipe + (room.gender_lock?.let { " · $it" } ?: ""),
                fontSize = 11.sp, color = AdminTextGray)
            if (room.penghuni.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                room.penghuni.forEach { p ->
                    Text("• ${p.nama} (${p.usia} th, ${p.gender})",
                        fontSize = 11.sp, color = Color.DarkGray)
                }
            }
        }
    }
}
