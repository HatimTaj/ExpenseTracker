package com.example.expensetracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StatsScreen(dao: TransactionDao) {
    val transactions by dao.getAllTransactions().collectAsState(initial = emptyList())
    val categories by dao.getAllCategories().collectAsState(initial = emptyList())

    // Group and Sort
    val categoryStats = transactions
        .filter { it.type == "EXPENSE" }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val totalExpense = categoryStats.sumOf { it.second }

    // Map Category Name to Color
    fun getCategoryColor(name: String): Color {
        val cat = categories.find { it.name == name }
        return if (cat != null) Color(cat.color) else Color.Gray
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Spending Breakdown", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        if (categoryStats.isNotEmpty()) {
            // PIE CHART
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    var startAngle = -90f
                    val total = categoryStats.sumOf { it.second }.toFloat()

                    categoryStats.forEach { (category, amount) ->
                        val sweepAngle = (amount.toFloat() / total) * 360f
                        drawArc(
                            color = getCategoryColor(category),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 40.dp.toPx())
                        )
                        startAngle += sweepAngle
                    }
                }
                // Text in center
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total", style = MaterialTheme.typography.labelSmall)
                    Text("₹${String.format("%.0f", totalExpense)}", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("No data to chart", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(categoryStats) { (category, amount) ->
                val percentage = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
                val color = getCategoryColor(category)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Dot indicator
                            Canvas(modifier = Modifier.size(10.dp), onDraw = { drawCircle(color) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(category, fontWeight = FontWeight.SemiBold)
                        }
                        Text("₹ ${String.format("%.2f", amount)}", fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = percentage,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}