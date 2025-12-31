package com.example.expensetracker

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.ui.graphics.vector.ImageVector

object IconUtils {
    // Map of Icon Name -> ImageVector
    val iconMap = mapOf(
        "Shopping" to Icons.Default.ShoppingCart,
        "Food" to Icons.Default.Restaurant,
        "Transport" to Icons.Default.DirectionsBus,
        "Rent" to Icons.Default.Home,
        "Salary" to Icons.Default.Work,
        "Health" to Icons.Default.LocalHospital,
        "Education" to Icons.Default.School,
        "Entertainment" to Icons.Default.Movie,
        "Sports" to Icons.Default.SportsEsports,
        "Other" to Icons.Default.Category,
        "Income" to Icons.Default.AttachMoney
    )

    fun getIconByName(name: String): ImageVector {
        return iconMap[name] ?: Icons.Default.Category
    }
}