package com.example.expensetracker

import android.content.Context
import android.graphics.Color
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [TransactionEntity::class, AccountEntity::class, CategoryEntity::class, BudgetEntity::class], version = 5, exportSchema = false)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var Instance: ExpenseDatabase? = null

        fun getDatabase(context: Context): ExpenseDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, ExpenseDatabase::class.java, "expense_db")
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Instance?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    val dao = database.transactionDao()
                                    // Default Data
                                    dao.insertAccount(AccountEntity(name = "Cash", type = "Cash", includeInTotal = true))
                                    dao.insertAccount(AccountEntity(name = "Bank", type = "Bank", includeInTotal = true))
                                    dao.insertCategory(CategoryEntity(name = "Food", iconName = "Food", color = Color.parseColor("#FF5722")))
                                    dao.insertCategory(CategoryEntity(name = "Shopping", iconName = "Shopping", color = Color.parseColor("#2196F3")))
                                    dao.insertCategory(CategoryEntity(name = "Transport", iconName = "Transport", color = Color.parseColor("#FFC107")))
                                    dao.insertCategory(CategoryEntity(name = "Health", iconName = "Health", color = Color.parseColor("#E91E63")))
                                    // Default Budget: 0
                                    dao.insertBudget(BudgetEntity(amount = 0.0))
                                }
                            }
                        }
                    })
                    .build()
                    .also { Instance = it }
            }
        }
    }
}