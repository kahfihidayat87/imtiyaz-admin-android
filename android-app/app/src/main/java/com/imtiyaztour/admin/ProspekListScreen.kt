package com.imtiyaztour.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProspekListScreen(
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onProspekClick: (Prospek) -> Unit = {},
    onEditClick: (Prospek) -> Unit = {},
    onDeleteClick: (Prospek) -> Unit = {}
) {
    val all by ProspekRepository.observeAll().collectAsState(initial = emptyList())
    val now = System.currentTimeMillis()
    val due = all.filter { it.statusEnum() == ProspekStatus.FU_DIJADWALKAN && (it.fu_date ?: Long.MAX_VALUE) <= now }
    val rest = all.filterNot { p -> due.any { it.id == p.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Prospek") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminPrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
        if (all.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Call, contentDescription = null,
                    modifier = Modifier.size(64.dp), tint = AdminTextGray)
                Spacer(Modifier.height(12.dp))
                Text("Belum ada prospek", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text("Tap tombol + untuk menambah prospek",
                    fontSize = 12.sp, color = AdminTextGray)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (due.isNotEmpty()) {
                    item { SectionLabel("Jatuh Tempo Follow Up", Color(0xFFDC2626)) }
                    items(due, key = { "d-${it.id}" }) { ProspekCard(it, true, { onProspekClick(it) }, { onEditClick(it) }, { onDeleteClick(it) }) }
                }
                if (rest.isNotEmpty()) {
                    item { SectionLabel("Semua Prospek", AdminText) }
                    items(rest, key = { "a-${it.id}" }) { ProspekCard(it, false, { onProspekClick(it) }, { onEditClick(it) }, { onDeleteClick(it) }) }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = AdminPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Prospek", tint = Color.White)
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color,
        modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun ProspekCard(p: Prospek, highlight: Boolean, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val (bc, bt) = when (p.statusEnum()) {
        ProspekStatus.BARU -> AdminTextGray to "Baru"
        ProspekStatus.FU_DIJADWALKAN -> AdminWarning to "FU ${formatTgl(p.fu_date)}"
        ProspekStatus.SUDAH_FU -> AdminSuccess to "Sudah FU"
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) Color(0xFFFEF2F2) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.nama, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = AdminText, modifier = Modifier.weight(1f))
                Box(
                    Modifier.background(bc.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(bt, fontSize = 11.sp, color = bc, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("${p.kota} • ${p.no_wa}", fontSize = 12.sp, color = AdminTextGray)
            Text("Umrah: ${p.umrahUntukEnum().label}", fontSize = 12.sp, color = AdminTextGray)
            if (p.catatan.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(p.catatan, fontSize = 12.sp, color = AdminText, maxLines = 2)
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Prospek", tint = AdminPrimary)
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus Prospek", tint = Color(0xFFDC2626))
                }
            }
        }
    }
}

private fun formatTgl(ms: Long?): String {
    if (ms == null || ms <= 0L) return "-"
    return SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(ms))
}

    }
}