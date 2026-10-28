package com.imtiyaztour.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KantorListScreen(onBack: () -> Unit) {
    val list by KantorRepository.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Kantor?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kantor Operasional") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminPrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showForm = true }, containerColor = AdminPrimary) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list, key = { it.id }) { item ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(item.nama, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AdminPrimary, modifier = Modifier.weight(1f))
                            Row {
                                IconButton(onClick = { editing = item; showForm = true }) { Icon(Icons.Default.Edit, "Edit", tint = AdminPrimary) }
                                IconButton(onClick = { scope.launch { KantorRepository.delete(item.id) } }) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Red) }
                            }
                        }
                        Text(item.alamat, fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(4.dp))
                        Text(item.telepon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showForm) {
        KantorFormDialog(existing = editing, onDismiss = { showForm = false })
    }
}

@Composable
fun KantorFormDialog(existing: Kantor?, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var nama by remember { mutableStateOf(existing?.nama ?: "") }
    var alamat by remember { mutableStateOf(existing?.alamat ?: "") }
    var telepon by remember { mutableStateOf(existing?.telepon ?: "") }
    var saving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (existing == null) "Tambah Kantor" else "Edit Kantor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nama, { nama = it }, label = { Text("Nama Cabang") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(alamat, { alamat = it }, label = { Text("Alamat Lengkap") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(telepon, { telepon = it }, label = { Text("Telp / WhatsApp") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    saving = true
                    scope.launch {
                        KantorRepository.save(Kantor(id = existing?.id ?: "", nama = nama, alamat = alamat, telepon = telepon, urutan = existing?.urutan ?: System.currentTimeMillis()))
                        saving = false
                        onDismiss()
                    }
                },
                enabled = !saving && nama.isNotBlank() && alamat.isNotBlank()
            ) { Text(if (saving) "..." else "SIMPAN") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Batal") } }
    )
}
