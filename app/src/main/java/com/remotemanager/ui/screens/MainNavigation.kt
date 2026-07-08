package com.remotemanager.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    data object ServerList : Screen("servers")
    data object ServerDetail : Screen("servers/{serverId}") {
        fun createRoute(serverId: Long) = "servers/$serverId"
    }

    data object ServerEdit : Screen("servers/{serverId}/edit") {
        fun createRoute(serverId: Long) = "servers/$serverId/edit"
    }

    data object SshTerminal : Screen("ssh/{serverId}") {
        fun createRoute(serverId: Long) = "ssh/$serverId"
    }

    data object SftpBrowser : Screen("sftp/{serverId}") {
        fun createRoute(serverId: Long) = "sftp/$serverId"
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainNavigation() {
    val windowSizeClass = calculateWindowSizeClass(activity = androidx.compose.ui.platform.LocalContext.current as androidx.activity.ComponentActivity)
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded ||
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium

    val navController = rememberNavController()
    var selectedServerId by rememberSaveable { mutableStateOf(0L) }

    if (isExpanded) {
        TwoPaneLayout(
            selectedServerId = selectedServerId,
            onServerSelected = { selectedServerId = it },
            navController = navController
        )
    } else {
        SinglePaneLayout(navController = navController)
    }
}

@Composable
private fun SinglePaneLayout(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.ServerList.route) {
        composable(Screen.ServerList.route) {
            ServerListScreen(
                selectedServerId = 0L,
                onServerClick = { serverId ->
                    navController.navigate(Screen.ServerDetail.createRoute(serverId))
                },
                onAddServer = {
                    navController.navigate(Screen.ServerEdit.createRoute(0))
                }
            )
        }
        composable(
            route = Screen.ServerDetail.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: 0L
            ServerDetailScreen(
                serverId = serverId,
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { navController.navigate(Screen.ServerEdit.createRoute(serverId)) },
                onSshClick = { navController.navigate(Screen.SshTerminal.createRoute(serverId)) },
                onSftpClick = { navController.navigate(Screen.SftpBrowser.createRoute(serverId)) }
            )
        }
        composable(
            route = Screen.ServerEdit.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: 0L
            ServerEditScreen(
                serverId = serverId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.SshTerminal.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: 0L
            SshTerminalScreen(
                serverId = serverId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.SftpBrowser.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: 0L
            SftpBrowserScreen(
                serverId = serverId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun TwoPaneLayout(
    selectedServerId: Long,
    onServerSelected: (Long) -> Unit,
    navController: NavHostController
) {
    var currentPane by rememberSaveable { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 400.dp)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.surface
        ) {
            ServerListScreen(
                selectedServerId = selectedServerId,
                onServerClick = { serverId ->
                    onServerSelected(serverId)
                    currentPane = "detail"
                },
                onAddServer = {
                    onServerSelected(0)
                    currentPane = "edit"
                }
            )
        }

        Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentPane) {
                "edit" -> ServerEditScreen(
                    serverId = selectedServerId,
                    onNavigateBack = {
                        currentPane = if (selectedServerId != 0L) "detail" else ""
                    }
                )
                "ssh" -> SshTerminalScreen(
                    serverId = selectedServerId,
                    onNavigateBack = { currentPane = "detail" }
                )
                "sftp" -> SftpBrowserScreen(
                    serverId = selectedServerId,
                    onNavigateBack = { currentPane = "detail" }
                )
                else -> {
                    if (selectedServerId != 0L) {
                        ServerDetailScreen(
                            serverId = selectedServerId,
                            onNavigateBack = { onServerSelected(0) },
                            onEditClick = { currentPane = "edit" },
                            onSshClick = { currentPane = "ssh" },
                            onSftpClick = { currentPane = "sftp" }
                        )
                    } else {
                        EmptyDetailPane()
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDetailPane() {
    Surface(modifier = Modifier.fillMaxSize()) {
        // Intentionally empty; could show a placeholder illustration.
    }
}
