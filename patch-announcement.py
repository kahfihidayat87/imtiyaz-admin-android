#!/usr/bin/env python3
"""
Patch admin app: tambah fitur Kirim Pengumuman.
- Update AdminApi.kt: method sendAnnouncement
- Buat PengumumanScreen.kt
- Update MainActivity.kt: tab Info
"""
import sys, os, shutil

JAVA = "android-app/app/src/main/java/com/imtiyaztour/admin"
if not os.path.isdir(JAVA):
    print("ERROR: jalankan dari root repo imtiyaz-admin-android")
    sys.exit(1)

def read(p):
    with open(p, "r", encoding="utf-8", newline="") as f:
        return f.read().replace("\r\n", "\n").replace("\r", "\n")

def write(p, c):
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(c)

def patch(c, find, repl, label, req=True):
    if find not in c:
        if req:
            print(f"\nERROR [{label}]: pola tidak ditemukan")
            print("  Cari: " + repr(find[:100]))
            sys.exit(2)
        print(f"  SKIP [{label}]")
        return c
    return c.replace(find, repl, 1)

# ============ 1. Update AdminApi.kt ============
print("[1/3] Update AdminApi.kt")
api_path = f"{JAVA}/AdminApi.kt"
shutil.copy(api_path, api_path + ".bak-ann")
c = read(api_path)

# Cek sudah di-patch atau belum
if "sendAnnouncement" in c:
    print("  SKIP: sudah di-patch")
else:
    # Tambah method di interface AdminApiService
    c = patch(c,
        "    @POST(\"wp-json/imtiyaz/v1/admin-jamaah\")\n"
        "    suspend fun jamaahList(@Body body: Map<String, Any>): JamaahListResponse\n"
        "}",
        "    @POST(\"wp-json/imtiyaz/v1/admin-jamaah\")\n"
        "    suspend fun jamaahList(@Body body: Map<String, Any>): JamaahListResponse\n"
        "\n"
        "    @POST(\"wp-json/imtiyaz/v1/admin-announcement\")\n"
        "    suspend fun sendAnnouncement(@Body body: Map<String, Any>): AnnouncementResponse\n"
        "}",
        "api-method")
    write(api_path, c)
    print("  OK")

# ============ 2. Tambah AnnouncementResponse di AdminModels.kt ============
print("[2/3] Update AdminModels.kt")
models_path = f"{JAVA}/AdminModels.kt"
shutil.copy(models_path, models_path + ".bak-ann")
c = read(models_path)

if "AnnouncementResponse" in c:
    print("  SKIP: sudah di-patch")
else:
    c = c + '''

// Response kirim pengumuman
data class AnnouncementResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val item: AnnouncementItem? = null,
    val error: String? = null
)

data class AnnouncementItem(
    val id: String? = null,
    val judul: String? = null,
    val pesan: String? = null,
    val tanggal: Long = 0L,
    val penting: Boolean = false,
    val dikirim_oleh: String? = null
)
'''
    write(models_path, c)
    print("  OK")

