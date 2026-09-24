package com.imtiyaztour.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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

// ============================================================================
// [INVOICE] v1.1 — Form generate invoice PDF (dengan tombol SIMPAN dulu)
// Alur: Admin isi NILAI TAGIHAN -> klik SIMPAN (update ke WP) -> tombol
// GENERATE aktif -> klik GENERATE -> PDF dibuat -> buka di browser.
// Rumus: sisa = max(0, total_tagihan - sum(paymentHistory))
// ============================================================================

private val DEFAULT_FASILITAS = listOf(
    "Tiket pesawat internasional",
    "Visa umrah",
    "Hotel Madinah",
    "Hotel Makkah",
    "Makan prasmanan 3x sehari",
    "Transportasi bus AC",
    "Pembimbing sejak dari tanah air",
    "Pendampingan 24 jam selama di tanah suci",
    "Handling bagasi",
    "Asuransi perjalanan",
    "Air zam-zam (jika diizinkan)",
    "Perlengkapan umrah (koper, ihram/mukena, kain batik, id card)"
)

private val DEFAULT_EXCLUDED = listOf(
    "Biaya pembuatan paspor",
    "Pengeluaran di luar program",
    "Kelebihan bagasi",
    "Hotel transit di Jakarta (jika ada)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(jamaah: JamaahSummary, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminId = Prefs.getAdminId(context)
    val token = Prefs.getToken(context)

    // ============ IDENTITAS ============
    var invoiceNumber by remember {
        mutableStateOf("INV-${jamaah.id}-${System.currentTimeMillis() / 1000}")
    }
    var billTo by remember { mutableStateOf(jamaah.nama) }

    // ============ NILAI TAGIHAN (bagian utama) ============
    var totalTagihan by remember {
        mutableStateOf(if (jamaah.total_tagihan > 0) jamaah.total_tagihan.toString() else "0")
    }
    val paymentHistory = remember {
        mutableStateListOf<Long>().apply {
            if (jamaah.sudah_dibayar > 0) add(jamaah.sudah_dibayar)
        }
    }
    val totalSudah = paymentHistory.sum()
    val totalTagihanLong = totalTagihan.toLongOrNull() ?: 0L
    val sisaTagihan = totalTagihanLong - totalSudah
    val sisaTagihanClamped = if (sisaTagihan < 0) 0L else sisaTagihan

    // ============ DETAIL PERJALANAN ============
    var keberangkatan by remember { mutableStateOf("") }
    var hotelMadinah by remember { mutableStateOf("") }
    var hotelMakkah by remember { mutableStateOf("") }

    // ============ ITEMS (auto-fill dari total) ============
    val items = remember {
        mutableStateListOf(InvoiceItem("Paket Umrah", 1, jamaah.total_tagihan))
    }
    // Sync item pertama dengan totalTagihan
    LaunchedEffect(totalTagihan) {
        if (items.isNotEmpty()) {
            items[0] = items[0].copy(quantity = 1, price = totalTagihanLong)
        }
    }

    // ============ FASILITAS ============
    var fasilitasText by remember { mutableStateOf(DEFAULT_FASILITAS.joinToString("\n")) }
    var excludedText by remember { mutableStateOf(DEFAULT_EXCLUDED.joinToString("\n")) }

    // ============ STATE SIMPAN / GENERATE ============
    var savedSnapshot by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var saving by remember { mutableStateOf(false) }
    var saveMsg by remember { mutableStateOf("") }
    var saveMsgError by remember { mutableStateOf(false) }

    var generating by remember { mutableStateOf(false) }
    var generateMsg by remember { mutableStateOf("") }
    var generateMsgError by remember { mutableStateOf(false) }
    var resultUrl by remember { mutableStateOf("") }

    val currentSnapshot = totalTagihanLong to totalSudah
    val hasUnsavedChanges = savedSnapshot == null || savedSnapshot != currentSnapshot
    val canGenerate = !hasUnsavedChanges && savedSnapshot != null

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("<- Kembali", color = AdminPrimary) }
            Spacer(Modifier.weight(1f))
            Text("Generate Invoice", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AdminPrimary)
        }

        // ============ IDENTITAS JAMAAH ============
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Jamaah", fontWeight = FontWeight.Bold, color = AdminPrimary)
                Text("ID: ${jamaah.id}  •  Nama: ${jamaah.nama}", fontSize = 12.sp, color = Color.Gray)
                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = { invoiceNumber = it },
                    label = { Text("Nomor Invoice") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = billTo,
                    onValueChange = { billTo = it },
                    label = { Text("BILL TO") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // ============ NILAI TAGIHAN (PALING ATAS) ============
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("NILAI TAGIHAN", fontWeight = FontWeight.Bold, color = AdminPrimary, fontSize = 15.sp)

                OutlinedTextField(
                    value = totalTagihan,
                    onValueChange = { totalTagihan = it.filter { c -> c.isDigit() } },
                    label = { Text("Total Tagihan (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Riwayat Pembayaran", fontWeight = FontWeight.Bold, color = AdminPrimary, modifier = Modifier.weight(1f))
                    TextButton(onClick = { paymentHistory.add(0L) }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tambah", fontSize = 12.sp)
                    }
                }

                if (paymentHistory.isEmpty()) {
                    Text("Belum ada pembayaran.", fontSize = 11.sp, color = Color.Gray)
                }
                paymentHistory.forEachIndexed { idx, amount ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = if (amount == 0L) "" else amount.toString(),
                            onValueChange = {
                                val a = it.filter { c -> c.isDigit() }.toLongOrNull() ?: 0L
                                paymentHistory[idx] = a
                            },
                            label = { Text("Jumlah (Rp)") },
                            placeholder = { Text("0", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { paymentHistory.removeAt(idx) }) {
                            Icon(Icons.Default.Delete, "Hapus", tint = AdminDanger)
                        }
                    }
                }

                Divider(Modifier.padding(vertical = 6.dp))

                Row(Modifier.fillMaxWidth()) {
                    Text("Total Sudah Dibayar", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(formatRupiah(totalSudah), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth()) {
                    Text("Sisa Tagihan (Auto)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AdminPrimary, modifier = Modifier.weight(1f))
                    Text(
                        formatRupiah(sisaTagihanClamped),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sisaTagihanClamped <= 0) AdminSuccess else AdminDanger
                    )
                }
                if (sisaTagihan < 0) {
                    Text("⚠ Pembayaran melebihi tagihan. Sisa dikunci ke Rp 0.", fontSize = 10.sp, color = AdminDanger)
                }
                Text("Rumus: Sisa = Total - Total Sudah Dibayar", fontSize = 10.sp, color = Color.Gray)
            }
        }

        // ============ TOMBOL SIMPAN (WAJIB DULU) ============
        Button(
            onClick = {
                if (totalTagihanLong <= 0) {
                    saveMsg = "Total tagihan wajib > 0"
                    saveMsgError = true
                    return@Button
                }
                saving = true
                saveMsg = ""
                saveMsgError = false
                scope.launch {
                    try {
                        val resp = withContext(Dispatchers.IO) {
                            AdminApiClient.service.jamaahUpdate(mapOf(
                                "admin_id" to adminId,
                                "token" to token,
                                "jamaah_id" to jamaah.id.toString(),
                                "total_tagihan" to totalTagihanLong.toString(),
                                "sudah_dibayar" to totalSudah.toString()
                            ))
                        }
                        if (resp.success == true) {
                            savedSnapshot = currentSnapshot
                            saveMsg = "Tersimpan. Sekarang bisa generate invoice."
                            saveMsgError = false
                        } else {
                            saveMsg = resp.error ?: "Gagal simpan"
                            saveMsgError = true
                        }
                    } catch (e: Exception) {
                        saveMsg = "Error: ${e.message}"
                        saveMsgError = true
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
            else Text("SIMPAN NILAI TAGIHAN", fontWeight = FontWeight.Bold)
        }
        if (saveMsg.isNotEmpty()) {
            Text(saveMsg, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = if (saveMsgError) AdminDanger else AdminSuccess)
        }
        if (hasUnsavedChanges && savedSnapshot != null) {
            Text("⚠ Ada perubahan belum disimpan. Klik SIMPAN lagi.", fontSize = 11.sp, color = AdminDanger)
        }

        Divider()

        // ============ DETAIL PERJALANAN ============
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Detail Perjalanan", fontWeight = FontWeight.Bold, color = AdminPrimary)
                OutlinedTextField(
                    value = keberangkatan,
                    onValueChange = { keberangkatan = it },
                    label = { Text("Tanggal Keberangkatan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = hotelMadinah,
                    onValueChange = { hotelMadinah = it },
                    label = { Text("Hotel Madinah") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = hotelMakkah,
                    onValueChange = { hotelMakkah = it },
                    label = { Text("Hotel Makkah") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // ============ ITEMS ============
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Items", fontWeight = FontWeight.Bold, color = AdminPrimary, modifier = Modifier.weight(1f))
                    TextButton(onClick = { items.add(InvoiceItem("", 1, 0)) }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tambah", fontSize = 12.sp)
                    }
                }
                items.forEachIndexed { idx, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = item.description,
                                onValueChange = { items[idx] = item.copy(description = it) },
                                label = { Text("Deskripsi") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(
                                    value = item.quantity.toString(),
                                    onValueChange = {
                                        val q = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                                        items[idx] = item.copy(quantity = q)
                                    },
                                    label = { Text("Qty") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = item.price.toString(),
                                    onValueChange = {
                                        val p = it.filter { c -> c.isDigit() }.toLongOrNull() ?: 0L
                                        items[idx] = item.copy(price = p)
                                    },
                                    label = { Text("Harga") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                        IconButton(onClick = { items.removeAt(idx) }, enabled = items.size > 1) {
                            Icon(Icons.Default.Delete, "Hapus", tint = AdminDanger)
                        }
                    }
                    Divider(Modifier.padding(vertical = 4.dp))
                }
            }
        }

        // ============ FASILITAS ============
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Fasilitas (satu per baris)", fontWeight = FontWeight.Bold, color = AdminPrimary)
                OutlinedTextField(
                    value = fasilitasText,
                    onValueChange = { fasilitasText = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
                )
                Text("Fasilitas Belum Termasuk", fontWeight = FontWeight.Bold, color = AdminPrimary)
                OutlinedTextField(
                    value = excludedText,
                    onValueChange = { excludedText = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                )
            }
        }

        // ============ TOMBOL GENERATE (hanya aktif setelah SIMPAN) ============
        Button(
            onClick = {
                generating = true
                generateMsg = ""
                generateMsgError = false
                resultUrl = ""
                scope.launch {
                    try {
                        val paymentsList = paymentHistory
                            .filter { it > 0 }
                            .mapIndexed { _, amount ->
                                InvoicePayment(
                                    date = System.currentTimeMillis(),
                                    amount = amount,
                                    method = "bank"
                                )
                            }
                        val req = InvoiceRequest(
                            jamaah_id = jamaah.id.toString(),
                            invoice_number = invoiceNumber.trim(),
                            bill_to = billTo.trim(),
                            invoice_date = System.currentTimeMillis(),
                            payment_due = System.currentTimeMillis(),
                            items = items.toList(),
                            total = totalTagihanLong,
                            amount_due = sisaTagihanClamped,
                            keberangkatan = keberangkatan.ifBlank { null },
                            hotel_madinah = hotelMadinah.ifBlank { null },
                            hotel_makkah = hotelMakkah.ifBlank { null },
                            fasilitas = fasilitasText.lines().map { it.trim() }.filter { it.isNotBlank() },
                            fasilitas_excluded = excludedText.lines().map { it.trim() }.filter { it.isNotBlank() },
                            payments = paymentsList
                        )
                        val resp = withContext(Dispatchers.IO) {
                            InvoiceApiClient.service.generateInvoice(ApiConfig.INVOICE_API_KEY, req)
                        }
                        if (resp.success == true && !resp.pdf_url.isNullOrBlank()) {
                            resultUrl = resp.pdf_url
                            generateMsg = "Invoice berhasil dibuat!"
                            generateMsgError = false
                        } else {
                            generateMsg = resp.error ?: "Gagal generate invoice"
                            generateMsgError = true
                        }
                    } catch (e: Exception) {
                        generateMsg = "Error: ${e.message}"
                        generateMsgError = true
                    }
                    generating = false
                }
            },
            enabled = canGenerate && !generating,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canGenerate) AdminSuccess else Color.Gray
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (generating) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text(
                if (canGenerate) "GENERATE INVOICE" else "SIMPAN DULU SEBELUM GENERATE",
                fontWeight = FontWeight.Bold
            )
        }

        // ============ HASIL ============
        if (generateMsg.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (generateMsgError) Color(0xFFFEE2E2) else Color(0xFFD1FAE5)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(generateMsg, color = if (generateMsgError) AdminDanger else AdminSuccess, fontWeight = FontWeight.Bold)
                    if (resultUrl.isNotBlank()) {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resultUrl)))
                                } catch (e: Exception) { }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AdminSuccess)
                        ) { Text("BUKA PDF DI BROWSER") }
                        Text(resultUrl, fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}
