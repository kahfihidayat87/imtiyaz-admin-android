package com.imtiyaztour.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
    val role = Prefs.getRole(context).lowercase()
    val isAdmin = role == "admin"

    var nama by remember { mutableStateOf(jamaah.nama) }
    var paketId by remember { mutableStateOf(jamaah.paket_id ?: "") }
    var kanalId by remember { mutableStateOf(jamaah.kanal_id ?: "") }
    var totalTagihan by remember { mutableStateOf(jamaah.total_tagihan.toString()) }
    var sudahDibayar by remember { mutableStateOf(jamaah.sudah_dibayar.toString()) }
    var status by remember { mutableStateOf(jamaah.status_pembayaran) }
    var isTL by remember { mutableStateOf(jamaah.is_tour_leader) }

    var dokumenList by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var checklist by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var loadingDetail by remember { mutableStateOf(true) }

    var saving by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var msgError by remember { mutableStateOf(false) }
    var showInvoice by remember { mutableStateOf(false) }

    LaunchedEffect(jamaah.id) {
        loadingDetail = true
        try {
            val resp = withContext(Dispatchers.IO) {
                AdminApiClient.service.jamaahDetail(jamaah.id, adminId, token)
            }
            resp.jamaah?.let { d ->
                dokumenList = d.dokumen_list ?: emptyMap()
                checklist = d.checklist_dokumen ?: emptyMap()
                nama = d.nama
                paketId = d.paket_id ?: ""
                kanalId = d.kanal_id ?: ""
                totalTagihan = d.total_tagihan.toString()
                sudahDibayar = d.sudah_dibayar.toString()
                status = d.status_pembayaran
                isTL = d.is_tour_leader
            }
        } catch (e: Exception) {
            // biarkan, tetap bisa edit basic
        }
        loadingDetail = false
    }

    if (showInvoice) {
        InvoiceScreen(
            jamaah = jamaah,
            onBack = { showInvoice = false }
        )
        return
    }

    Column(
        Modifier.fillMaxSize().background(AdminBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("<- Kembali", color = AdminPrimary) }
        Text("Edit Jamaah", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = AdminPrimary)
        Text("ID: ${jamaah.id}", fontSize = 11.sp, color = AdminTextGray)
        Spacer(Modifier.height(16.dp))

        // ==== DATA DASAR ====
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Data Dasar", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AdminPrimary)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(nama, { nama = it }, label = { Text("Nama") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(paketId, { paketId = it }, label = { Text("Paket ID") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(kanalId, { kanalId = it }, label = { Text("Kanal Radio ID") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(totalTagihan, { totalTagihan = it.filter { c -> c.isDigit() } },
                    label = { Text("Total Tagihan") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(sudahDibayar, { sudahDibayar = it.filter { c -> c.isDigit() } },
                    label = { Text("Sudah Dibayar") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ==== STATUS PEMBAYARAN ====
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Status Pembayaran", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AdminPrimary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Belum Lunas", "Menunggu Verifikasi", "Lunas").forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ==== CHECKLIST DOKUMEN ====
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Checklist Dokumen", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AdminPrimary)
                    Spacer(Modifier.weight(1f))
                    if (loadingDetail) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = AdminPrimary)
                    } else {
                        val total = dokumenList.size
                        val done = dokumenList.count { checklist[it.key] == true }
                        Text("$done / $total", fontSize = 11.sp, color = AdminTextGray)
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (dokumenList.isEmpty()) {
                    Text(
                        if (loadingDetail) "Memuat..." else "Belum ada dokumen wajib.",
                        fontSize = 11.sp, color = AdminTextGray
                    )
                } else {
                    dokumenList.forEach { (key, label) ->
                        val checked = checklist[key] == true
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (checked) Color(0xFFF0FDF4) else Color(0xFFF9FAFB)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { v ->
                                        checklist = checklist.toMutableMap().apply { put(key, v) }
                                    }
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                if (checked) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AdminSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ==== TOGGLE TL (hanya admin) ====
        if (isAdmin) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Tour Leader", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                        Text("Boleh bicara di Radio TL", fontSize = 10.sp, color = Color(0xFF92400E))
                    }
                    Switch(checked = isTL, onCheckedChange = { v -> isTL = v })
                }
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
                        if (r1.success != true) {
                            msg = r1.error ?: "Gagal simpan data dasar"
                            msgError = true
                            saving = false
                            return@launch
                        }

                        if (dokumenList.isNotEmpty()) {
                            val r2 = withContext(Dispatchers.IO) {
                                AdminApiClient.service.checklistUpdate(ChecklistUpdateRequest(
                                    admin_id = adminId,
                                    token = token,
                                    jamaah_id = jamaah.id.toString(),
                                    checklist = checklist
                                ))
                            }
                            if (r2.success != true) {
                                msg = r2.error ?: "Gagal simpan checklist"
                                msgError = true
                                saving = false
                                return@launch
                            }
                        }

                        if (isAdmin) {
                            withContext(Dispatchers.IO) {
                                AdminApiClient.service.toggleTL(mapOf(
                                    "admin_id" to adminId, "token" to token,
                                    "jamaah_id" to jamaah.id.toString(),
                                    "is_tour_leader" to (if (isTL) "1" else "0")
                                ))
                            }
                        }

                        msg = "Tersimpan!"
                        msgError = false
                        onSaved()
                    } catch (e: Exception) {
                        msg = "Error: " + (e.message ?: "unknown")
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
            Text(msg,
                color = if (msgError) AdminDanger else AdminSuccess,
                fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // ==== GENERATE INVOICE ====
        Spacer(Modifier.height(24.dp))
        Divider()
        Spacer(Modifier.height(16.dp))
        Text("Invoice", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AdminPrimary)
        Text("Buat invoice PDF untuk jamaah ini", fontSize = 11.sp, color = AdminTextGray)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showInvoice = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("GENERATE INVOICE", fontWeight = FontWeight.Bold, color = AdminPrimary)
        }
    }
}
