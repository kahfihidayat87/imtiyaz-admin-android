package com.imtiyaztour.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashflowScreen(
    onAddClick: () -> Unit,
    onItemClick: (Cashflow) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Operasional, 1: Jamaah
    val akunAktif = if (selectedTab == 0) CashflowAkun.KAS_OPERASIONAL.name else CashflowAkun.KAS_JAMAAH.name

    val listKas by CashflowRepository.observeCashflow(akunAktif).collectAsState(initial = emptyList())

    val totalMasuk = listKas.filter { it.tipeEnum() == CashflowTipe.MASUK }.sumOf { it.nominal }
    val totalKeluar = listKas.filter { it.tipeEnum() == CashflowTipe.KELUAR }.sumOf { it.nominal }
    val saldo = totalMasuk - totalKeluar

    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFF9FAFB))) {
        Column(Modifier.fillMaxSize()) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = AdminPrimary,
                contentColor = Color.White
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Kas Operasional") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Kas Jamaah") })
            }

            // Kartu Saldo
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Total Saldo", fontSize = 12.sp, color = AdminTextGray)
                    Text(formatter.format(saldo), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (saldo >= 0) AdminSuccess else Color(0xFFDC2626))
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Masuk", fontSize = 11.sp, color = AdminTextGray)
                            Text(formatter.format(totalMasuk), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AdminSuccess)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Keluar", fontSize = 11.sp, color = AdminTextGray)
                            Text(formatter.format(totalKeluar), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                        }
                    }
                }
            }

            // List Transaksi
            Text(
                "Riwayat Transaksi",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (listKas.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada transaksi", color = AdminTextGray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listKas, key = { it.id }) { item ->
                        CashflowItemCard(item, formatter) { onItemClick(item) }
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = AdminPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Catat Transaksi", tint = Color.White)
        }
    }
}

@Composable
private fun CashflowItemCard(item: Cashflow, formatter: NumberFormat, onClick: () -> Unit) {
    val isMasuk = item.tipeEnum() == CashflowTipe.MASUK
    val colorNominal = if (isMasuk) AdminSuccess else Color(0xFFDC2626)
    val icon = if (isMasuk) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
    val tglFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(item.tanggal))

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).background(colorNominal.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colorNominal)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.kategori, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AdminText)
                Text(item.keterangan.ifBlank { "-" }, fontSize = 12.sp, color = AdminTextGray, maxLines = 1)
                Text(tglFormat, fontSize = 10.sp, color = AdminTextGray)
            }
            Text(
                (if (isMasuk) "+" else "-") + formatter.format(item.nominal),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = colorNominal
            )
        }
    }
}