# ============ 3. Buat PengumumanScreen.kt ============
print("[3/3] Buat PengumumanScreen.kt")
pengumuman_content = '''package com.imtiyaztour.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun PengumumanScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminId = Prefs.getAdminId(context)
    val token = Prefs.getToken(context)

    var judul by remember { mutableStateOf("") }
    var pesan by remember { mutableStateOf("") }
    var penting by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var resultMsg by remember { mutableStateOf("") }
    var resultIsError by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(AdminBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Kirim Pengumuman",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = AdminPrimary)
        Text("Broadcast info ke semua jamaah aplikasi",
            fontSize = 12.sp,
            color = AdminTextGray)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = judul,
            onValueChange = { judul = it },
            label = { Text("Judul") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = pesan,
            onValueChange = { pesan = it },
            label = { Text("Pesan lengkap") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Tandai sebagai INFO PENTING",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E))
                    Text("Notifikasi lebih menonjol + warna kuning di app jamaah",
                        fontSize = 10.sp,
                        color = Color(0xFF92400E))
                }
                Switch(
                    checked = penting,
                    onCheckedChange = { penting = it }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (judul.isBlank() || pesan.isBlank()) {
                    resultMsg = "Judul dan pesan wajib diisi"
                    resultIsError = true
                    return@Button
                }
                isSending = true
                resultMsg = ""
                scope.launch {
                    try {
                        val resp = withContext(Dispatchers.IO) {
                            AdminApiClient.service.sendAnnouncement(mapOf(
                                "admin_id" to adminId,
                                "token" to token,
                                "judul" to judul.trim(),
                                "pesan" to pesan.trim(),
                                "penting" to penting
                            ))
                        }
                        if (resp.success == true) {
                            resultMsg = "Pengumuman terkirim ke semua jamaah!"
                            resultIsError = false
                            judul = ""
                            pesan = ""
                            penting = false
                        } else {
                            resultMsg = resp.error ?: "Gagal mengirim"
                            resultIsError = true
                        }
                    } catch (e: Exception) {
                        resultMsg = "Koneksi gagal: " + (e.message ?: "unknown")
                        resultIsError = true
                    }
                    isSending = false
                }
            },
            enabled = !isSending,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("KIRIM PENGUMUMAN", fontWeight = FontWeight.Bold)
            }
        }

        if (resultMsg.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (resultIsError) Color(0xFFFEE2E2) else Color(0xFFD1FAE5)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    resultMsg,
                    modifier = Modifier.padding(14.dp),
                    fontSize = 12.sp,
                    color = if (resultIsError) AdminDanger else AdminSuccess,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
'''
with open(f"{JAVA}/PengumumanScreen.kt", "w", encoding="utf-8", newline="\n") as f:
    f.write(pengumuman_content)
print("  OK: PengumumanScreen.kt dibuat")

# ============ 4. Update MainActivity.kt — tambah tab Info ============
print("[4/4] Update MainActivity.kt")
ma_path = f"{JAVA}/MainActivity.kt"
shutil.copy(ma_path, ma_path + ".bak-ann")
c = read(ma_path)

if "PengumumanScreen" in c:
    print("  SKIP: sudah di-patch")
else:
    # Tambah import icon
    c = patch(c,
        "import androidx.compose.material.icons.filled.Dashboard",
        "import androidx.compose.material.icons.filled.Campaign\n"
        "import androidx.compose.material.icons.filled.Dashboard",
        "ma-import")

    # Tambah NavigationBarItem untuk tab Info
    c = patch(c,
        "                NavigationBarItem(\n"
        "                    selected = selectedTab == 2,\n"
        "                    onClick = { selectedTab = 2 },\n"
        "                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },\n"
        "                    label = { Text(\"Saya\", fontSize = 10.sp) }\n"
        "                )",
        "                NavigationBarItem(\n"
        "                    selected = selectedTab == 2,\n"
        "                    onClick = { selectedTab = 2 },\n"
        "                    icon = { Icon(Icons.Default.Campaign, contentDescription = null) },\n"
        "                    label = { Text(\"Info\", fontSize = 10.sp) }\n"
        "                )\n"
        "                NavigationBarItem(\n"
        "                    selected = selectedTab == 3,\n"
        "                    onClick = { selectedTab = 3 },\n"
        "                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },\n"
        "                    label = { Text(\"Saya\", fontSize = 10.sp) }\n"
        "                )",
        "ma-nav")

    # Update when() untuk handle tab 2 & 3
    c = patch(c,
        "                0 -> DashboardScreen()\n"
        "                1 -> JamaahListScreen()\n"
        "                2 -> SayaScreen(onLoggedOut = {\n"
        "                    isLoggedIn = false\n"
        "                    selectedTab = 0\n"
        "                })",
        "                0 -> DashboardScreen()\n"
        "                1 -> JamaahListScreen()\n"
        "                2 -> PengumumanScreen()\n"
        "                3 -> SayaScreen(onLoggedOut = {\n"
        "                    isLoggedIn = false\n"
        "                    selectedTab = 0\n"
        "                })",
        "ma-when")

    write(ma_path, c)
    print("  OK")

print("\n" + "=" * 60)
print("PATCH PENGUMUMAN SELESAI")
print("=" * 60)
print("\nSelanjutnya:")
print("  1. git add . && git commit -m 'feat: kirim pengumuman'")
print("  2. git push origin main")
print("  3. Tunggu CI ~5 menit")