package com.imtiyaztour.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.withContext

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val adminId = Prefs.getAdminId(context)
    val token = Prefs.getToken(context)
    val nama = Prefs.getNama(context)
    val role = Prefs.getRole(context)

    var data by remember { mutableStateOf<DashboardResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true; errorMsg = ""
        try {
            data = withContext(Dispatchers.IO) {
                AdminApiClient.service.dashboard(
                    mapOf("admin_id" to adminId, "token" to token)
                )
            }
        } catch (e: Exception) {
            errorMsg = "Gagal memuat dashboard"
        }
        loading = false
    }

    Column(
        Modifier.fillMaxSize().background(AdminBg).padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AdminPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Assalamualaikum,", color = Color(0xFFD1FAE5), fontSize = 13.sp)
                Text(nama.ifBlank { "Admin" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp)
                Text("Role: ${role.uppercase()}",
                    color = Color(0xFFBFDBFE),
                    fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(16.dp))

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = AdminPrimary)
        }
        if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = AdminDanger, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
        }

        data?.let { d ->
            Text("Ringkasan",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AdminText)
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Jamaah", d.total_jamaah.toString(), AdminPrimary, Modifier.weight(1f))
                StatCard("Belum Lunas", d.belum_lunas.toString(), AdminDanger, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Bukti Baru", d.bukti_menunggu.toString(), AdminWarning, Modifier.weight(1f))
                StatCard("Skrining", d.skrining_baru.toString(), AdminSuccess, Modifier.weight(1f))
            }

            if (d.perlu_followup > 0) {
                Spacer(Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("!!", color = AdminWarning, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("${d.perlu_followup} jamaah belum lunas > 30 hari",
                            fontSize = 12.sp,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = accent)
            Text(label, fontSize = 11.sp, color = AdminTextGray)
        }
    }
}
