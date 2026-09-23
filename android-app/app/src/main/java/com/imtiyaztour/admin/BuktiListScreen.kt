package com.imtiyaztour.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BuktiListScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminId = Prefs.getAdminId(context)
    val token = Prefs.getToken(context)

    var list by remember { mutableStateOf<List<BuktiItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<BuktiItem?>(null) }
    var processing by remember { mutableStateOf(false) }
    var processMsg by remember { mutableStateOf("") }

    suspend fun load() {
        loading = true; errorMsg = ""
        try {
            val resp = withContext(Dispatchers.IO) {
                AdminApiClient.service.buktiList(mapOf(
                    "admin_id" to adminId, "token" to token
                ))
            }
            if (resp.success == true) list = resp.list
            else errorMsg = resp.error ?: "Gagal memuat"
        } catch (e: Exception) {
            errorMsg = "Koneksi gagal: " + (e.message ?: "")
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    // Dialog detail
    if (selected != null) {
        val item = selected!!
        AlertDialog(
            onDismissRequest = { if (!processing) { selected = null; processMsg = "" } },
            title = { Text(item.nama, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("ID: ${item.id}", fontSize = 11.sp, color = AdminTextGray)
                    Spacer(Modifier.height(6.dp))
                    Text("Total: ${formatRupiah(item.total_tagihan)}", fontSize = 12.sp)
                    Text("Sudah dibayar: ${formatRupiah(item.sudah_dibayar)}", fontSize = 12.sp)
                    Text("Sisa: ${formatRupiah(item.sisa_tagihan)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminDanger)
                    Spacer(Modifier.height(12.dp))
                    Text("Bukti Transfer:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    val url = item.bukti_transfer
                    if (!url.isNullOrBlank()) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Bukti transfer",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 320.dp)
                                .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                        )
                    } else {
                        Text("Bukti belum di-upload.", fontSize = 11.sp, color = AdminTextGray)
                    }
                    if (processMsg.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(processMsg, fontSize = 11.sp, color = if (processMsg.startsWith("OK")) AdminSuccess else AdminDanger)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        processing = true; processMsg = ""
                        scope.launch {
                            try {
                                val resp = withContext(Dispatchers.IO) {
                                    AdminApiClient.service.buktiApprove(mapOf(
                                        "admin_id" to adminId,
                                        "token" to token,
                                        "jamaah_id" to item.id.toString()
                                    ))
                                }
                                if (resp.success == true) {
                                    processMsg = "OK tersimpan"
                                    selected = null
                                    load()
                                } else {
                                    processMsg = resp.error ?: "Gagal"
                                }
                            } catch (e: Exception) {
                                processMsg = "Error: " + (e.message ?: "")
                            }
                            processing = false
                        }
                    },
                    enabled = !processing,
                    colors = ButtonDefaults.buttonColors(containerColor = AdminSuccess)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("SETUJUI", fontSize = 12.sp)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        processing = true; processMsg = ""
                        scope.launch {
                            try {
                                val resp = withContext(Dispatchers.IO) {
                                    AdminApiClient.service.buktiReject(mapOf(
                                        "admin_id" to adminId,
                                        "token" to token,
                                        "jamaah_id" to item.id.toString(),
                                        "alasan" to "Bukti ditolak oleh admin"
                                    ))
                                }
                                if (resp.success == true) {
                                    processMsg = "OK ditolak"
                                    selected = null
                                    load()
                                } else {
                                    processMsg = resp.error ?: "Gagal"
                                }
                            } catch (e: Exception) {
                                processMsg = "Error: " + (e.message ?: "")
                            }
                            processing = false
                        }
                    },
                    enabled = !processing,
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDanger)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("TOLAK", fontSize = 12.sp)
                }
            }
        )
    }

    Column(Modifier.fillMaxSize().background(AdminBg).padding(16.dp)) {
        Text("Bukti Transfer", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = AdminPrimary)
        Text("${list.size} menunggu verifikasi", fontSize = 12.sp, color = AdminTextGray)
        Spacer(Modifier.height(12.dp))

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = AdminPrimary)
        if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = AdminDanger, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list) { item ->
                BuktiCard(item, onClick = { selected = item })
            }
            if (!loading && list.isEmpty() && errorMsg.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = AdminSuccess, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Tidak ada bukti menunggu", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AdminSuccess)
                            Text("Semua bukti sudah diverifikasi", fontSize = 11.sp, color = AdminTextGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuktiCard(item: BuktiItem, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(Modifier.padding(12.dp)) {
            // Thumbnail bukti
            val url = item.bukti_transfer
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!url.isNullOrBlank()) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Image, contentDescription = null, tint = AdminTextGray, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Total: ${formatRupiah(item.total_tagihan)}", fontSize = 11.sp, color = AdminTextGray)
                Text("Sisa: ${formatRupiah(item.sisa_tagihan)}", fontSize = 11.sp, color = AdminDanger, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Ketuk untuk verifikasi", fontSize = 10.sp, color = AdminPrimary)
            }
        }
    }
}
