package com.example.kitchenratio.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kitchenratio.ui.components.AppIcons
import com.example.kitchenratio.ui.screens.AboutScreen
import com.example.kitchenratio.ui.screens.CalculatorScreen
import com.example.kitchenratio.ui.screens.SavedRecipesScreen
import com.example.kitchenratio.ui.screens.SettingsScreen
import com.example.kitchenratio.ui.screens.SplashScreen
import com.example.kitchenratio.ui.theme.KitchenRatioTheme

object Routes {
    const val CALCULATOR = "calculator"
    const val SAVED = "saved"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

private data class TopLevelDestination(val route: String, val label: String, val icon: ImageVector)

private val Destinations = listOf(
    TopLevelDestination(Routes.CALCULATOR, "Calculator", AppIcons.Calculate),
    TopLevelDestination(Routes.SAVED, "Saved", AppIcons.Bookmark),
    TopLevelDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
    TopLevelDestination(Routes.ABOUT, "About", Icons.Filled.Info)
)

@Composable
fun KitchenRatioApp(viewModel: KitchenRatioViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showSplash by rememberSaveable { mutableStateOf(true) }

    KitchenRatioTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Crossfade(targetState = showSplash, label = "splash") { splash ->
                if (splash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    MainScaffold(viewModel)
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(viewModel: KitchenRatioViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recipes by viewModel.savedRecipes.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                Destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = { navigateTo(destination.route) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CALCULATOR,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
        ) {
            composable(Routes.CALCULATOR) {
                CalculatorScreen(viewModel, state, settings, snackbarHostState)
            }
            composable(Routes.SAVED) {
                SavedRecipesScreen(
                    viewModel = viewModel,
                    recipes = recipes,
                    snackbarHostState = snackbarHostState,
                    onOpen = { recipe ->
                        viewModel.loadRecipe(recipe)
                        navController.navigate(Routes.CALCULATOR) {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel, settings, snackbarHostState)
            }
            composable(Routes.ABOUT) {
                AboutScreen()
            }
        }
    }
}
