package com.imtiyaztour.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorRadioScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<GelombangMonitor>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }

    fun loadData() {
        scope.launch {
            loading = true
            try {
                val snap = FirebaseFirestore.getInstance().collection("gelombang")
                    .get().await()
                list = snap.documents
                    .mapNotNull { it.toObject(GelombangMonitor::class.java) }
                    .sortedByDescending { it.created_at }
            } catch (e: Exception) {
                snack.showSnackbar("Gagal memuat: ${e.message ?: "kesalahan tidak diketahui"}")
            } finally {
                loading = false
                refreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monitor Radio Aktif", fontSize = 17.sp) },
                actions = {
                    IconButton(onClick = { refreshing = true; loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AdminPrimary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            if (loading || refreshing) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = AdminPrimary)
            } else if (list.isEmpty()) {
                Text("Tidak ada gelombang di database", modifier = Modifier.align(Alignment.Center), color = AdminTextGray)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list, key = { it.kode }) { item ->
                        MonitorCard(item) {
                            scope.launch {
                                val adminId = Prefs.getAdminId(context)
                                val token = Prefs.getToken(context)
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        AdminApiClient.service.adminRadioRevoke(
                                            mapOf(
                                                "admin_id" to adminId,
                                                "token" to token,
                                                "kode" to item.kode
                                            )
                                        )
                                    }
                                }

                                val response = result.getOrNull()
                                if (result.isSuccess && response?.success == true) {
                                    snack.showSnackbar("Gelombang ${item.kode} berhasil dihapus.")
                                    loadData()
                                } else {
                                    val message = response?.error
                                        ?: response?.message
                                        ?: result.exceptionOrNull()?.localizedMessage
                                        ?: "Server tidak mengonfirmasi penghapusan."
                                    snack.showSnackbar("Gagal menghapus gelombang ${item.kode}: $message")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonitorCard(item: GelombangMonitor, onRevoke: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("HH:mm", Locale("id", "ID"))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.kode, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AdminPrimary)
                Text("${item.jumlahPeserta} Pendengar", fontSize = 12.sp, color = AdminSuccess, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(item.nama, fontSize = 13.sp, color = AdminText)
            Text("TL: ${item.tl_nama}", fontSize = 12.sp, color = AdminTextGray)
            if (item.created_at > 0) {
                Text("Mulai: ${sdf.format(Date(item.created_at))}", fontSize = 11.sp, color = AdminTextGray)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { showConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                modifier = Modifier.fillMaxWidth().height(36.dp)
            ) {
                Text("HAPUS GELOMBANG", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Hapus Gelombang?") },
            text = { Text("Gelombang ${item.kode} akan dihentikan dan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onRevoke() }) {
                    Text("HAPUS", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("BATAL") } }
        )
    }
}
