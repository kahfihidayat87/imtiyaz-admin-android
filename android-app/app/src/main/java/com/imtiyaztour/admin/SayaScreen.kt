package com.imtiyaztour.admin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
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
fun SayaScreen(
    onLoggedOut: () -> Unit,
    onKajianClick: () -> Unit = {},
    onProspekClick: () -> Unit = {},
    onAdminClick: () -> Unit = {},
    showProspek: Boolean = true,
    showKajian: Boolean = true,
    showAdmin: Boolean = false
) {
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

        // --- TOMBOL PROSPEK (hanya untuk role yang berwenang) ---
        if (showProspek) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onProspekClick() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = AdminPrimary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Prospek Jamaah", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Kelola data calon jamaah & follow up", fontSize = 11.sp, color = AdminTextGray)
                }
            }
        }
        }  // endif showProspek
        Spacer(Modifier.height(16.dp))

        // --- TOMBOL JADWAL KAJIAN MABRUR (hanya untuk role berwenang) ---
        if (showKajian) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onKajianClick() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = AdminPrimary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Jadwal Kajian Mabrur", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Kelola jadwal kajian untuk jamaah", fontSize = 11.sp, color = AdminTextGray)
                }
            }
        }
        }  // endif showKajian
        if (showAdmin) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onAdminClick() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = AdminPrimary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Kelola Admin", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Tambah, edit, nonaktifkan akun admin", fontSize = 11.sp, color = AdminTextGray)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        }  // endif showAdmin
        // ------------------------------------

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
                    FirebaseAuthHelper.logout()
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
