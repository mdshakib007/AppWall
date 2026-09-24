package io.github.mdshakib007.appwall.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.mdshakib007.appwall.ui.add.AddScreen
import io.github.mdshakib007.appwall.ui.detail.DetailScreen
import io.github.mdshakib007.appwall.ui.focus.FocusScreen
import io.github.mdshakib007.appwall.ui.home.HomeScreen
import io.github.mdshakib007.appwall.ui.insights.InsightsScreen
import io.github.mdshakib007.appwall.ui.onboarding.OnboardingScreen
import io.github.mdshakib007.appwall.ui.settings.AboutScreen
import io.github.mdshakib007.appwall.ui.settings.PrivacyScreen
import io.github.mdshakib007.appwall.ui.settings.SettingsScreen

/** Material "emphasized decelerate" curve: fast start, soft landing. */
val Emphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val FOCUS = "focus"
    const val INSIGHTS = "insights"
    const val ADD = "add?tab={tab}"
    const val DETAIL = "detail/{id}"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val PRIVACY = "privacy"
    fun add(tab: Int = 0) = "add?tab=$tab"
    fun detail(id: Long) = "detail/$id"
}

private class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Blocks", Icons.Outlined.Shield, Icons.Rounded.Shield),
    Tab(Routes.FOCUS, "Focus", Icons.Outlined.TrackChanges, Icons.Rounded.TrackChanges),
    Tab(Routes.INSIGHTS, "Insights", Icons.Outlined.Insights, Icons.Rounded.Insights),
)

@Composable
fun AppRoot(onboardingDone: Boolean) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = currentRoute in tabs.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (showBar) BottomBar(nav, currentRoute) },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            val tabRoutes = tabs.map { it.route }
            NavHost(
                navController = nav,
                startDestination = if (onboardingDone) Routes.HOME else Routes.ONBOARDING,
                // Tab ↔ tab: quiet fade-through. Push: new screen slides in from the right while the old one
                // parallaxes left. Pop: the reverse. Onboarding → home: slide up.
                enterTransition = {
                    when {
                        initialState.destination.route == Routes.ONBOARDING -> slideInVertically(tween(420, easing = Emphasized)) { it / 3 } + fadeIn(tween(300))
                        targetState.destination.route in tabRoutes && initialState.destination.route in tabRoutes -> fadeIn(tween(220)) + scaleIn(tween(260, easing = Emphasized), initialScale = 0.97f)
                        else -> slideInHorizontally(tween(360, easing = Emphasized)) { it } + fadeIn(tween(220))
                    }
                },
                exitTransition = {
                    when {
                        targetState.destination.route in tabRoutes && initialState.destination.route in tabRoutes -> fadeOut(tween(160))
                        else -> slideOutHorizontally(tween(360, easing = Emphasized)) { -it / 4 } + fadeOut(tween(260))
                    }
                },
                popEnterTransition = { slideInHorizontally(tween(340, easing = Emphasized)) { -it / 4 } + fadeIn(tween(240)) },
                popExitTransition = { slideOutHorizontally(tween(340, easing = Emphasized)) { it } + fadeOut(tween(220)) },
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(onDone = {
                        nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    })
                }
                composable(Routes.HOME) {
                    HomeScreen(
                        bottomPadding = padding.calculateBottomPadding(),
                        onAdd = { nav.navigate(Routes.add(it)) },
                        onOpenItem = { nav.navigate(Routes.detail(it)) },
                        onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                        onOpenFocus = { nav.navigate(Routes.FOCUS) },
                    )
                }
                composable(Routes.FOCUS) {
                    FocusScreen(bottomPadding = padding.calculateBottomPadding(), onAdd = { nav.navigate(Routes.add(0)) })
                }
                composable(Routes.INSIGHTS) {
                    InsightsScreen(bottomPadding = padding.calculateBottomPadding())
                }
                composable(Routes.ADD, arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })) {
                    AddScreen(initialTab = it.arguments?.getInt("tab") ?: 0, onBack = { nav.popBackStack() })
                }
                composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                    DetailScreen(id = it.arguments?.getLong("id") ?: -1L, onBack = { nav.popBackStack() })
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onBack = { nav.popBackStack() },
                        onAbout = { nav.navigate(Routes.ABOUT) },
                        onPrivacy = { nav.navigate(Routes.PRIVACY) },
                        onOnboarding = { nav.navigate(Routes.ONBOARDING) },
                    )
                }
                composable(Routes.ABOUT) { AboutScreen(onBack = { nav.popBackStack() }, onPrivacy = { nav.navigate(Routes.PRIVACY) }) }
                composable(Routes.PRIVACY) { PrivacyScreen(onBack = { nav.popBackStack() }) }
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, currentRoute: String?) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
        }
    }
}

private val Int.dp get() = androidx.compose.ui.unit.Dp(this.toFloat())
