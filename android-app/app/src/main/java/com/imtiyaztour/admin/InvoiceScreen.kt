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
// [INVOICE] v1.0 — Form generate invoice PDF
// Alur: Admin isi form -> klik Generate -> API panggil Node.js -> PDF dibuat
// -> URL PDF dibuka di browser (via Intent.ACTION_VIEW)
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

    // Form state
    var invoiceNumber by remember {
        mutableStateOf("INV-${jamaah.id}-${System.currentTimeMillis() / 1000}")
    }
    var billTo by remember { mutableStateOf(jamaah.nama) }
    var keberangkatan by remember { mutableStateOf("") }
    var hotelMadinah by remember { mutableStateOf("") }
    var hotelMakkah by remember { mutableStateOf("") }
    var totalTagihan by remember {
        mutableStateOf(if (jamaah.total_tagihan > 0) jamaah.total_tagihan.toString() else "0")
    }
    var amountDue by remember {
        mutableStateOf(if (jamaah.sisa_tagihan > 0) jamaah.sisa_tagihan.toString() else "0")
    }
    var fasilitasText by remember { mutableStateOf(DEFAULT_FASILITAS.joinToString("\n")) }
    var excludedText by remember { mutableStateOf(DEFAULT_EXCLUDED.joinToString("\n")) }

    // Items & Payments
    val items = remember {
        mutableStateListOf(
            InvoiceItem(
                description = "Paket Umrah",
                quantity = 1,
                price = jamaah.total_tagihan
            )
        )
    }
    val payments = remember { mutableStateListOf<InvoicePayment>() }

    // Result state
    var saving by remember { mutableStateOf(false) }
    var resultMsg by remember { mutableStateOf("") }
    var resultUrl by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

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
                Text("ID: ${jamaah.id}", fontSize = 12.sp, color = Color.Gray)

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
                    label = { Text("BILL TO (nama penerima invoice)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // ============ DETAIL TRIP ============
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
                    label = { Text("Tanggal Keberangkatan (mis. 29 Maret 2026)") },
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

        // ============ NILAI TAGIHAN ============
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nilai Tagihan", fontWeight = FontWeight.Bold, color = AdminPrimary)
                OutlinedTextField(
                    value = totalTagihan,
                    onValueChange = { totalTagihan = it.filter { c -> c.isDigit() } },
                    label = { Text("Total Tagihan (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = amountDue,
                    onValueChange = { amountDue = it.filter { c -> c.isDigit() } },
                    label = { Text("Sisa Tagihan / Amount Due (Rp)") },
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
                    TextButton(onClick = {
                        items.add(InvoiceItem("", 1, 0))
                    }) {
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
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
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

        // ============ PAYMENT HISTORY ============
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Riwayat Pembayaran", fontWeight = FontWeight.Bold, color = AdminPrimary, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        payments.add(InvoicePayment(System.currentTimeMillis(), 0L, "bank"))
                    }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tambah", fontSize = 12.sp)
                    }
                }
                if (payments.isEmpty()) {
                    Text("Belum ada pembayaran dicatat (opsional).", fontSize = 11.sp, color = Color.Gray)
                }
                payments.forEachIndexed { idx, p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = p.amount.toString(),
                            onValueChange = {
                                val a = it.filter { c -> c.isDigit() }.toLongOrNull() ?: 0L
                                payments[idx] = p.copy(amount = a)
                            },
                            label = { Text("Jumlah (Rp)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { payments.removeAt(idx) }) {
                            Icon(Icons.Default.Delete, "Hapus", tint = AdminDanger)
                        }
                    }
                }
            }
        }

        // ============ TOMBOL GENERATE ============
        Button(
            onClick = {
                if (invoiceNumber.isBlank()) {
                    resultMsg = "Nomor invoice wajib diisi"
                    isError = true
                    return@Button
                }
                if (billTo.isBlank()) {
                    resultMsg = "BILL TO wajib diisi"
                    isError = true
                    return@Button
                }
                if (items.isEmpty() || items.any { it.description.isBlank() }) {
                    resultMsg = "Minimal 1 item dengan deskripsi"
                    isError = true
                    return@Button
                }

                saving = true
                resultMsg = ""
                resultUrl = ""
                isError = false

                scope.launch {
                    try {
                        val req = InvoiceRequest(
                            jamaah_id = jamaah.id.toString(),
                            invoice_number = invoiceNumber.trim(),
                            bill_to = billTo.trim(),
                            invoice_date = System.currentTimeMillis(),
                            payment_due = System.currentTimeMillis(),
                            items = items.toList(),
                            total = totalTagihan.toLongOrNull() ?: 0L,
                            amount_due = amountDue.toLongOrNull() ?: 0L,
                            keberangkatan = keberangkatan.ifBlank { null },
                            hotel_madinah = hotelMadinah.ifBlank { null },
                            hotel_makkah = hotelMakkah.ifBlank { null },
                            fasilitas = fasilitasText.lines().map { it.trim() }.filter { it.isNotBlank() },
                            fasilitas_excluded = excludedText.lines().map { it.trim() }.filter { it.isNotBlank() },
                            payments = payments.toList()
                        )
                        val resp = withContext(Dispatchers.IO) {
                            InvoiceApiClient.service.generateInvoice(ApiConfig.INVOICE_API_KEY, req)
                        }
                        if (resp.success == true && !resp.pdf_url.isNullOrBlank()) {
                            resultUrl = resp.pdf_url
                            resultMsg = "Invoice berhasil dibuat!"
                            isError = false
                        } else {
                            resultMsg = resp.error ?: "Gagal generate invoice"
                            isError = true
                        }
                    } catch (e: Exception) {
                        resultMsg = "Error: ${e.message}"
                        isError = true
                    }
                    saving = false
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (saving) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("GENERATE INVOICE", fontWeight = FontWeight.Bold)
            }
        }

        // ============ HASIL ============
        if (resultMsg.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) Color(0xFFFEE2E2) else Color(0xFFD1FAE5)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        resultMsg,
                        color = if (isError) AdminDanger else AdminSuccess,
                        fontWeight = FontWeight.Bold
                    )
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
