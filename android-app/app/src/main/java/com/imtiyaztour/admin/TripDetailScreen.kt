package com.imtiyaztour.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Hotel
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
fun TripDetailScreen(
    trip: Trip,
    onBack: () -> Unit,
    onHotelClick: (Hotel) -> Unit,
    onAddHotel: () -> Unit
) {
    val hotels by TripRepository.observeHotels(trip.id)
        .collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(trip.nama, fontWeight = FontWeight.Bold, color = AdminPrimary,
                            maxLines = 1)
                        Text("${trip.jumlah_jamaah} jamaah · ${trip.status}",
                            fontSize = 10.sp, color = AdminTextGray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AdminPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (hotels.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Hotel, contentDescription = null,
                        modifier = Modifier.size(64.dp), tint = AdminTextGray)
                    Spacer(Modifier.height(12.dp))
                    Text("Belum ada hotel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Tap tombol + untuk tambah hotel",
                        fontSize = 12.sp, color = AdminTextGray)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(hotels, key = { it.id }) { h ->
                        HotelCard(h, onClick = { onHotelClick(h) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            FloatingActionButton(
                onClick = onAddHotel,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                containerColor = AdminPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Hotel", tint = Color.White)
            }
        }
    }
}

@Composable
private fun HotelCard(h: Hotel, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM", Locale("id", "ID"))
    val checkin = if (h.checkin > 0) sdf.format(Date(h.checkin)) else "-"
    val checkout = if (h.checkout > 0) sdf.format(Date(h.checkout)) else "-"

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = if (h.tipe == "makkah") Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(h.tipe.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = if (h.tipe == "makkah") Color(0xFF0F7A5A) else Color(0xFF1565C0),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(h.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("$checkin — $checkout · ${h.jumlah_lantai} lantai",
                    fontSize = 11.sp, color = AdminTextGray)
            }
        }
    }
}
