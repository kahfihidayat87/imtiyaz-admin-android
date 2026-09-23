package com.imtiyaztour.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun JamaahListScreen() {
    val context = LocalContext.current
    val adminId = Prefs.getAdminId(context)
    val token = Prefs.getToken(context)

    var list by remember { mutableStateOf<List<JamaahSummary>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }

    suspend fun load() {
        loading = true; errorMsg = ""
        try {
            val resp = withContext(Dispatchers.IO) {
                AdminApiClient.service.jamaahList(
                    mapOf(
                        "admin_id" to adminId,
                        "token" to token,
                        "search" to query,
                        "per_page" to 100
                    )
                )
            }
            if (resp.success == true) {
                list = resp.jamaah
                total = resp.total
            } else {
                errorMsg = resp.error ?: "Gagal memuat"
            }
        } catch (e: Exception) {
            errorMsg = "Koneksi gagal"
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    LaunchedEffect(query) {
        delay(400)
        load()
    }

    Column(Modifier.fillMaxSize().background(AdminBg).padding(16.dp)) {
        Text("Jamaah", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = AdminPrimary)
        Text("$total jamaah terdaftar", fontSize = 12.sp, color = AdminTextGray)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Cari nama/username...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = AdminPrimary)
        if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = AdminDanger, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list) { j ->
                JamaahCard(j)
            }
            if (!loading && list.isEmpty() && errorMsg.isEmpty()) {
                item {
                    Text("Tidak ada jamaah", fontSize = 12.sp, color = AdminTextGray)
                }
            }
        }
    }
}

@Composable
private fun JamaahCard(j: JamaahSummary) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(j.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (j.is_tour_leader) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(AdminWarning, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text("TL", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("Paket: ${j.paket_id ?: "-"}",
                        fontSize = 11.sp, color = AdminTextGray)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Tagihan", fontSize = 10.sp, color = AdminTextGray)
                    Text(formatRupiah(j.total_tagihan), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Status", fontSize = 10.sp, color = AdminTextGray)
                    Text(
                        j.status_pembayaran,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (j.status_pembayaran == "Lunas") AdminSuccess else AdminDanger
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Dokumen: ${j.dokumen_lengkap}/${j.dokumen_total}",
                fontSize = 10.sp, color = AdminTextGray)
        }
    }
}
