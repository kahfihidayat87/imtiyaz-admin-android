package com.imtiyaztour.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
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
fun TripListScreen(
    onTripClick: (Trip) -> Unit,
    onAddClick: () -> Unit
) {
    val trips by TripRepository.observeTrips()
        .collectAsState(initial = emptyList())

    Box(Modifier.fillMaxSize()) {
        if (trips.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null,
                    modifier = Modifier.size(64.dp), tint = AdminTextGray)
                Spacer(Modifier.height(12.dp))
                Text("Belum ada trip", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text("Tap tombol + untuk membuat trip baru",
                    fontSize = 12.sp, color = AdminTextGray)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(trips, key = { it.id }) { trip ->
                    TripCard(trip, onClick = { onTripClick(trip) })
                }
                item { Spacer(Modifier.height(80.dp)) }  // FAB space
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = AdminPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Trip", tint = Color.White)
        }
    }
}

@Composable
private fun TripCard(trip: Trip, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    val tglBerangkat = if (trip.tanggal_berangkat > 0) sdf.format(Date(trip.tanggal_berangkat)) else "-"

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(trip.nama.ifBlank { "(tanpa nama)" },
                    fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    modifier = Modifier.weight(1f))
                TripStatusBadge(trip.status)
            }
            Spacer(Modifier.height(6.dp))
            Text("Berangkat: $tglBerangkat", fontSize = 12.sp, color = AdminTextGray)
            Text("${trip.jumlah_jamaah} jamaah", fontSize = 12.sp, color = AdminTextGray)
        }
    }
}

@Composable
fun TripStatusBadge(status: String) {
    val (color, label) = when (status.lowercase()) {
        "planning" -> Color(0xFFFFA726) to "Planning"
        "berlangsung" -> Color(0xFF29B6F6) to "Berlangsung"
        "selesai" -> Color(0xFF66BB6A) to "Selesai"
        else -> Color.Gray to status
    }
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}
