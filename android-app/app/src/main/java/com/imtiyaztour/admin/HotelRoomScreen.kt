package com.imtiyaztour.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.window.Dialog
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
    onGenerateRooms: (mode: String) -> Unit,
    onAutoAssign: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val rooms by TripRepository.observeRooms(tripId, hotel.id)
        .collectAsState(initial = emptyList())

    var trip by remember { mutableStateOf<Trip?>(null) }
    var editingRoom by remember { mutableStateOf<Room?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var showModeSheet by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf("nomor") }  // nomor | tipe | L | P
    var showSortSheet by remember { mutableStateOf(false) }

    // Terapkan sortir
    val sortedRooms = remember(rooms, sortMode) {
        when (sortMode) {
            "tipe" -> rooms.sortedWith(compareBy(
                { tipeOrder(it.tipe) }, { it.nomor }
            ))
            "L" -> rooms.filter { it.gender_lock == "L" || it.penghuni.any { p -> p.gender == "L" } }
                .sortedBy { it.nomor }
            "P" -> rooms.filter { it.gender_lock == "P" || it.penghuni.any { p -> p.gender == "P" } }
                .sortedBy { it.nomor }
            else -> rooms.sortedBy { it.nomor }
        }
    }

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
                        onClick = { showSortSheet = true },
                        containerColor = Color(0xFF6B7280),
                        contentColor = Color.White,
                        icon = { Text("⇅", fontSize = 16.sp) },
                        text = { Text("Sortir", fontWeight = FontWeight.Bold) }
                    )
                }
                FloatingActionButton(
                    onClick = { showModeSheet = true },
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
                items(sortedRooms, key = { it.id }) { room ->
                    RoomCard(room, onClick = { editingRoom = room })
                }
                item { Spacer(Modifier.height(120.dp)) }
            }
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(onDismissRequest = { showSortSheet = false }) {
            Column(Modifier.padding(20.dp)) {
                Text("Sortir / Filter Kamar", fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, color = AdminPrimary)
                Spacer(Modifier.height(12.dp))

                val opts = listOf(
                    Triple("nomor", "Nomor Kamar", "Urutkan sesuai nomor"),
                    Triple("tipe", "Tipe Kamar", "Quad → Triple → Double → Single → Quint"),
                    Triple("L", "Kamar Laki-laki", "Tampilkan hanya kamar L"),
                    Triple("P", "Kamar Perempuan", "Tampilkan hanya kamar P"),
                )
                opts.forEach { (key, title, desc) ->
                    val selected = sortMode == key
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            .clickable {
                                sortMode = key
                                showSortSheet = false
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) Color(0xFFE8F5E9) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(title, fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selected) Color(0xFF0F7A5A) else Color(0xFF111827))
                                Text(desc, fontSize = 11.sp, color = AdminTextGray)
                            }
                            if (selected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null,
                                    tint = Color(0xFF0F7A5A), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showModeSheet) {
        ModalBottomSheet(onDismissRequest = { showModeSheet = false }) {
            Column(Modifier.padding(20.dp)) {
                Text("Pilih Mode Setup Kamar", fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, color = AdminPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Pilih cara generate kamar sesuai kebutuhan",
                    fontSize = 12.sp, color = AdminTextGray)
                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        showModeSheet = false
                        onGenerateRooms("manual")
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🛏", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Setup Manual", fontWeight = FontWeight.Bold,
                                fontSize = 14.sp, color = Color(0xFF1565C0))
                            Text("Input pax L/P + jumlah kamar per tipe (quint/quad/triple/double/single)",
                                fontSize = 11.sp, color = Color(0xFF1565C0))
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        showModeSheet = false
                        onGenerateRooms("auto")
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Auto Generate", fontWeight = FontWeight.Bold,
                                fontSize = 14.sp, color = Color(0xFF0F7A5A))
                            Text("Input pax L/P saja — sistem hitung kebutuhan kamar (quad→triple→double→single)",
                                fontSize = 11.sp, color = Color(0xFF0F7A5A))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }

    editingRoom?.let { room ->
        var showDeleteConfirm by remember { mutableStateOf(false) }
        RoomDetailDialog(
            room = room,
            onDismiss = { editingRoom = null },
            onSave = { newNomor, newPenghuni ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            TripRepository.updateRoomAndPenghuni(
                                tripId, hotel.id, room.id, newNomor, newPenghuni
                            )
                        }
                        editingRoom = null
                        Toast.makeText(context, "Kamar disimpan", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDelete = { showDeleteConfirm = true }
        )
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Hapus Kamar?") },
                text = { Text("Kamar ${room.nomor} beserta semua penghuninya akan dihapus.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    TripRepository.deleteRoom(tripId, hotel.id, room.id)
                                }
                                editingRoom = null
                                Toast.makeText(context, "Kamar dihapus", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) { Text("Hapus", color = AdminDanger, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
                }
            )
        }
    }
}

@Composable
private fun RoomDetailDialog(
    room: Room,
    onDismiss: () -> Unit,
    onSave: (newNomor: String, newPenghuni: List<Penghuni>) -> Unit,
    onDelete: () -> Unit = {}
) {
    var nomor by remember { mutableStateOf(room.nomor) }
    val penghuniState = remember {
        mutableStateListOf<Penghuni>().apply { addAll(room.penghuni) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().background(AdminPrimary).padding(16.dp)) {
                    Column {
                        Text("Kamar ${room.nomor}", color = Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${room.tipe} · ${room.penghuni.size}/${room.kapasitas} pax",
                            color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                    }
                }

                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text("Nomor Kamar", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        color = AdminPrimary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = nomor,
                        onValueChange = { nomor = it },
                        label = { Text("Nomor") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Penghuni", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        color = AdminPrimary)
                    Spacer(Modifier.height(6.dp))

                    if (penghuniState.isEmpty()) {
                        Text("(Kosong)", fontSize = 12.sp, color = AdminTextGray)
                    } else {
                        penghuniState.forEachIndexed { idx, p ->
                            OutlinedTextField(
                                value = p.nama,
                                onValueChange = { newName ->
                                    penghuniState[idx] = p.copy(nama = newName)
                                },
                                label = { Text("Penghuni ${idx + 1} (${p.gender})") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("BATAL", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    Button(
                        onClick = {
                            val cleanNomor = nomor.trim()
                            if (cleanNomor.isNotBlank()) {
                                onSave(cleanNomor, penghuniState.toList())
                            }
                        },
                        enabled = nomor.isNotBlank(),
                        modifier = Modifier.weight(1.5f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("SIMPAN", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminDanger),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AdminDanger)
                    ) { Text("HAPUS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AdminDanger) }
                }
            }
        }
    }
}

@Composable
private fun RoomCard(room: Room, onClick: () -> Unit) {
    val occupied = room.penghuni.size
    val full = occupied >= room.kapasitas

    // Deteksi gender kamar
    val gender = room.gender_lock
        ?: room.penghuni.firstOrNull()?.gender

    val (bgColor, accentColor, genderLabel) = when (gender) {
        "L" -> Triple(Color(0xFFE3F2FD), Color(0xFF1976D2), "♂ LAKI-LAKI")
        "P" -> Triple(Color(0xFFFCE4EC), Color(0xFFC2185B), "♀ PEREMPUAN")
        else -> Triple(Color(0xFFF3F4F6), Color(0xFF6B7280), "— KOSONG")
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kamar ${room.nomor}", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = accentColor)
                Text("${occupied}/${room.kapasitas}",
                    fontSize = 12.sp,
                    color = if (full) accentColor else AdminTextGray,
                    fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(room.tipe, fontSize = 11.sp, color = AdminTextGray)
                Text(genderLabel, fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
            }
            if (room.penghuni.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                room.penghuni.forEach { p ->
                    Text("• ${p.nama}",
                        fontSize = 11.sp, color = Color.DarkGray)
                }
            }
        }
    }
}

/** Urutan tipe untuk sorting */
internal fun tipeOrder(tipe: String): Int = when (tipe.lowercase()) {
    "quad" -> 1
    "triple" -> 2
    "double" -> 3
    "single" -> 4
    "quint" -> 5
    else -> 99
}
