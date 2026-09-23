package com.imtiyaztour.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun PengumumanScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminId = Prefs.getAdminId(context)
    val token = Prefs.getToken(context)

    var judul by remember { mutableStateOf("") }
    var pesan by remember { mutableStateOf("") }
    var penting by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var resultMsg by remember { mutableStateOf("") }
    var resultIsError by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(AdminBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Kirim Pengumuman",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = AdminPrimary)
        Text("Broadcast info ke semua jamaah aplikasi",
            fontSize = 12.sp,
            color = AdminTextGray)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = judul,
            onValueChange = { judul = it },
            label = { Text("Judul") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = pesan,
            onValueChange = { pesan = it },
            label = { Text("Pesan lengkap") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Tandai sebagai INFO PENTING",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E))
                    Text("Notifikasi lebih menonjol + warna kuning di app jamaah",
                        fontSize = 10.sp,
                        color = Color(0xFF92400E))
                }
                Switch(
                    checked = penting,
                    onCheckedChange = { penting = it }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (judul.isBlank() || pesan.isBlank()) {
                    resultMsg = "Judul dan pesan wajib diisi"
                    resultIsError = true
                    return@Button
                }
                isSending = true
                resultMsg = ""
                scope.launch {
                    try {
                        val resp = withContext(Dispatchers.IO) {
                            AdminApiClient.service.sendAnnouncement(mapOf(
                                "admin_id" to adminId,
                                "token" to token,
                                "judul" to judul.trim(),
                                "pesan" to pesan.trim(),
                                "penting" to penting
                            ))
                        }
                        if (resp.success == true) {
                            resultMsg = "Pengumuman terkirim ke semua jamaah!"
                            resultIsError = false
                            judul = ""
                            pesan = ""
                            penting = false
                        } else {
                            resultMsg = resp.error ?: "Gagal mengirim"
                            resultIsError = true
                        }
                    } catch (e: Exception) {
                        resultMsg = "Koneksi gagal: " + (e.message ?: "unknown")
                        resultIsError = true
                    }
                    isSending = false
                }
            },
            enabled = !isSending,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("KIRIM PENGUMUMAN", fontWeight = FontWeight.Bold)
            }
        }

        if (resultMsg.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (resultIsError) Color(0xFFFEE2E2) else Color(0xFFD1FAE5)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    resultMsg,
                    modifier = Modifier.padding(14.dp),
                    fontSize = 12.sp,
                    color = if (resultIsError) AdminDanger else AdminSuccess,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
