package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = ExpenseDatabase.getDatabase(this)
        val dao = database.transactionDao()

        setContent {
            ExpenseTrackerTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        val showBottomBar = currentRoute in listOf(Screen.Home.route, Screen.Stats.route, Screen.Accounts.route)
                        if (showBottomBar) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home") },
                                    selected = currentRoute == Screen.Home.route,
                                    onClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Wallet, contentDescription = "Accounts") },
                                    label = { Text("Accounts") },
                                    selected = currentRoute == Screen.Accounts.route,
                                    onClick = { navController.navigate(Screen.Accounts.route) { popUpTo(Screen.Home.route) } }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Stats") },
                                    label = { Text("Stats") },
                                    selected = currentRoute == Screen.Stats.route,
                                    onClick = { navController.navigate(Screen.Stats.route) { popUpTo(Screen.Home.route) } }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) { HomeScreen(navController, dao) }
                        composable(
                            route = Screen.AddEdit.route,
                            arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = -1 })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getInt("id") ?: -1
                            AddEditScreen(navController, dao, id)
                        }
                        composable(Screen.Stats.route) { StatsScreen(dao) }
                        composable(Screen.Accounts.route) { AccountScreen(navController, dao) }
                    }
                }
            }
        }
    }
}