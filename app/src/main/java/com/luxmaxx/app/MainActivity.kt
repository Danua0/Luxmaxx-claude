package com.luxmaxx.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.luxmaxx.app.ui.components.ScreenBackground
import com.luxmaxx.app.ui.screens.CoachScreen
import com.luxmaxx.app.ui.screens.FaceScreen
import com.luxmaxx.app.ui.screens.HomeScreen
import com.luxmaxx.app.ui.screens.NutritionScreen
import com.luxmaxx.app.ui.screens.ProfileScreen
import com.luxmaxx.app.ui.screens.WorkoutScreen
import com.luxmaxx.app.ui.theme.Gold
import com.luxmaxx.app.ui.theme.LuxTheme
import com.luxmaxx.app.ui.theme.Surface1
import com.luxmaxx.app.ui.theme.TextLo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent { LuxTheme { App() } }
    }
}

private data class Tab(val route: String, val title: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Главная", Icons.Filled.Home),
    Tab("workout", "Зал", Icons.Filled.FitnessCenter),
    Tab("food", "Питание", Icons.Filled.Restaurant),
    Tab("face", "Лицо", Icons.Filled.Face),
    Tab("coach", "AI", Icons.Filled.SmartToy),
)

@Composable
fun App(vm: AppViewModel = viewModel()) {
    val data by vm.data.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(msg) {
        msg?.let { snackbar.showSnackbar(it); vm.message.value = null }
    }

    // Первый запуск — сначала анкета
    if (data.profile == null) {
        ScreenBackground {
            Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = Color.Transparent) { pad ->
                ProfileScreen(vm, Modifier.padding(pad), firstRun = true, onDone = {})
            }
        }
        return
    }

    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    fun go(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    ScreenBackground {
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (current != "profile") NavigationBar(containerColor = Surface1.copy(alpha = 0.97f), tonalElevation = 0.dp) {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = current == t.route,
                        onClick = { go(t.route) },
                        icon = { Icon(t.icon, contentDescription = t.title) },
                        label = { Text(t.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1C1200),
                            selectedTextColor = Gold,
                            indicatorColor = Gold,
                            unselectedIconColor = TextLo,
                            unselectedTextColor = TextLo,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad).consumeWindowInsets(pad)) {
            composable("home") { HomeScreen(vm, onProfile = { nav.navigate("profile") }, onNavigate = ::go) }
            composable("workout") { WorkoutScreen(vm) }
            composable("food") { NutritionScreen(vm) }
            composable("face") { FaceScreen(vm) }
            composable("coach") { CoachScreen(vm, onProfile = { nav.navigate("profile") }) }
            composable("profile") { ProfileScreen(vm, firstRun = false, onDone = { nav.popBackStack() }) }
        }
    }
    }
}
