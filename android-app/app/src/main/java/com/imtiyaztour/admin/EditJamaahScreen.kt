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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditJamaahScreen(jamaah: JamaahSummary, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminId = Prefs.getAdminId(context)
    val token = Prefs.getToken(context)

    var nama by remember { mutableStateOf(jamaah.nama) }
    var paketId by remember { mutableStateOf(jamaah.paket_id ?: "") }
    var kanalId by remember { mutableStateOf(jamaah.kanal_id ?: "") }
    var totalTagihan by remember { mutableStateOf(jamaah.total_tagihan.toString()) }
    var sudahDibayar by remember { mutableStateOf(jamaah.sudah_dibayar.toString()) }
    var status by remember { mutableStateOf(jamaah.status_pembayaran) }
    var isTL by remember { mutableStateOf(jamaah.is_tour_leader) }
    var saving by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var msgError by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(AdminBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("<- Kembali", color = AdminPrimary) }
        Text("Edit Jamaah", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = AdminPrimary)
        Text("ID: ${jamaah.id}", fontSize = 11.sp, color = AdminTextGray)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(nama, { nama = it }, label = { Text("Nama") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(paketId, { paketId = it }, label = { Text("Paket ID") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(kanalId, { kanalId = it }, label = { Text("Kanal Radio ID") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(totalTagihan, { totalTagihan = it.filter { c -> c.isDigit() } },
            label = { Text("Total Tagihan") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(sudahDibayar, { sudahDibayar = it.filter { c -> c.isDigit() } },
            label = { Text("Sudah Dibayar") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
        Spacer(Modifier.height(10.dp))
        Text("Status Pembayaran:", fontSize = 12.sp, color = AdminTextGray)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Belum Lunas", "Menunggu Verifikasi", "Lunas").forEach { s ->
                FilterChip(selected = status == s, onClick = { status = s },
                    label = { Text(s, fontSize = 10.sp) })
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
            shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tour Leader", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                    Text("Boleh bicara di Radio TL", fontSize = 10.sp, color = Color(0xFF92400E))
                }
                Switch(checked = isTL, onCheckedChange = { isTL = it })
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                saving = true; msg = ""
                scope.launch {
                    try {
                        val r1 = withContext(Dispatchers.IO) {
                            AdminApiClient.service.jamaahUpdate(mapOf(
                                "admin_id" to adminId, "token" to token,
                                "jamaah_id" to jamaah.id.toString(),
                                "nama" to nama,
                                "paket_id" to paketId,
                                "kanal_id" to kanalId,
                                "total_tagihan" to (totalTagihan.toLongOrNull() ?: 0L).toString(),
                                "sudah_dibayar" to (sudahDibayar.toLongOrNull() ?: 0L).toString(),
                                "status_pembayaran" to status
                            ))
                        }
                        withContext(Dispatchers.IO) {
                            AdminApiClient.service.toggleTL(mapOf(
                                "admin_id" to adminId, "token" to token,
                                "jamaah_id" to jamaah.id.toString(), "is_tour_leader" to (if (isTL) "1" else "0")
                            ))
                        }
                        if (r1.success == true) {
                            msg = "Tersimpan!"
                            msgError = false
                            onSaved()
                        } else {
                            msg = r1.error ?: "Gagal simpan"
                            msgError = true
                        }
                    } catch (e: Exception) {
                        msg = "Koneksi gagal: " + (e.message ?: "")
                        msgError = true
                    }
                    saving = false
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (saving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("SIMPAN PERUBAHAN", fontWeight = FontWeight.Bold)
        }

        if (msg.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(msg, color = if (msgError) AdminDanger else AdminSuccess,
                fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
