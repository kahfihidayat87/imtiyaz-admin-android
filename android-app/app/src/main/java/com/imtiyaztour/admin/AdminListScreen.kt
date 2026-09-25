package com.imtiyaztour.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ROLE_OPTIONS = listOf("super_admin", "admin", "keuangan", "tl")

private fun roleColor(role: String): Color = when (role) {
    "super_admin" -> Color(0xFF7C3AED)
    "admin"       -> AdminPrimary
    "keuangan"    -> Color(0xFF0F7A5A)
    "tl"          -> Color(0xFFEA580C)
    else          -> Color.Gray
}

private fun roleLabel(role: String): String = when (role) {
    "super_admin" -> "SUPER ADMIN"
    "admin"       -> "ADMIN"
    "keuangan"    -> "KEUANGAN"
    "tl"          -> "TOUR LEADER"
    else          -> role.uppercase()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminListScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminId = Prefs.getAdminId(context)
    val token = Prefs.getToken(context)
    val myId = adminId.toIntOrNull() ?: 0

    var list by remember { mutableStateOf<List<AdminItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var reloadKey by remember { mutableStateOf(0) }

    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AdminItem?>(null) }
    var showResetPass by remember { mutableStateOf<AdminItem?>(null) }

    LaunchedEffect(reloadKey) {
        loading = true; errorMsg = ""
        try {
            val resp = withContext(Dispatchers.IO) {
                AdminApiClient.service.adminList(mapOf("admin_id" to adminId, "token" to token))
            }
            list = resp.admins
        } catch (e: Exception) {
            errorMsg = "Gagal memuat daftar admin: " + (e.message ?: "unknown")
        }
        loading = false
    }

    if (showCreate) {
        CreateAdminDialog(adminId, token,
            onDismiss = { showCreate = false },
            onSuccess = { showCreate = false; reloadKey++ })
        return
    }
    if (editing != null) {
        EditRoleDialog(editing!!, adminId, token,
            onDismiss = { editing = null },
            onSuccess = { editing = null; reloadKey++ })
        return
    }
    if (showResetPass != null) {
        ResetPasswordDialog(showResetPass!!, adminId, token,
            onDismiss = { showResetPass = null },
            onSuccess = { showResetPass = null })
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Kelola Admin", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = AdminPrimary)
            Text("Buat, ubah role, reset password, nonaktifkan admin",
                fontSize = 11.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { showCreate = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("TAMBAH ADMIN BARU", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (errorMsg.isNotEmpty()) {
                Text(errorMsg, fontSize = 12.sp, color = AdminDanger)
                TextButton(onClick = { reloadKey++ }) { Text("Coba Lagi") }
            }
            if (!loading && errorMsg.isEmpty()) {
                Text("" + list.size + " akun admin terdaftar",
                    fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }

        items(list) { a ->
            val isMe = a.id == myId
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp)
                                .background(roleColor(a.role), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(a.nama ?: a.username, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (isMe) {
                                    Spacer(Modifier.width(6.dp))
                                    Text("(Anda)", fontSize = 10.sp, color = AdminSuccess, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("@" + a.username, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.background(roleColor(a.role).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(roleLabel(a.role), fontSize = 10.sp,
                                color = roleColor(a.role), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.weight(1f))
                        Text("ID " + a.id, fontSize = 10.sp, color = Color.LightGray)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { editing = a },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isMe
                        ) { Text("Ubah Role", fontSize = 11.sp) }
                        OutlinedButton(
                            onClick = { showResetPass = a },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("Reset Pass", fontSize = 11.sp) }
                        if (!isMe) {
                            IconButton(onClick = {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            AdminApiClient.service.adminDelete(mapOf(
                                                "admin_id" to adminId,
                                                "token" to token,
                                                "target_id" to a.id.toString()
                                            ))
                                        }
                                        reloadKey++
                                    } catch (e: Exception) { }
                                }
                            }) {
                                Icon(Icons.Default.Delete, "Nonaktifkan", tint = AdminDanger)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAdminDialog(adminId: String, token: String,
    onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nama by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("admin") }
    var kanalId by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Tambah Admin Baru", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(username, { username = it },
                    label = { Text("Username (min 3)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(password, { password = it },
                    label = { Text("Password (min 8)") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(nama, { nama = it },
                    label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Role:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ROLE_OPTIONS.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            pair.forEach { r ->
                                FilterChip(
                                    selected = role == r,
                                    onClick = { role = r },
                                    label = { Text(roleLabel(r), fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                OutlinedTextField(kanalId, { kanalId = it },
                    label = { Text("Kanal Radio ID (opsional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (msg.isNotEmpty()) Text(msg, fontSize = 11.sp, color = AdminDanger)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (username.length < 3) { msg = "Username min 3"; return@TextButton }
                if (password.length < 8) { msg = "Password min 8"; return@TextButton }
                saving = true; msg = ""
                scope.launch {
                    try {
                        val resp = withContext(Dispatchers.IO) {
                            AdminApiClient.service.adminCreate(AdminCreateRequest(
                                admin_id = adminId, token = token,
                                username = username.trim(), password = password,
                                nama = nama.trim(), role = role, kanal_id = kanalId.trim()))
                        }
                        if (resp.success == true) onSuccess() else msg = resp.error ?: "Gagal"
                    } catch (e: Exception) { msg = "Error: " + (e.message ?: "") }
                    saving = false
                }
            }, enabled = !saving) {
                Text(if (saving) "Menyimpan..." else "SIMPAN", color = AdminPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRoleDialog(target: AdminItem, adminId: String, token: String,
    onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    var role by remember { mutableStateOf(target.role) }
    var saving by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Ubah Role: " + (target.nama ?: target.username), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Role saat ini: " + roleLabel(target.role), fontSize = 12.sp, color = Color.Gray)
                Text("Role baru:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                ROLE_OPTIONS.forEach { r ->
                    Row(Modifier.fillMaxWidth().clickable { role = r }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = role == r, onClick = { role = r })
                        Spacer(Modifier.width(6.dp))
                        Text(roleLabel(r), fontSize = 13.sp)
                    }
                }
                if (msg.isNotEmpty()) Text(msg, fontSize = 11.sp, color = AdminDanger)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                saving = true; msg = ""
                scope.launch {
                    try {
                        val resp = withContext(Dispatchers.IO) {
                            AdminApiClient.service.adminUpdateRole(mapOf(
                                "admin_id" to adminId, "token" to token,
                                "target_id" to target.id.toString(), "role" to role))
                        }
                        if (resp.success == true) onSuccess() else msg = resp.error ?: "Gagal"
                    } catch (e: Exception) { msg = "Error: " + (e.message ?: "") }
                    saving = false
                }
            }, enabled = !saving && role != target.role) {
                Text(if (saving) "..." else "UBAH", color = AdminPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResetPasswordDialog(target: AdminItem, adminId: String, token: String,
    onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    var newPass by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Reset password untuk:", fontSize = 12.sp, color = Color.Gray)
                Text(target.nama ?: target.username, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(newPass, { newPass = it },
                    label = { Text("Password Baru (min 8)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = PasswordVisualTransformation())
                Text("Admin akan logout otomatis setelah reset.", fontSize = 10.sp, color = Color.Gray)
                if (msg.isNotEmpty()) Text(msg, fontSize = 11.sp, color = AdminDanger)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (newPass.length < 8) { msg = "Password min 8"; return@TextButton }
                saving = true; msg = ""
                scope.launch {
                    try {
                        val resp = withContext(Dispatchers.IO) {
                            AdminApiClient.service.adminResetPassword(mapOf(
                                "admin_id" to adminId, "token" to token,
                                "target_id" to target.id.toString(),
                                "new_password" to newPass))
                        }
                        if (resp.success == true) onSuccess() else msg = resp.error ?: "Gagal"
                    } catch (e: Exception) { msg = "Error: " + (e.message ?: "") }
                    saving = false
                }
            }, enabled = !saving) {
                Text(if (saving) "..." else "RESET", color = AdminPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Batal") } }
    )
}
