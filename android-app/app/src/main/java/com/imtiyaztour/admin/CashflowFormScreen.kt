package com.imtiyaztour.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashflowFormScreen(
    existing: Cashflow?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val isEditing = existing != null

    var tipe by remember { mutableStateOf(existing?.tipeEnum() ?: CashflowTipe.KELUAR) }
    var akun by remember { mutableStateOf(existing?.akunEnum() ?: CashflowAkun.KAS_OPERASIONAL) }
    var kategori by remember { mutableStateOf(existing?.kategori ?: "") }
    var nominalStr by remember { mutableStateOf(existing?.nominal?.toString() ?: "") }
    var tanggal by remember { mutableStateOf(existing?.tanggal ?: System.currentTimeMillis()) }
    var keterangan by remember { mutableStateOf(existing?.keterangan ?: "") }

    var expandedTipe by remember { mutableStateOf(false) }
    var expandedAkun by remember { mutableStateOf(false) }
    var expandedKategori by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    // Cache list kategori berdasarkan akun & tipe agar tidak flicker
    val currentKategoriList = remember(akun, tipe) {
        CashflowKategori.getBy(akun, tipe)
    }

    // Reset kategori jika akun atau tipe berubah dan kategori saat ini tidak valid
    LaunchedEffect(akun, tipe) {
        if (kategori !in currentKategoriList) {
            kategori = currentKategoriList.firstOrNull() ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Transaksi" else "Catat Transaksi", fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AdminPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tipe Dropdown
            ExposedDropdownMenuBox(expanded = expandedTipe, onExpandedChange = { if (!saving) expandedTipe = it }) {
                OutlinedTextField(
                    value = tipe.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipe Transaksi") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipe) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expandedTipe, onDismissRequest = { expandedTipe = false }) {
                    CashflowTipe.values().forEach {
                        DropdownMenuItem(text = { Text(it.label) }, onClick = { tipe = it; expandedTipe = false })
                    }
                }
            }

            // Akun Dropdown
            ExposedDropdownMenuBox(expanded = expandedAkun, onExpandedChange = { if (!saving) expandedAkun = it }) {
                OutlinedTextField(
                    value = akun.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sumber Kas") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAkun) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expandedAkun, onDismissRequest = { expandedAkun = false }) {
                    CashflowAkun.values().forEach {
                        DropdownMenuItem(text = { Text(it.label) }, onClick = { akun = it; expandedAkun = false })
                    }
                }
            }

            // Kategori Dropdown (Dinamis)
            ExposedDropdownMenuBox(expanded = expandedKategori, onExpandedChange = { if (!saving) expandedKategori = it }) {
                OutlinedTextField(
                    value = kategori,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kategori") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKategori) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expandedKategori, onDismissRequest = { expandedKategori = false }) {
                    currentKategoriList.forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { kategori = it; expandedKategori = false })
                    }
                }
            }

            // Nominal
            OutlinedTextField(
                value = nominalStr,
                onValueChange = { if (it.all { char -> char.isDigit() }) nominalStr = it },
                label = { Text("Nominal (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Tanggal
            OutlinedButton(
                onClick = { if (!saving) showDatePicker = true },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.DateRange, null)
                Spacer(Modifier.width(8.dp))
                Text(SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date(tanggal)))
            }

            // Keterangan
            OutlinedTextField(
                value = keterangan,
                onValueChange = { keterangan = it },
                label = { Text("Keterangan (opsional)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Tombol Simpan
            Button(
                onClick = {
                    val nominal = nominalStr.toLongOrNull() ?: 0L
                    if (nominal <= 0L) {
                        scope.launch { snack.showSnackbar("Nominal harus lebih dari 0") }
                        return@Button
                    }
                    if (kategori.isBlank()) {
                        scope.launch { snack.showSnackbar("Pilih kategori terlebih dahulu") }
                        return@Button
                    }
                    scope.launch {
                        saving = true
                        val p = Cashflow(
                            id = existing?.id ?: "",
                            tipe = tipe.name,
                            akun = akun.name,
                            nominal = nominal,
                            kategori = kategori,
                            tanggal = tanggal,
                            keterangan = keterangan.trim()
                        )
                        val res = withContext(Dispatchers.IO) {
                            if (isEditing) CashflowRepository.update(existing!!.id, p)
                            else CashflowRepository.create(p, "admin")
                        }
                        saving = false
                        res.onSuccess {
                            snack.showSnackbar("Transaksi tersimpan")
                            onSaved()
                        }.onFailure {
                            snack.showSnackbar("Gagal simpan: ${it.message}")
                        }
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                else {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(6.dp))
                    Text("SIMPAN")
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = tanggal)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    tanggal = state.selectedDateMillis ?: tanggal
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } }
        ) { DatePicker(state = state) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Transaksi?") },
            text = { Text("Data yang dihapus tidak dapat dikembalikan.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        saving = true
                        withContext(Dispatchers.IO) { CashflowRepository.delete(existing!!.id) }
                        saving = false
                        showDeleteDialog = false
                        onSaved()
                    }
                }) { Text("HAPUS", color = Color(0xFFDC2626)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("BATAL") } }
        )
    }
}
