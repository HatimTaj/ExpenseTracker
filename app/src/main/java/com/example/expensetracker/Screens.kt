package com.example.expensetracker

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddEdit : Screen("add_edit?id={id}") {
        fun createRoute(id: Int?) = "add_edit?id=${id ?: -1}"
    }
    object Stats : Screen("stats")
    object Accounts : Screen("accounts") // NEW
}