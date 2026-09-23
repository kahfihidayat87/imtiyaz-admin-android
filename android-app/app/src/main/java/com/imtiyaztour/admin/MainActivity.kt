package com.imtiyaztour.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_Material_Light_NoActionBar)
        setContent { AdminApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApp() {
    val context = LocalContext.current
    var isLoggedIn by remember { mutableStateOf(Prefs.isLoggedIn(context)) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingJamaah by remember { mutableStateOf<JamaahSummary?>(null) }

    if (!isLoggedIn) {
        LoginScreen(onLoggedIn = { isLoggedIn = true })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Imtiyaz Admin", fontWeight = FontWeight.Bold, color = AdminPrimary)
                        Text("Panel Manajemen", fontSize = 10.sp, color = AdminTextGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("Jamaah", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                    label = { Text("Info", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    label = { Text("Saya", fontSize = 10.sp) }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> DashboardScreen()
                1 -> {
                    if (editingJamaah != null) {
                        EditJamaahScreen(
                            jamaah = editingJamaah!!,
                            onBack = { editingJamaah = null },
                            onSaved = { editingJamaah = null }
                        )
                    } else {
                        JamaahListScreen(onJamaahClick = { editingJamaah = it })
                    }
                }
                2 -> PengumumanScreen()
                3 -> SayaScreen(onLoggedOut = {
                    isLoggedIn = false
                    selectedTab = 0
                })
            }
        }
    }
}
