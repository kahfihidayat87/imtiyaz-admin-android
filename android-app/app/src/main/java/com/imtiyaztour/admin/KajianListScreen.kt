package com.imtiyaztour.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KajianListScreen(
    onAddClick: () -> Unit,
    onEdit: (Kajian) -> Unit = {},
    onBack: () -> Unit
) {
    val flow = remember { KajianRepository.observeAll() }
    val list by flow.collectAsState(initial = emptyList())
    var menuTarget by remember { mutableStateOf<Kajian?>(null) }
    var confirmDelete by remember { mutableStateOf<Kajian?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jadwal Kajian Mabrur") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AdminPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            if (list.isEmpty()) {
                Text("Belum ada jadwal kajian", modifier = Modifier.align(Alignment.Center), color = AdminTextGray)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list, key = { it.id }) { item ->
                        val tgl = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")).format(Date(item.tanggal))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                    Text(item.tema, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AdminPrimary,
                                        modifier = Modifier.weight(1f))
                                    Box {
                                        IconButton(onClick = { menuTarget = item }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = AdminTextGray)
                                        }
                                        DropdownMenu(
                                            expanded = menuTarget?.id == item.id,
                                            onDismissRequest = { menuTarget = null }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Edit Jadwal") },
                                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = AdminPrimary) },
                                                onClick = { menuTarget = null; onEdit(item) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Hapus", color = Color(0xFFDC2626)) },
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626)) },
                                                onClick = { menuTarget = null; confirmDelete = item }
                                            )
                                        }
                                    }
                                }
                                Text("Pemateri: ${item.pemateri}", fontSize = 13.sp)
                                Text("Waktu: $tgl", fontSize = 12.sp, color = AdminTextGray)
                                Text("Jam: ${item.jam} WIB", fontSize = 12.sp, color = AdminTextGray)
                                Spacer(Modifier.height(8.dp))
                                Surface(color = AdminPrimary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                    Text("Kode Radio: ${item.kode_gelombang}", modifier = Modifier.padding(4.dp, 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AdminPrimary)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                containerColor = AdminPrimary
            ) { Icon(Icons.Default.Add, null, tint = Color.White) }
        }
    }

    if (confirmDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Hapus Jadwal Kajian?") },
            text = { Text("\"" + confirmDelete!!.tema + "\" akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = confirmDelete!!.id
                    confirmDelete = null
                    scope.launch { KajianRepository.delete(id) }
                }) { Text("HAPUS", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Batal") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddKajianScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    existingKajian: Kajian? = null
) {
    val scope = rememberCoroutineScope()
    var tema by remember { mutableStateOf(existingKajian?.tema ?: "") }
    var pemateri by remember { mutableStateOf(existingKajian?.pemateri ?: "") }
    var tanggal by remember { mutableStateOf(existingKajian?.tanggal ?: System.currentTimeMillis()) }
    var jam by remember { mutableStateOf(existingKajian?.jam ?: "") }
    var kode by remember { mutableStateOf(existingKajian?.kode_gelombang ?: "") }
    val isEditing = existingKajian != null
    var showDatePicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Jadwal Kajian" else "Tambah Jadwal Kajian") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AdminPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = tema, onValueChange = { tema = it }, label = { Text("Tema Kajian") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = pemateri, onValueChange = { pemateri = it }, label = { Text("Nama Pemateri") }, modifier = Modifier.fillMaxWidth())
            
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Default.DateRange, null)
                Spacer(Modifier.width(8.dp))
                Text(SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")).format(Date(tanggal)))
            }
            
            OutlinedTextField(value = jam, onValueChange = { jam = it }, label = { Text("Jam (Contoh: 08:00)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = kode, onValueChange = { kode = it.uppercase() }, label = { Text("Kode Gelombang Radio (Opsional)") }, modifier = Modifier.fillMaxWidth())
            
            Button(
                onClick = {
                    scope.launch {
                        saving = true
                        val kajian = Kajian(
                            id = existingKajian?.id ?: "",
                            tema = tema, pemateri = pemateri, tanggal = tanggal,
                            jam = jam, kode_gelombang = kode
                        )
                        val res = if (isEditing && existingKajian != null) {
                            KajianRepository.update(existingKajian.id, kajian)
                        } else {
                            KajianRepository.create(kajian)
                        }
                        saving = false
                        if (res.isSuccess) onSaved()
                    }
                },
                enabled = tema.isNotBlank() && pemateri.isNotBlank() && jam.isNotBlank() && !saving,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
            ) { Text(if (saving) "MENYIMPAN..." else if (isEditing) "SIMPAN PERUBAHAN" else "SIMPAN JADWAL") }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = tanggal)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    tanggal = state.selectedDateMillis ?: tanggal
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } }
        ) { DatePicker(state = state) }
    }
}
