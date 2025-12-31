package com.example.expensetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(navController: NavController, dao: TransactionDao) {
    val accounts by dao.getAllAccounts().collectAsState(initial = emptyList())
    val transactions by dao.getAllTransactions().collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var newAccountName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Wallets") })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(Icons.Default.Add, "Add") },
                text = { Text("Add Wallet") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (accounts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No accounts yet.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(accounts) { account ->
                        val accountBalance = transactions
                            .filter { it.account == account.name }
                            .sumOf { if (it.type == "INCOME") it.amount else -it.amount }

                        AccountCard(
                            account = account,
                            balance = accountBalance,
                            onDelete = { scope.launch { dao.deleteAccount(account) } },
                            onToggleInclude = { isChecked ->
                                scope.launch { dao.updateAccount(account.copy(includeInTotal = isChecked)) }
                            }
                        )
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Add New Wallet") },
                text = {
                    OutlinedTextField(
                        value = newAccountName,
                        onValueChange = { newAccountName = it },
                        label = { Text("Wallet Name") },
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newAccountName.isNotBlank()) {
                            scope.launch {
                                dao.insertAccount(AccountEntity(name = newAccountName, includeInTotal = true))
                                newAccountName = ""
                                showDialog = false
                            }
                        }
                    }) { Text("Add") }
                },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun AccountCard(account: AccountEntity, balance: Double, onDelete: () -> Unit, onToggleInclude: (Boolean) -> Unit) {
    val (color1, color2) = when(account.name.take(1).uppercase()) {
        "C", "S" -> Color(0xFF43A047) to Color(0xFF2E7D32)
        "H", "B" -> Color(0xFF1E88E5) to Color(0xFF1565C0)
        else -> Color(0xFF8E24AA) to Color(0xFF6A1B9A)
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(190.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors = listOf(color1, color2)))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                // Top Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(account.name.uppercase(), color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                // Middle
                Column {
                    Text("Current Balance", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text("₹ ${String.format("%.2f", balance)}", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }

                // Bottom Row (Switch)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Show in Total", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    Switch(
                        checked = account.includeInTotal,
                        onCheckedChange = onToggleInclude,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = color2,
                            checkedTrackColor = Color.White,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }
}