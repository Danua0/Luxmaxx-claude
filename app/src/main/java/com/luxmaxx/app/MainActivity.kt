package com.luxmaxx.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.luxmaxx.app.ui.components.SplashScreen
import com.luxmaxx.app.ui.screens.AchievementsScreen
import com.luxmaxx.app.ui.screens.CelebrationOverlay
import com.luxmaxx.app.ui.screens.CoachScreen
import com.luxmaxx.app.ui.screens.HomeScreen
import com.luxmaxx.app.ui.screens.LooksScreen
import com.luxmaxx.app.ui.screens.NutritionScreen
import com.luxmaxx.app.ui.screens.ProfileScreen
import com.luxmaxx.app.ui.screens.WorkoutScreen
import com.luxmaxx.app.ui.theme.Gold
import com.luxmaxx.app.ui.theme.GoldDeep
import com.luxmaxx.app.ui.theme.LuxTheme
import com.luxmaxx.app.ui.theme.Stroke
import com.luxmaxx.app.ui.theme.Surface1
import com.luxmaxx.app.ui.theme.Surface2
import com.luxmaxx.app.ui.theme.TextHi
import com.luxmaxx.app.ui.theme.TextLo
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent { LuxTheme { Root() } }
    }
}

@Composable
private fun Root(vm: AppViewModel = viewModel()) {
    var splash by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1700); splash = false }
    val celebration by vm.celebration.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = splash,
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(400)) },
            label = "splash",
        ) { s -> if (s) SplashScreen() else App(vm) }

        AnimatedVisibility(
            visible = celebration != null,
            enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(tween(250)),
        ) {
            celebration?.let { CelebrationOverlay(it, onDismiss = { vm.dismissCelebration() }) }
        }
    }
}

private data class Tab(val route: String, val title: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Главная", Icons.Filled.Home),
    Tab("workout", "Зал", Icons.Filled.FitnessCenter),
    Tab("food", "Еда", Icons.Filled.Restaurant),
    Tab("face", "Образ", Icons.Filled.Face),
    Tab("coach", "AI", Icons.Filled.SmartToy),
)

@Composable
fun App(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(msg) {
        msg?.let { snackbar.showSnackbar(it); vm.message.value = null }
    }
    val snackHost: @Composable () -> Unit = {
        SnackbarHost(snackbar) { d ->
            Snackbar(d, containerColor = Surface2, contentColor = TextHi, shape = RoundedCornerShape(16.dp))
        }
    }

    // Первый запуск — сначала анкета
    if (data.profile == null) {
        ScreenBackground {
            Scaffold(snackbarHost = snackHost, containerColor = Color.Transparent) { pad ->
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
            snackbarHost = snackHost,
            bottomBar = {
                AnimatedVisibility(
                    visible = current in tabs.map { it.route },
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    FloatingNavBar(current) { go(it) }
                }
            },
        ) { pad ->
            NavHost(
                nav, startDestination = "home",
                modifier = Modifier.padding(pad).consumeWindowInsets(pad),
                enterTransition = { fadeIn(tween(260)) + slideInVertically(tween(320)) { it / 18 } },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(260)) },
                popExitTransition = { fadeOut(tween(180)) },
            ) {
                composable("home") {
                    HomeScreen(vm, onProfile = { nav.navigate("profile") }, onNavigate = { r ->
                        if (r == "achievements") nav.navigate(r) else go(r)
                    })
                }
                composable("workout") { WorkoutScreen(vm) }
                composable("food") { NutritionScreen(vm) }
                composable("face") { LooksScreen(vm) }
                composable("coach") { CoachScreen(vm, onProfile = { nav.navigate("profile") }) }
                composable("profile") {
                    ProfileScreen(vm, firstRun = false, onDone = { nav.popBackStack() },
                        onAchievements = { nav.navigate("achievements") })
                }
                composable("achievements") { AchievementsScreen(vm, onBack = { nav.popBackStack() }) }
            }
        }
    }
}

/** Плавающая навигация: активная вкладка раскрывается в золотую «таблетку» с подписью. */
@Composable
private fun FloatingNavBar(current: String?, onSelect: (String) -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .fillMaxWidth()
            .shadow(24.dp, shape, ambientColor = Gold.copy(alpha = 0.25f), spotColor = Gold.copy(alpha = 0.25f))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Surface2.copy(alpha = 0.97f), Surface1.copy(alpha = 0.98f))))
            .border(1.dp, Brush.linearGradient(listOf(Gold.copy(alpha = 0.35f), Stroke, Stroke)), shape)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { t ->
            val sel = current == t.route
            val weight by animateFloatAsState(if (sel) 2.1f else 1f, spring(dampingRatio = 0.7f, stiffness = 400f), label = "w")
            val iconColor by animateColorAsState(if (sel) Color(0xFF1C1200) else TextLo, label = "c")
            val bounce by animateFloatAsState(if (sel) 1.12f else 1f, spring(dampingRatio = 0.35f, stiffness = 500f), label = "b")
            Row(
                Modifier
                    .weight(weight)
                    .height(48.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        if (sel) Brush.linearGradient(listOf(Gold, GoldDeep))
                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(t.route) },
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(t.icon, t.title, tint = iconColor, modifier = Modifier.size(22.dp).graphicsLayer { scaleX = bounce; scaleY = bounce })
                AnimatedVisibility(sel, enter = expandHorizontally() + fadeIn(), exit = shrinkHorizontally() + fadeOut()) {
                    Row {
                        Spacer(Modifier.width(6.dp))
                        Text(t.title, style = MaterialTheme.typography.labelLarge, color = iconColor, maxLines = 1)
                    }
                }
            }
        }
    }
}
