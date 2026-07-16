import re

file_path = '/Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/data/RoomModels.kt'

with open(file_path, 'r') as f:
    content = f.read()

# 1. Add CashTransactionEntity
cash_transaction_entity = """
@Entity(tableName = "cash_transaction")
@Serializable
data class CashTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // "DEPOSIT", "WITHDRAWAL", "CORRECTION", "FEE", "DIVIDEND"
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = ""
)
"""
if "CashTransactionEntity" not in content:
    content = content.replace('@Entity(tableName = "cash")', cash_transaction_entity + '\n@Entity(tableName = "cash")')

# 2. Add CashTransactionDao
cash_transaction_dao = """
@Dao
interface CashTransactionDao {
    @Query("SELECT * FROM cash_transaction ORDER BY dateMillis DESC")
    fun getAllTransactions(): Flow<List<CashTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CashTransactionEntity)
    
    @Delete
    suspend fun deleteTransaction(transaction: CashTransactionEntity)
}
"""
if "CashTransactionDao" not in content:
    content = content.replace('@Dao\ninterface CashDao', cash_transaction_dao + '\n@Dao\ninterface CashDao')

# 3. Add to StockDatabase entities and update version
content = content.replace('PortfolioSnapshotEntity::class\n    ], \n    version = 23', 'PortfolioSnapshotEntity::class,\n        CashTransactionEntity::class\n    ], \n    version = 24')

# 4. Add abstract fun cashTransactionDao
if "abstract fun cashTransactionDao(): CashTransactionDao" not in content:
    content = content.replace('abstract fun cashDao(): CashDao', 'abstract fun cashDao(): CashDao\n    abstract fun cashTransactionDao(): CashTransactionDao')

# 5. Add MIGRATION_23_24
migration_23_24 = """
        val MIGRATION_23_24 = object : androidx.room.migration.Migration(23, 24) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(""\"
                    CREATE TABLE IF NOT EXISTS `cash_transaction` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `amount` REAL NOT NULL, 
                        `type` TEXT NOT NULL, 
                        `dateMillis` INTEGER NOT NULL, 
                        `note` TEXT NOT NULL
                    )
                ""\".trimIndent())
            }
        }
"""
if "MIGRATION_23_24" not in content:
    content = content.replace('val MIGRATION_12_13', migration_23_24 + '\n        val MIGRATION_12_13')

# 6. Add MIGRATION_23_24 to addMigrations array
content = content.replace('MIGRATION_22_23\n            )', 'MIGRATION_22_23,\n                MIGRATION_23_24\n            )')

with open(file_path, 'w') as f:
    f.write(content)

print("Patched RoomModels.kt")
