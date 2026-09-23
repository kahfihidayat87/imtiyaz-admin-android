package com.imtiyaztour.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AdminPrimary, AdminPrimaryDark))),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("Imtiyaz Admin",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = AdminPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Panel manajemen jamaah & pembayaran",
                    fontSize = 12.sp,
                    color = AdminTextGray)
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                if (errorMsg.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(errorMsg, fontSize = 12.sp, color = AdminDanger)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            errorMsg = "Username & password wajib diisi"
                            return@Button
                        }
                        isLoading = true
                        errorMsg = ""
                        scope.launch {
                            try {
                                val resp = withContext(Dispatchers.IO) {
                                    AdminApiClient.service.login(
                                        AdminLoginRequest(username.trim(), password)
                                    )
                                }
                                if (resp.success == true && resp.token != null && resp.admin_id != null) {
                                    Prefs.saveLogin(
                                        context,
                                        resp.admin_id.toString(),
                                        resp.token,
                                        resp.nama ?: "",
                                        resp.role ?: "admin",
                                        resp.kanal_id ?: ""
                                    )
                                    onLoggedIn()
                                } else {
                                    errorMsg = resp.error ?: "Login gagal"
                                }
                            } catch (e: Exception) {
                                errorMsg = "Koneksi gagal: " + (e.message ?: "unknown")
                            }
                            isLoading = false
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = androidx.compose.ui.graphics.Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Masuk", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Hubungi Super Admin untuk mendapatkan akun.",
                    fontSize = 10.sp,
                    color = AdminTextGray)
            }
        }
    }
}
