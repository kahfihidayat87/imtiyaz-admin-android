package com.imtiyaztour.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun SayaScreen(onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val nama = Prefs.getNama(context)
    val role = Prefs.getRole(context)
    val adminId = Prefs.getAdminId(context)
    val token = Prefs.getToken(context)

    Column(Modifier.fillMaxSize().background(AdminBg).padding(16.dp)) {
        Text("Profil Admin", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = AdminPrimary)
        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(nama.ifBlank { "Admin" },
                    fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AdminPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Role: ${role.uppercase()}", fontSize = 12.sp, color = AdminTextGray)
                Text("Admin ID: $adminId", fontSize = 11.sp, color = AdminTextGray)
            }
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            AdminApiClient.service.logout(
                                mapOf("admin_id" to adminId, "token" to token)
                            )
                        }
                    } catch (e: Exception) { }
                    Prefs.clearLogin(context)
                    onLoggedOut()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AdminDanger),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Keluar", fontWeight = FontWeight.Bold)
        }
    }
}
