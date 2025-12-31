package com.example.expensetracker

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    navController: NavController,
    dao: TransactionDao,
    transactionId: Int
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Load Data
    val accounts by dao.getAllAccounts().collectAsState(initial = emptyList())
    val categories by dao.getAllCategories().collectAsState(initial = emptyList())

    // States
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var selectedIcon by remember { mutableStateOf("Food") }
    var selectedAccount by remember { mutableStateOf("Cash") }

    // Date State
    var transactionDate by remember { mutableStateOf(System.currentTimeMillis()) }

    // Helper to pick date
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = transactionDate
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            calendar.set(year, month, dayOfMonth)
            transactionDate = calendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // New Category Dialog States
    var showCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("Food") }

    // Load existing transaction if editing
    val transactionToEdit = if (transactionId != -1) {
        dao.getTransactionById(transactionId).collectAsState(initial = null).value
    } else null

    LaunchedEffect(transactionToEdit) {
        if (transactionToEdit != null) {
            amount = transactionToEdit.amount.toString()
            note = transactionToEdit.note
            type = transactionToEdit.type
            selectedCategory = transactionToEdit.category
            selectedIcon = transactionToEdit.iconName
            selectedAccount = transactionToEdit.account
            transactionDate = transactionToEdit.date // Load saved date
        } else if (accounts.isNotEmpty() && selectedAccount == "Cash") {
            // Default account if new
            if (selectedAccount == "Cash" && accounts.any { it.name == "Cash" }) {
                // Keep Cash
            } else {
                selectedAccount = accounts.first().name
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId == -1) "Add Transaction" else "Edit Transaction") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (transactionId != -1) {
                        IconButton(onClick = {
                            scope.launch {
                                if (transactionToEdit != null) {
                                    dao.deleteTransaction(transactionToEdit)
                                    navController.popBackStack()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Type Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                listOf("EXPENSE", "INCOME").forEach { tabType ->
                    val isSelected = type == tabType
                    val bgColor = if (isSelected) {
                        if (tabType == "EXPENSE") Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    } else Color.Transparent
                    val textColor = if (isSelected) {
                        if (tabType == "EXPENSE") Color(0xFFD32F2F) else Color(0xFF388E3C)
                    } else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .clickable { type = tabType }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = if (tabType == "EXPENSE") "Expense" else "Income",
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Account Selector
            Text("Pay Using", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                items(accounts) { account ->
                    val isSelected = selectedAccount == account.name
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedAccount = account.name },
                        label = { Text(account.name) },
                        leadingIcon = { if(isSelected) Icon(Icons.Default.Check, null) }
                    )
                }
                item {
                    IconButton(onClick = { navController.navigate(Screen.Accounts.route) }) {
                        Icon(Icons.Default.Add, "Add")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- DATE PICKER ROW ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { datePickerDialog.show() }
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Date", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(transactionDate)),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            // -----------------------

            Spacer(modifier = Modifier.height(20.dp))

            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                label = { Text("Amount") },
                prefix = { Text("₹", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Note Input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Categories Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 72.dp),
                modifier = Modifier.weight(1f).padding(top = 12.dp)
            ) {
                // "Add Category" Button
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp).clickable { showCategoryDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, "Add")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Add", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Existing Categories
                items(categories) { category ->
                    val isSelected = selectedCategory == category.name
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable {
                                selectedCategory = category.name
                                selectedIcon = category.iconName
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .shadow(if (isSelected) 6.dp else 0.dp, CircleShape)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(category.color) else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                IconUtils.getIconByName(category.iconName),
                                contentDescription = category.name,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            category.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: 0.0
                    if (amountDouble > 0) {
                        scope.launch {
                            val transaction = TransactionEntity(
                                id = if (transactionId == -1) 0 else transactionId,
                                amount = amountDouble,
                                type = type,
                                category = selectedCategory,
                                iconName = selectedIcon,
                                note = note,
                                account = selectedAccount,
                                date = transactionDate // Uses the user-selected date
                            )
                            dao.insertTransaction(transaction)
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (transactionId == -1) "Save Transaction" else "Update Changes", fontSize = 18.sp)
            }
        }
    }

    // --- Add Category Dialog ---
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Name (e.g. Gym)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Icon")
                    LazyRow(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(IconUtils.iconMap.keys.toList()) { iconName ->
                            val isSelected = newCategoryIcon == iconName
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { newCategoryIcon = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    IconUtils.getIconByName(iconName),
                                    null,
                                    tint = if(isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newCategoryName.isNotBlank()) {
                        scope.launch {
                            val colors = listOf(0xFFEF5350, 0xFFEC407A, 0xFFAB47BC, 0xFF7E57C2, 0xFF5C6BC0, 0xFF42A5F5, 0xFF26A69A, 0xFF66BB6A, 0xFFFFA726, 0xFFFF7043)
                            val randomColor = colors.random().toInt()

                            dao.insertCategory(CategoryEntity(name = newCategoryName, iconName = newCategoryIcon, color = randomColor))
                            newCategoryName = ""
                            showCategoryDialog = false
                        }
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }
}