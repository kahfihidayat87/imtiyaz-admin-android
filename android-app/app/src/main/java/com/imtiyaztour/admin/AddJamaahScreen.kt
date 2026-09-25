package com.imtiyaztour.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ============================================================================
// [JAMAAH-CREATE] Form tambah jamaah baru
// Hanya bisa diakses oleh role admin & super_admin.
// Paket ID & Kanal Radio ID disembunyikan — edit via WP Admin.
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddJamaahScreen(onBack: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminId = Prefs.getAdminId(context)
    val token = Prefs.getToken(context)

    var nama by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var totalTagihan by remember { mutableStateOf("") }
    var sudahDibayar by remember { mutableStateOf("0") }
    var saving by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(AdminBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text("<- Kembali", color = AdminPrimary) }
        Text("Tambah Jamaah", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = AdminPrimary)
        Text("Buat akun jamaah baru. Login jamaah: pakai username & password ini.",
            fontSize = 11.sp, color = AdminTextGray)
        Spacer(Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Data Jamaah", fontWeight = FontWeight.Bold, color = AdminPrimary)

                OutlinedTextField(nama, { nama = it },
                    label = { Text("Nama Lengkap") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(10.dp))

                OutlinedTextField(username, { username = it },
                    label = { Text("Username Login (min 3)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(10.dp))

                OutlinedTextField(password, { password = it },
                    label = { Text("Password Login (min 6)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    visualTransformation = PasswordVisualTransformation())

                OutlinedTextField(totalTagihan, { totalTagihan = it.filter { c -> c.isDigit() } },
                    label = { Text("Total Tagihan (Rp)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp))

                OutlinedTextField(sudahDibayar, { sudahDibayar = it.filter { c -> c.isDigit() } },
                    label = { Text("Sudah Dibayar (Rp) - default 0") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp))

                val total = totalTagihan.toLongOrNull() ?: 0L
                val sudah = sudahDibayar.toLongOrNull() ?: 0L
                val sisa = if (total - sudah > 0) total - sudah else 0L
                Text("Sisa Tagihan (auto): " + formatRupiah(sisa),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = if (sisa <= 0 && total > 0) AdminSuccess else AdminDanger)
            }
        }

        Button(
            onClick = {
                if (nama.isBlank()) { msg = "Nama wajib diisi"; isError = true; return@Button }
                if (username.length < 3) { msg = "Username min 3 karakter"; isError = true; return@Button }
                if (password.length < 6) { msg = "Password min 6 karakter"; isError = true; return@Button }
                if ((totalTagihan.toIntOrNull() ?: 0) <= 0) { msg = "Total tagihan wajib > 0"; isError = true; return@Button }

                saving = true; msg = ""; isError = false
                scope.launch {
                    try {
                        val resp = withContext(Dispatchers.IO) {
                            AdminApiClient.service.jamaahCreate(JamaahCreateRequest(
                                admin_id = adminId,
                                token = token,
                                nama = nama.trim(),
                                username = username.trim(),
                                password = password,
                                total_tagihan = totalTagihan.toIntOrNull() ?: 0,
                                sudah_dibayar = sudahDibayar.toIntOrNull() ?: 0
                            ))
                        }
                        if (resp.success == true) {
                            msg = resp.message ?: "Berhasil ditambahkan"
                            isError = false
                            kotlinx.coroutines.delay(800)
                            onSuccess()
                        } else {
                            msg = resp.error ?: "Gagal tambah jamaah"
                            isError = true
                        }
                    } catch (e: Exception) {
                        msg = "Error: " + (e.message ?: "unknown")
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
            if (saving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("SIMPAN JAMAAH BARU", fontWeight = FontWeight.Bold)
        }

        if (msg.isNotEmpty()) {
            Text(msg, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = if (isError) AdminDanger else AdminSuccess)
        }
        Spacer(Modifier.height(24.dp))
    }
}
