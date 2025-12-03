@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bookery.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bookery.R
import com.bookery.ui.library.LibraryScreen
import com.bookery.ui.vocabulary.VocabularyScreen
import com.bookery.HomeActivity

sealed class BottomDest(val route: String, val label: String) {
    object Home : BottomDest("home", "Home")
    object Library : BottomDest("library", "Biblioteca")
    object Vocabulary : BottomDest("vocabulary", "Dizionario")
}

private val bottomItems = listOf(
    BottomDest.Home,
    BottomDest.Library,
    BottomDest.Vocabulary
)

@Composable
fun HomeRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showSettingsDialog = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookery") },
                actions = {
                    IconButton(onClick = { showSettingsDialog.value = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = "Impostazioni"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                bottomItems.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = {
                            if (currentRoute != dest.route) {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        label = { Text(dest.label) },
                        icon = { }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomDest.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomDest.Home.route) { HomeScreen() }
            composable(BottomDest.Library.route) {
                val version by HomeActivity.progressVersion
                LibraryScreen(progressVersion = version)
            }
            composable(BottomDest.Vocabulary.route) { VocabularyScreen() }
        }

    }

    if (showSettingsDialog.value) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog.value = false },
            title = { Text("Impostazioni") },
            text = { Text("Qui potrai aggiungere le impostazioni dell'app.") },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog.value = false }) {
                    Text("Chiudi")
                }
            }
        )
    }
}
