package com.example.expensetracker

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, dao: TransactionDao) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- DATA ---
    val allTransactions by dao.getAllTransactions().collectAsState(initial = emptyList())
    val accounts by dao.getAllAccounts().collectAsState(initial = emptyList())
    val categories by dao.getAllCategories().collectAsState(initial = emptyList())
    val budgetObj by dao.getBudget().collectAsState(initial = null)

    // --- UI STATES ---
    var searchQuery by remember { mutableStateOf("") }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var newBudgetAmount by remember { mutableStateOf("") }

    // --- FILTER STATES ---
    // Modes: "ALL", "MONTH", "RANGE"
    var filterMode by remember { mutableStateOf("ALL") }

    // Month Mode State
    var currentMonthOffset by remember { mutableIntStateOf(0) }

    // Range Mode State
    var startDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var endDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDateRangePicker by remember { mutableStateOf(false) } // To toggle dialogs logic

    // --- DATE LOGIC ---
    val calendar = Calendar.getInstance()

    // 1. Month Mode Logic
    calendar.timeInMillis = System.currentTimeMillis()
    calendar.add(Calendar.MONTH, currentMonthOffset)
    val viewMonth = calendar.get(Calendar.MONTH)
    val viewYear = calendar.get(Calendar.YEAR)
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

    // --- FILTERING LOGIC ---
    val filteredTransactions = allTransactions.filter { t ->
        val tCal = Calendar.getInstance()
        tCal.timeInMillis = t.date

        // Filter by Date Mode
        val dateMatch = when (filterMode) {
            "ALL" -> true
            "MONTH" -> tCal.get(Calendar.MONTH) == viewMonth && tCal.get(Calendar.YEAR) == viewYear
            "RANGE" -> {
                // Check if date is >= start (00:00) and <= end (23:59)
                val tDate = t.date
                // Reset start to 00:00
                val sCal = Calendar.getInstance().apply { timeInMillis = startDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }
                // Reset end to 23:59
                val eCal = Calendar.getInstance().apply { timeInMillis = endDate; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }
                tDate >= sCal.timeInMillis && tDate <= eCal.timeInMillis
            }
            else -> true
        }

        // Filter by Search
        val searchMatch = t.note.contains(searchQuery, ignoreCase = true) || t.category.contains(searchQuery, ignoreCase = true)

        dateMatch && searchMatch
    }

    // --- TOTAL CALCULATION ---
    // 1. Total Wallet Balance (Always shows real money available)
    var totalBalance = 0.0
    allTransactions.forEach { t ->
        val account = accounts.find { it.name == t.account }
        if (account == null || account.includeInTotal) {
            if (t.type == "INCOME") totalBalance += t.amount else totalBalance -= t.amount
        }
    }

    // 2. Visible Stats (Updates based on filters!)
    var viewIncome = 0.0
    var viewExpense = 0.0
    filteredTransactions.forEach { t ->
        if (t.type == "INCOME") viewIncome += t.amount else viewExpense += t.amount
    }

    val budgetLimit = budgetObj?.amount ?: 0.0
    val budgetProgress = if (budgetLimit > 0) (viewExpense / budgetLimit).toFloat() else 0f
    val budgetColor = if (budgetProgress > 1f) Color.Red else MaterialTheme.colorScheme.primary

    // --- DATE PICKER DIALOGS ---
    // Only used when setting a custom range. First picks Start, then picks End.
    val startPicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val c = Calendar.getInstance()
            c.set(y, m, d)
            startDate = c.timeInMillis
            // Immediately show end picker
            val endPicker = DatePickerDialog(
                context,
                { _, y2, m2, d2 ->
                    val c2 = Calendar.getInstance()
                    c2.set(y2, m2, d2)
                    endDate = c2.timeInMillis
                    filterMode = "RANGE" // Activate range mode
                },
                y, m, d
            )
            endPicker.setTitle("Select End Date")
            endPicker.show()
        },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )
    startPicker.setTitle("Select Start Date")

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                CenterAlignedTopAppBar(title = { Text("Dashboard", fontWeight = FontWeight.Bold) })
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search transactions...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddEdit.createRoute(null)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, "Add") }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)
        ) {
            // --- BALANCE CARD ---
            Card(
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp)),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF42A5F5), Color(0xFF0D47A1)))
                    ).padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total Balance", color = Color.White.copy(alpha = 0.8f))
                            Text("₹ ${String.format("%.2f", totalBalance)}", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatRow(Icons.Default.TrendingUp, "Income", viewIncome, Color(0xFFC8E6C9))
                            StatRow(Icons.Default.TrendingDown, "Expense", viewExpense, Color(0xFFFFCDD2))
                        }
                    }
                }
            }

            // --- FILTER CONTROLS ---
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterMode == "ALL",
                    onClick = { filterMode = "ALL" },
                    label = { Text("All Time") },
                    leadingIcon = { if (filterMode == "ALL") Icon(Icons.Default.FilterList, null) }
                )
                FilterChip(
                    selected = filterMode == "MONTH",
                    onClick = { filterMode = "MONTH" },
                    label = { Text("Monthly") },
                    leadingIcon = { if (filterMode == "MONTH") Icon(Icons.Default.CalendarToday, null) }
                )
                FilterChip(
                    selected = filterMode == "RANGE",
                    onClick = { startPicker.show() }, // Open Picker
                    label = { Text("Custom Date") },
                    leadingIcon = { if (filterMode == "RANGE") Icon(Icons.Default.DateRange, null) }
                )
            }

            // --- DYNAMIC FILTER UI ---
            if (filterMode == "MONTH") {
                // Month Selector Arrows
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentMonthOffset-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
                    Text(monthName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { currentMonthOffset++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                }
            } else if (filterMode == "RANGE") {
                // Date Range Display
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { startPicker.show() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                    Text(
                        "${sdf.format(Date(startDate))} - ${sdf.format(Date(endDate))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp)) // Small spacer for All Time
            }

            // --- BUDGET BAR (Optional) ---
            // Only show budget if Monthly is selected OR All Time (optional choice)
            // For now, let's show it always, but it makes most sense in Month view.
            if (filterMode == "MONTH") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Budget", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showBudgetDialog = true }) {
                        Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(16.dp))
                    }
                }
                if (budgetLimit > 0.0) {
                    LinearProgressIndicator(
                        progress = budgetProgress.coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = budgetColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    if (budgetProgress > 1f) Text("Over Limit!", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // --- LIST ---
            LazyColumn(contentPadding = PaddingValues(bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredTransactions) { transaction ->
                    TransactionItem(transaction, categories) {
                        navController.navigate(Screen.AddEdit.createRoute(transaction.id))
                    }
                }
            }
        }

        // --- BUDGET DIALOG ---
        if (showBudgetDialog) {
            AlertDialog(
                onDismissRequest = { showBudgetDialog = false },
                title = { Text("Set Monthly Budget") },
                text = {
                    OutlinedTextField(
                        value = newBudgetAmount,
                        onValueChange = { newBudgetAmount = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val amount = newBudgetAmount.toDoubleOrNull() ?: 0.0
                        scope.launch {
                            dao.insertBudget(BudgetEntity(amount = amount))
                            showBudgetDialog = false
                        }
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showBudgetDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun StatRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, amount: Double, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
            Text("₹ ${String.format("%.0f", amount)}", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionEntity, categories: List<CategoryEntity>, onClick: () -> Unit) {
    val isExpense = transaction.type == "EXPENSE"
    val color = if (isExpense) Color(0xFFE53935) else Color(0xFF43A047)
    val sign = if (isExpense) "- " else "+ "

    val categoryColor = categories.find { it.name == transaction.category }?.color
    val iconBgColor = if (categoryColor != null) Color(categoryColor) else MaterialTheme.colorScheme.secondaryContainer
    val iconTint = if (categoryColor != null) Color.White else MaterialTheme.colorScheme.onSecondaryContainer

    ElevatedCard(
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = iconBgColor, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(IconUtils.getIconByName(transaction.iconName), null, modifier = Modifier.size(20.dp), tint = iconTint)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.category, fontWeight = FontWeight.Bold)
                if (transaction.note.isNotEmpty()) Text(transaction.note, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Text(SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(transaction.date)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$sign₹${String.format("%.2f", transaction.amount)}", color = color, fontWeight = FontWeight.Bold)
                Text(transaction.account, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}