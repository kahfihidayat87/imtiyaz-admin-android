package com.imtiyaztour.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KajianListScreen(onAddClick: () -> Unit) {
    val list by KajianRepository.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        if (list.isEmpty()) {
            Text("Belum ada jadwal kajian", modifier = Modifier.align(Alignment.Center), color = AdminTextGray)
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(list, key = { it.id }) { item ->
                    val tgl = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")).format(Date(item.tanggal))
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.tema, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AdminPrimary)
                                IconButton(onClick = { scope.launch { KajianRepository.delete(item.id) } }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFDC2626))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddKajianScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tema by remember { mutableStateOf("") }
    var pemateri by remember { mutableStateOf("") }
    var tanggal by remember { mutableStateOf(System.currentTimeMillis()) }
    var jam by remember { mutableStateOf("") }
    var kode by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Jadwal Kajian") },
                navigationIcon = { IconButton(onClick = onBack) { Text("← Kembali") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminPrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        val res = KajianRepository.create(Kajian(tema = tema, pemateri = pemateri, tanggal = tanggal, jam = jam, kode_gelombang = kode))
                        saving = false
                        if (res.isSuccess) onSaved()
                    }
                },
                enabled = tema.isNotBlank() && pemateri.isNotBlank() && jam.isNotBlank() && !saving,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
            ) { Text("SIMPAN JADWAL") }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = tanggal)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { tanggal = state.selectedDateMillis ?: tanggal; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } }
        ) { DatePicker(state = state) }
    }
}
