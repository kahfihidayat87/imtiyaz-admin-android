package com.imtiyaztour.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProspekScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit = {},
    existingProspek: Prospek? = null
) {
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val context = LocalContext.current

    val isEditing = existingProspek != null
    var nama by remember { mutableStateOf(existingProspek?.nama ?: "") }
    var kota by remember { mutableStateOf(existingProspek?.kota ?: "") }
    var noWa by remember { mutableStateOf(existingProspek?.no_wa ?: "") }
    var umrahUntuk by remember { mutableStateOf(existingProspek?.umrahUntukEnum() ?: UmrahUntuk.DIRI_SENDIRI) }
    var tglKantor by remember { mutableStateOf<Long?>(existingProspek?.tgl_kantor) }
    var catatan by remember { mutableStateOf(existingProspek?.catatan ?: "") }

    var expandedUmrah by remember { mutableStateOf(false) }
    var showDateKantor by remember { mutableStateOf(false) }
    var showDateFu by remember { mutableStateOf(false) }

    var saving by remember { mutableStateOf(false) }
    var createdId by remember { mutableStateOf<String?>(null) }
    var fuSaved by remember { mutableStateOf(false) }

    val readOnly = createdId != null && !isEditing

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Prospek" else if (readOnly) "Detail Prospek" else "Tambah Prospek", fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = nama, onValueChange = { if (!readOnly) nama = it },
                label = { Text("Nama Prospek *") },
                singleLine = true, readOnly = readOnly,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = kota, onValueChange = { if (!readOnly) kota = it },
                label = { Text("Kota *") },
                singleLine = true, readOnly = readOnly,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = noWa, onValueChange = { if (!readOnly) noWa = it },
                label = { Text("No. WhatsApp *") },
                singleLine = true, readOnly = readOnly,
                modifier = Modifier.fillMaxWidth()
            )
            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { if (!readOnly) expandedUmrah = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(umrahUntuk.label, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = expandedUmrah,
                    onDismissRequest = { expandedUmrah = false }
                ) {
                    UmrahUntuk.values().forEach { u ->
                        DropdownMenuItem(
                            text = { Text(u.label) },
                            onClick = { umrahUntuk = u; expandedUmrah = false }
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = { if (!readOnly) showDateKantor = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Icon(Icons.Default.DateRange, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (tglKantor == null) "Tgl ke kantor / WA Admin"
                    else SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(tglKantor!!))
                )
            }
            OutlinedTextField(
                value = catatan, onValueChange = { if (!readOnly) catatan = it },
                label = { Text("Catatan") },
                minLines = 2, maxLines = 4, readOnly = readOnly,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            if (createdId == null || isEditing) {
                Button(
                    onClick = {
                        if (nama.isBlank() || kota.isBlank() || noWa.isBlank()) {
                            scope.launch { snack.showSnackbar("Nama, Kota, dan No. WA wajib diisi") }
                            return@Button
                        }
                        scope.launch {
                            saving = true
                            if (isEditing && existingProspek != null) {
                                val res = withContext(Dispatchers.IO) {
                                    ProspekRepository.update(
                                        existingProspek.id,
                                        existingProspek.copy(
                                            nama = nama.trim(),
                                            kota = kota.trim(),
                                            no_wa = noWa.trim(),
                                            umrah_untuk = umrahUntuk.name,
                                            tgl_kantor = tglKantor,
                                            catatan = catatan.trim()
                                        )
                                    )
                                }
                                saving = false
                                res.onSuccess { snack.showSnackbar("Prospek diperbarui") }
                                    .onFailure { snack.showSnackbar("Gagal simpan: ${it.message}") }
                            } else {
                                val res = withContext(Dispatchers.IO) {
                                    ProspekRepository.create(
                                        Prospek(
                                            nama = nama.trim(),
                                            kota = kota.trim(),
                                            no_wa = noWa.trim(),
                                            umrah_untuk = umrahUntuk.name,
                                            tgl_kantor = tglKantor,
                                            catatan = catatan.trim()
                                        ),
                                        createdBy = Prefs.getAdminId(context)
                                    )
                                }
                                saving = false
                                res.onSuccess { newId ->
                                    createdId = newId
                                    snack.showSnackbar("Prospek tersimpan")
                                }.onFailure {
                                    snack.showSnackbar("Gagal simpan: ${it.message}")
                                }
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
                        Text(if (isEditing) "SIMPAN PERUBAHAN" else "SIMPAN")
                    }
                }
            }
            if (createdId != null || isEditing) {
                Button(
                    onClick = { showDateFu = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(containerColor = AdminSuccess)
                ) {
                    Icon(Icons.Default.DateRange, null)
                    Spacer(Modifier.width(6.dp))
                    val hasFu = fuSaved || (existingProspek?.fu_date ?: 0L) > 0L
                    Text(if (hasFu) "UBAH TGL. FOLLOW UP" else "SET TGL. FOLLOW UP (FU)")
                }
                if (fuSaved || (existingProspek?.fu_date ?: 0L) > 0L) {
                    OutlinedButton(
                        onClick = { onSaved() },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Kembali ke Daftar") }
                }
            }
        }
    }

    if (showDateKantor) {
        val state = rememberDatePickerState(initialSelectedDateMillis = tglKantor ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDateKantor = false },
            confirmButton = {
                TextButton(onClick = {
                    tglKantor = state.selectedDateMillis
                    showDateKantor = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDateKantor = false }) { Text("Batal") }
            }
        ) { DatePicker(state = state) }
    }

    if (showDateFu) {
        val state = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDateFu = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = state.selectedDateMillis
                    showDateFu = false
                    val targetId = createdId ?: existingProspek?.id
                    if (picked != null && targetId != null) {
                        scope.launch {
                            val r = withContext(Dispatchers.IO) {
                                ProspekRepository.updateFuDate(targetId, picked)
                            }
                            r.onSuccess {
                                fuSaved = true
                                snack.showSnackbar("Tgl FU disimpan")
                                ProspekReminderScheduler.scheduleExact(context, targetId, nama, picked)
                            }.onFailure {
                                snack.showSnackbar("Gagal simpan FU: ${it.message}")
                            }
                        }
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDateFu = false }) { Text("Batal") }
            }
        ) { DatePicker(state = state) }
    }
}
