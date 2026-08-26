package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Account
import com.example.data.model.DeletedAccount
import com.example.data.model.DeletedTransaction
import com.example.data.model.Transaction

@Database(entities = [Account::class, Transaction::class, DeletedAccount::class, DeletedTransaction::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun trashDao(): TrashDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE transactions ADD COLUMN currency TEXT NOT NULL DEFAULT 'YER'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE transactions ADD COLUMN exchangeRate REAL NOT NULL DEFAULT 1.0") } catch (e: Exception) {}
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE accounts ADD COLUMN creditLimit REAL NOT NULL DEFAULT 0.0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE accounts ADD COLUMN tag TEXT NOT NULL DEFAULT ''") } catch (e: Exception) {}
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE transactions ADD COLUMN dueDate TEXT NOT NULL DEFAULT ''") } catch (e: Exception) {}
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS deleted_accounts (
                            id INTEGER PRIMARY KEY NOT NULL, 
                            name TEXT NOT NULL, 
                            phone TEXT NOT NULL, 
                            type TEXT NOT NULL, 
                            createdAt INTEGER NOT NULL, 
                            creditLimit REAL NOT NULL, 
                            tag TEXT NOT NULL, 
                            deletedAt INTEGER NOT NULL
                        )
                    """.trimIndent())
                } catch (e: Exception) {}
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS deleted_transactions (
                            id INTEGER PRIMARY KEY NOT NULL, 
                            accountId INTEGER NOT NULL, 
                            day TEXT NOT NULL, 
                            date TEXT NOT NULL, 
                            details TEXT NOT NULL, 
                            quantity REAL NOT NULL, 
                            unitPrice REAL NOT NULL, 
                            addition REAL NOT NULL, 
                            total REAL NOT NULL, 
                            isPayment INTEGER NOT NULL, 
                            timestamp INTEGER NOT NULL, 
                            currency TEXT NOT NULL, 
                            exchangeRate REAL NOT NULL, 
                            dueDate TEXT NOT NULL, 
                            deletedAt INTEGER NOT NULL
                        )
                    """.trimIndent())
                } catch (e: Exception) {}
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE accounts ADD COLUMN initialBalance REAL NOT NULL DEFAULT 0.0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE deleted_accounts ADD COLUMN initialBalance REAL NOT NULL DEFAULT 0.0") } catch (e: Exception) {}
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pro_ledger_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
