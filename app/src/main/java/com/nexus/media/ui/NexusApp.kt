package com.nexus.media.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexus.media.core.permissions.MediaPermissions
import com.nexus.media.di.AppContainer
import com.nexus.media.features.*

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun NexusApp(c: AppContainer) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(MediaPermissions.hasMedia(ctx)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = MediaPermissions.hasMedia(ctx)
    }
    LaunchedEffect(granted) {
        if (granted) {
            c.repository.startObserving()
            c.repository.rescan()
            c.player.restore()
        }
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (!granted) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("NEXUS Media", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
                Text("Izinkan akses ke musik dan video di perangkat agar library bisa dipindai.")
                Spacer(Modifier.height(20.dp))
                Button(onClick = { launcher.launch(MediaPermissions.required()) }) { Text("Beri izin") }
            }
        } else {
            MainScaffold(c)
        }
    }
}

@Composable
private fun MainScaffold(c: AppContainer) {
    val nav = rememberNavController()
    var full by remember { mutableStateOf(false) }
    val vm: LibraryViewModel = viewModel(factory = viewModelFactory { initializer { LibraryViewModel(c) } })
    CompositionLocalProvider(LocalContainer provides c) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    Column {
                        MiniPlayer(c) { full = true }
                        BottomBar(nav)
                    }
                }
            ) { pad ->
                NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
                    composable("home") { HomeScreen(vm, nav) }
                    composable("video") { VideoScreen(vm, nav) }
                    composable("music") { MusicScreen(vm, nav) }
                    composable("library") { LibraryScreen(vm, nav) }
                    composable("settings") { SettingsScreen(vm) }
                    composable("search") { SearchScreen(vm, nav) }
                    composable("folders") { FoldersScreen(vm, nav) }
                    composable("recent") { RecentScreen(vm, nav) }
                    composable("duplicates") { DuplicatesScreen(vm, nav) }
                    composable("playlist/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                        PlaylistScreen(vm, nav, it.arguments!!.getLong("id"))
                    }
                    composable("folder/{path}") { FolderScreen(vm, nav, dec(it.arguments!!.getString("path")!!)) }
                    composable("group/{kind}/{name}") {
                        GroupScreen(vm, nav, it.arguments!!.getString("kind")!!, dec(it.arguments!!.getString("name")!!))
                    }
                }
            }
            AnimatedVisibility(
                visible = full,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                BackHandler { full = false }
                FullPlayer(c) { full = false }
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController) {
    val tabs = listOf(
        NavTab("home", "Beranda", Icons.Rounded.Home),
        NavTab("video", "Video", Icons.Rounded.Videocam),
        NavTab("music", "Musik", Icons.Rounded.MusicNote),
        NavTab("library", "Pustaka", Icons.Rounded.VideoLibrary),
        NavTab("settings", "Setelan", Icons.Rounded.Settings)
    )
    val entry by nav.currentBackStackEntryAsState()
    val dest = entry?.destination
    NavigationBar {
        tabs.forEach { t ->
            NavigationBarItem(
                selected = dest?.hierarchy?.any { it.route == t.route } == true,
                onClick = {
                    nav.navigate(t.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(t.icon, null) },
                label = { Text(t.label, maxLines = 1) }
            )
        }
    }
}
