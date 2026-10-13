package com.imtiyaztour.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreen(
    onBack: () -> Unit,
    onSuccess: (tripId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nama by remember { mutableStateOf("") }
    var jumlahJamaah by remember { mutableStateOf("") }
    var catatan by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Trip", fontWeight = FontWeight.Bold, color = AdminPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AdminPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = nama, onValueChange = { nama = it },
                label = { Text("Nama Trip") },
                placeholder = { Text("misal: Linuwih 17 Okt 2026") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp), singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = jumlahJamaah, onValueChange = { jumlahJamaah = it.filter { c -> c.isDigit() } },
                label = { Text("Jumlah Jamaah (target)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp), singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = catatan, onValueChange = { catatan = it },
                label = { Text("Catatan (opsional)") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(10.dp)
            )
            if (errorMsg.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(errorMsg, color = AdminDanger, fontSize = 12.sp)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (nama.isBlank()) { errorMsg = "Nama trip wajib diisi"; return@Button }
                    isLoading = true; errorMsg = ""
                    scope.launch {
                        try {
                            val trip = Trip(
                                nama = nama.trim(),
                                jumlah_jamaah = jumlahJamaah.toIntOrNull() ?: 0,
                                catatan = catatan.trim(),
                                status = "planning"
                            )
                            val id = withContext(Dispatchers.IO) { TripRepository.createTrip(trip) }
                            onSuccess(id)
                        } catch (e: Exception) {
                            errorMsg = "Gagal simpan: ${e.message ?: "unknown"}"
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp),
                    color = Color.White, strokeWidth = 2.dp)
                else Text("Simpan", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
