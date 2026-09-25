package com.imtiyaztour.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

data class AdminTab(val key: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApp() {
    val context = LocalContext.current
    var isLoggedIn by remember { mutableStateOf(Prefs.isLoggedIn(context)) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingJamaah by remember { mutableStateOf<JamaahSummary?>(null) }
    var showAddJamaah by remember { mutableStateOf(false) }

    if (!isLoggedIn) {
        LoginScreen(onLoggedIn = { isLoggedIn = true; selectedTab = 0 })
        return
    }

    // Role: "admin" | "keuangan" | "tl"
    val role = Prefs.getRole(context).lowercase()

    // Tab berbeda per role (RBAC)
    val tabs: List<AdminTab> = when (role) {
        "super_admin" -> listOf(
            AdminTab("dashboard", "Dashboard", Icons.Default.Dashboard),
            AdminTab("jamaah", "Jamaah", Icons.Default.People),
            AdminTab("bukti", "Bukti", Icons.Default.Payments),
            AdminTab("kanal", "Kanal", Icons.Default.Radio),
            AdminTab("info", "Info", Icons.Default.Campaign),
            AdminTab("admin", "Admin", Icons.Default.Person),
            AdminTab("saya", "Saya", Icons.Default.AccountCircle)
        )
        "keuangan" -> listOf(
            AdminTab("dashboard", "Dashboard", Icons.Default.Dashboard),
            AdminTab("jamaah", "Jamaah", Icons.Default.People),
            AdminTab("bukti", "Bukti", Icons.Default.Payments),
            AdminTab("saya", "Saya", Icons.Default.AccountCircle)
        )
        "tl" -> listOf(
            AdminTab("dashboard", "Dashboard", Icons.Default.Dashboard),
            AdminTab("kanal", "Kanal", Icons.Default.Radio),
            AdminTab("info", "Info", Icons.Default.Campaign),
            AdminTab("saya", "Saya", Icons.Default.AccountCircle)
        )
        else -> listOf( // admin (default)
            AdminTab("dashboard", "Dashboard", Icons.Default.Dashboard),
            AdminTab("jamaah", "Jamaah", Icons.Default.People),
            AdminTab("bukti", "Bukti", Icons.Default.Payments),
            AdminTab("kanal", "Kanal", Icons.Default.Radio),
            AdminTab("info", "Info", Icons.Default.Campaign),
            AdminTab("saya", "Saya", Icons.Default.AccountCircle)
        )
    }

    if (selectedTab >= tabs.size) selectedTab = 0
    val currentTabKey = tabs.getOrNull(selectedTab)?.key ?: "dashboard"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Imtiyaz Admin", fontWeight = FontWeight.Bold, color = AdminPrimary)
                        Text("Panel Manajemen - " + role.uppercase(), fontSize = 10.sp, color = AdminTextGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (currentTabKey) {
                "dashboard" -> DashboardScreen()
                "jamaah" -> {
                    when {
                        showAddJamaah -> AddJamaahScreen(
                            onBack = { showAddJamaah = false },
                            onSuccess = { showAddJamaah = false }
                        )
                        editingJamaah != null -> EditJamaahScreen(
                            jamaah = editingJamaah!!,
                            onBack = { editingJamaah = null },
                            onSaved = { editingJamaah = null },
                            canDelete = (role == "super_admin"),
                            onDeleted = { editingJamaah = null }
                        )
                        else -> JamaahListScreen(
                            onJamaahClick = { editingJamaah = it },
                            canAdd = (role == "admin" || role == "super_admin"),
                            onAddClick = { showAddJamaah = true }
                        )
                    }
                }
                "bukti" -> BuktiListScreen()
                "kanal" -> KanalListScreen()
                "info" -> PengumumanScreen()
                "admin" -> AdminListScreen()
                "saya" -> SayaScreen(onLoggedOut = {
                    isLoggedIn = false
                    selectedTab = 0
                    editingJamaah = null
                })
            }
        }
    }
}
