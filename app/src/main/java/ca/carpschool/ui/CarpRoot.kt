package ca.carpschool.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import ca.carpschool.data.*
import ca.carpschool.ui.components.*
import ca.carpschool.ui.screens.*
import com.clerk.api.Clerk
import kotlinx.coroutines.launch

@Composable
fun CarpRoot() {
    val inited by Clerk.isInitialized.collectAsState()
    val user by Clerk.userFlow.collectAsState()
    val code by Carp.schoolCode.collectAsState()
    val schools by Carp.schools.collectAsState()
    val me by Carp.me.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(user?.id) { if (user == null) Carp.signedOut() else { Carp.loadSchools(); if (Carp.school != null) Carp.refreshMe() } }
    val school = (schools as? Load.Ok)?.value?.find { it.schoolCode == code }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            !inited -> Splash()
            user == null -> LandingScreen()
            code == null || (schools is Load.Ok && school == null) -> SchoolScreen(onSignOut = { scope.launch { Clerk.auth.signOut() } })
            school == null -> if (schools is Load.Err) Center { ErrorBox((schools as Load.Err).message) { scope.launch { Carp.loadSchools() } } } else Splash()
            else -> when (val m = me) {
                is Load.Loading -> Splash()
                is Load.Err -> Center { Column { ErrorBox(m.message) { scope.launch { Carp.refreshMe() } }; TextButton(onClick = { scope.launch { Carp.chooseSchool(null) } }) { Text("Change school") } } }
                is Load.Ok -> {
                    val u = m.value
                    when {
                        u == null || !u.verified -> VerifyScreen(school) { scope.launch { Carp.chooseSchool(null) } }
                        u.role == null -> RoleScreen()
                        else -> AppShell(u)
                    }
                }
            }
        }
    }
}

@Composable fun Splash() = Center { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
@Composable fun Center(content: @Composable () -> Unit) = Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }

private data class Tab(val route: String, val label: String, val icon: ImageVector, val sel: ImageVector)
private val tabs = listOf(
    Tab("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
    Tab("rides", "Rides", Icons.Outlined.DirectionsCar, Icons.Filled.DirectionsCar),
    Tab("chats", "Chats", Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
    Tab("settings", "You", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle),
)

@Composable
fun AppShell(me: Me) {
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val toast: (String) -> Unit = { m -> scope.launch { snack.currentSnackbarData?.dismiss(); snack.showSnackbar(m) } }
    CompositionLocalProvider(LocalToast provides toast) {
        Scaffold(
            snackbarHost = { SnackbarHost(snack) },
            bottomBar = {
                AnimatedVisibility(tabs.any { it.route == route }, enter = fadeIn(), exit = fadeOut()) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
                        tabs.forEach { t ->
                            val s = route == t.route
                            NavigationBarItem(selected = s, label = { Text(t.label) }, icon = { Icon(if (s) t.sel else t.icon, null) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.secondaryContainer, selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary), onClick = {
                                nav.navigate(t.route) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
                            })
                        }
                    }
                }
            },
        ) { pad ->
            NavHost(nav, "home", Modifier.padding(bottom = pad.calculateBottomPadding()),
                enterTransition = { fadeIn(tween(160)) },
                exitTransition = { fadeOut(tween(120)) },
                popEnterTransition = { fadeIn(tween(180)) },
                popExitTransition = { fadeOut(tween(120)) }) {
                composable("home") { HomeScreen(me, nav) }
                composable("rides") { RidesScreen(me, nav) }
                composable("chats") { ChatsScreen(me, nav) }
                composable("settings") { SettingsScreen(me, nav) }
                composable("homes") { HomesScreen(nav) }
                composable("new") { NewCommuteScreen(me, nav) }
                composable("drive/{id}", listOf(navArgument("id") { type = NavType.StringType })) { DriveScreen(it.arguments!!.getString("id")!!, nav) }
                composable("request/{id}", listOf(navArgument("id") { type = NavType.StringType })) { RequestScreen(it.arguments!!.getString("id")!!, nav) }
                composable("chat/{id}", listOf(navArgument("id") { type = NavType.StringType })) { ChatScreen(it.arguments!!.getString("id")!!, me, nav) }
                composable("ride/{id}", listOf(navArgument("id") { type = NavType.StringType })) { RideScreen(it.arguments!!.getString("id")!!, me, nav) }
            }
        }
    }
}
