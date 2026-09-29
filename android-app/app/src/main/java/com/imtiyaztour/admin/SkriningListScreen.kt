package com.imtiyaztour.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SkriningListScreen() {
    val context = LocalContext.current
    val adminId = Prefs.getAdminId(context)
    val token = Prefs.getToken(context)

    var list by remember { mutableStateOf<List<SkriningItem>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }

    suspend fun load() {
        loading = true; errorMsg = ""
        try {
            val resp = withContext(Dispatchers.IO) {
                AdminApiClient.service.skriningList(
                    mapOf(
                        "admin_id" to adminId,
                        "token" to token,
                        "search" to query,
                        "per_page" to "100"
                    )
                )
            }
            if (resp.success == true) {
                list = resp.list
                total = resp.total
            } else {
                errorMsg = resp.error ?: "Gagal memuat"
            }
        } catch (e: retrofit2.HttpException) {
            val body = try { e.response()?.errorBody()?.string() ?: "" } catch (_: Exception) { "" }
            errorMsg = "HTTP ${e.code()}: " + body.take(200)
        } catch (e: Exception) {
            errorMsg = e.javaClass.simpleName + ": " + (e.message ?: "unknown")
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }
    LaunchedEffect(query) {
        delay(400)
        load()
    }

    Column(Modifier.fillMaxSize().background(AdminBg).padding(16.dp)) {
        Text("Skrining Kesehatan", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = AdminPrimary)
        Text("$total hasil skrining", fontSize = 12.sp, color = AdminTextGray)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Cari nama jamaah...") },
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
            items(list) { item ->
                SkriningCard(item) {
                    val url = "https://api.pastiumrah.com/api/skrining/pdf/${item.id}"
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        val browser = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        browser.addCategory(Intent.CATEGORY_BROWSABLE)
                        context.startActivity(browser)
                    }
                }
            }
            if (!loading && list.isEmpty() && errorMsg.isEmpty()) {
                item { Text("Belum ada data skrining", fontSize = 12.sp, color = AdminTextGray) }
            }
        }
    }
}

@Composable
private fun SkriningCard(item: SkriningItem, onOpenPdf: () -> Unit) {
    val tgl = try {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            .format(Date(item.tanggal * 1000L))
    } catch (_: Exception) { "-" }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPdf() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (item.catatan_kritis) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Perlu perhatian",
                                tint = AdminWarning,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "ID #${item.id}  -  Jamaah #${item.jamaah_id ?: "-"}",
                        fontSize = 11.sp, color = AdminTextGray
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tanggal", fontSize = 10.sp, color = AdminTextGray)
                    Text(tgl, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Column {
                    Text("Usia", fontSize = 10.sp, color = AdminTextGray)
                    Text(if (item.usia > 0) "${item.usia} thn" else "-", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = AdminPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "BUKA PDF",
                        color = AdminPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
