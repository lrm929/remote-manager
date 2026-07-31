package com.remotemanager.ui.screens

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.remotemanager.ui.screens.crt.CrtHomeScreen
import com.remotemanager.ui.screens.rdp.RdpSessionScreen

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

    data object RdpSession : Screen("rdp/{serverId}") {
        fun createRoute(serverId: Long) = "rdp/$serverId"
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainNavigation() {
    val windowSizeClass = calculateWindowSizeClass(activity = androidx.compose.ui.platform.LocalContext.current as androidx.activity.ComponentActivity)
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded ||
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium

    val navController = rememberNavController()

    if (isExpanded) {
        CrtHomeScreen()
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
                onSftpClick = { navController.navigate(Screen.SftpBrowser.createRoute(serverId)) },
                onRdpLaunch = { navController.navigate(Screen.RdpSession.createRoute(serverId)) }
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
        composable(
            route = Screen.RdpSession.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: 0L
            RdpSessionScreen(
                serverId = serverId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
