package com.imtiyaztour.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProspekFollowUpDialog(prospekId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var prospek by remember { mutableStateOf<Prospek?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(prospekId) {
        loading = true
        prospek = withContext(Dispatchers.IO) { ProspekRepository.getById(prospekId) }
        loading = false
    }

    if (loading) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Memuat...") },
            text = { Text("Mengambil data prospek") },
            confirmButton = {}
        )
        return
    }

    val p = prospek
    if (p == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Data tidak ditemukan") },
            text = { Text("Prospek sudah dihapus atau tidak ada.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("TUTUP") } }
        )
        return
    }

    if (showUpdate) {
        ProspekFuUpdateDialog(
            prospek = p,
            onCancel = { showUpdate = false },
            onSaved = { showUpdate = false; onDismiss() }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Jamaah Wajib difollow Up Sekarang",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        text = {
            Column {
                Text(p.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("${p.kota} • ${p.no_wa}", fontSize = 12.sp, color = AdminTextGray)
                Text("Umrah: ${p.umrahUntukEnum().label}", fontSize = 12.sp, color = AdminTextGray)
                if (p.catatan.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Catatan: ${p.catatan}", fontSize = 12.sp, color = AdminText)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.waLink())))
                } catch (_: Exception) {}
            }) { Text("BUKA WA", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = { showUpdate = true }) { Text("UPDATE STATUS FU") }
        }
    )
}

@Composable
private fun ProspekFuUpdateDialog(
    prospek: Prospek,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sudahFu by remember { mutableStateOf(true) }
    var catatan by remember { mutableStateOf(prospek.fu_notes ?: "") }
    var saving by remember { mutableStateOf(false) }
    var errMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Update Status Follow Up", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = sudahFu, onCheckedChange = { sudahFu = it })
                    Text("Sudah di FU")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    label = { Text("Catatan FU") },
                    minLines = 2, maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                errMsg?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = Color(0xFFDC2626), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    scope.launch {
                        saving = true
                        errMsg = null
                        val r = withContext(Dispatchers.IO) {
                            ProspekRepository.updateStatusFu(prospek.id, catatan.trim(), sudahFu)
                        }
                        saving = false
                        r.onSuccess {
                            ProspekReminderScheduler.cancel(context, prospek.id)
                            onSaved()
                        }.onFailure {
                            errMsg = "Gagal: ${it.message}"
                        }
                    }
                }
            ) { Text(if (saving) "MENYIMPAN..." else "SIMPAN") }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !saving) { Text("BATAL") }
        }
    )
}
