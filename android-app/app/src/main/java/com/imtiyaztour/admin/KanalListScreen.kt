package com.imtiyaztour.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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

@Composable
fun KanalListScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminId = Prefs.getAdminId(context)
    val token = Prefs.getToken(context)

    var list by remember { mutableStateOf<List<KanalItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    var editKanal by remember { mutableStateOf<KanalItem?>(null) }
    var inputNama by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    suspend fun load() {
        loading = true; errorMsg = ""
        try {
            val resp = withContext(Dispatchers.IO) {
                AdminApiClient.service.kanalList(mapOf(
                    "admin_id" to adminId, "token" to token
                ))
            }
            if (resp.success == true) list = resp.kanal
            else errorMsg = resp.error ?: "Gagal memuat"
        } catch (e: Exception) {
            errorMsg = "Koneksi gagal"
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    // Dialog tambah/edit
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false; editKanal = null },
            title = { Text(if (editKanal == null) "Tambah Kanal" else "Edit Kanal", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = inputNama,
                    onValueChange = { inputNama = it },
                    label = { Text("Nama Kanal") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    placeholder = { Text("Contoh: Rombongan A - 24 Nov 2026") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputNama.isBlank()) return@TextButton
                        saving = true
                        scope.launch {
                            try {
                                val body = mutableMapOf(
                                    "admin_id" to adminId,
                                    "token" to token,
                                    "nama" to inputNama.trim()
                                )
                                editKanal?.let { body["kanal_id"] = it.id }
                                val resp = withContext(Dispatchers.IO) {
                                    AdminApiClient.service.kanalSave(body)
                                }
                                if (resp.success == true) {
                                    showDialog = false
                                    editKanal = null
                                    inputNama = ""
                                    load()
                                } else {
                                    errorMsg = resp.error ?: "Gagal simpan"
                                }
                            } catch (e: Exception) {
                                errorMsg = "Koneksi gagal"
                            }
                            saving = false
                        }
                    },
                    enabled = !saving && inputNama.isNotBlank()
                ) { Text("Simpan", color = AdminPrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; editKanal = null; inputNama = "" }) {
                    Text("Batal")
                }
            }
        )
    }

    Column(Modifier.fillMaxSize().background(AdminBg).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Kanal Radio", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = AdminPrimary)
                Text("${list.size} kanal terdaftar", fontSize = 12.sp, color = AdminTextGray)
            }
            Button(
                onClick = { editKanal = null; inputNama = ""; showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tambah", fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = AdminPrimary)
        if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = AdminDanger, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list) { k ->
                KanalCard(
                    kanal = k,
                    onEdit = { editKanal = k; inputNama = k.nama; showDialog = true },
                    onDelete = {
                        scope.launch {
                            try {
                                val resp = withContext(Dispatchers.IO) {
                                    AdminApiClient.service.kanalDelete(mapOf(
                                        "admin_id" to adminId,
                                        "token" to token,
                                        "kanal_id" to k.id
                                    ))
                                }
                                if (resp.success == true) load()
                                else errorMsg = resp.error ?: "Gagal hapus"
                            } catch (e: Exception) {
                                errorMsg = "Koneksi gagal"
                            }
                        }
                    }
                )
            }
            if (!loading && list.isEmpty() && errorMsg.isEmpty()) {
                item {
                    Text("Belum ada kanal. Tekan Tambah untuk membuat.",
                        fontSize = 12.sp, color = AdminTextGray)
                }
            }
        }
    }
}

@Composable
private fun KanalCard(kanal: KanalItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus Kanal?", fontWeight = FontWeight.Bold) },
            text = { Text("Hapus '${kanal.nama}'? Jamaah di kanal ini akan pindah ke kanal umum.", fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Hapus", color = AdminDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Batal") } }
        )
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(kanal.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AdminPrimary)
            Text("ID: ${kanal.id}", fontSize = 10.sp, color = AdminTextGray)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${kanal.total_jamaah} jamaah", fontSize = 11.sp, color = AdminTextGray)
                Spacer(Modifier.width(12.dp))
                if (kanal.ada_tl) {
                    Box(
                        modifier = Modifier
                            .background(AdminSuccess, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("TL AKTIF", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(AdminWarning, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("BELUM ADA TL", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AdminPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = AdminDanger, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
