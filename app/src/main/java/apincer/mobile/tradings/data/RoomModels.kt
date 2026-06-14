package apincer.mobile.tradings.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class TradingBackup(
    val stocks: List<StockEntity>,
    val focusList: List<FocusEntity>,
    val trades: List<TradeEntity>,
    val cashBalance: Double
)

@Entity(tableName = "stocks")
@Serializable
data class StockEntity(
    @PrimaryKey val symbol: String,
    val name: String? = null,
    val nameTH: String? = null,
    val businessDescription: String? = null,
    val sector: String? = null,
    val industry: String? = null,
    val cost: Double = 0.0,
    val quantity: Int = 0,
    val tradePurpose: String = "SWING",
    val buyFees: Double = 0.0,
    val dividendPerShare: Double? = null,
    // Cached dynamic data for offline-first display
    val lastPrice: Double = 0.0,
    val change: Double = 0.0,
    val percentChange: Double = 0.0,
    val pe: Double? = null,
    val pbv: Double? = null,
    val roe: Double? = null,
    val eps: Double? = null,
    val netProfit: Double? = null,
    val equity: Double? = null,
    val debtToEquity: Double? = null,
    val dividendYield: Double? = null,
    val dividendDate: String? = null,
    val rsi: Double? = null,
    val macdHist: Double? = null,
    val signalType: String? = null, // BUY, SELL, NEUTRAL
    val signalReason: String? = null,
    val signalDescription: String? = null,
    val lastUpdated: String? = null,
    val stopLoss: Double = 0.0,
    val playbookNote: String = ""
)

@Entity(tableName = "trade_history")
@Serializable
data class TradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val buyPrice: Double,
    val sellPrice: Double,
    val quantity: Int,
    val netProfitPercent: Double,
    val netProfitBaht: Double,
    val dateMillis: Long,
    val note: String = "" // Lessons learned
)

@Entity(tableName = "cash")
@Serializable
data class CashEntity(
    @PrimaryKey val id: Int = 1,
    val balance: Double = 0.0
)

@Entity(tableName = "focus_list")
@Serializable
data class FocusEntity(
    @PrimaryKey val symbol: String,
    val startPrice: Double,
    val targetPrice: Double = 0.0,
    val addedAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks")
    fun getAllStocks(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks")
    suspend fun getAllStocksSync(): List<StockEntity>

    @Query("SELECT * FROM stocks WHERE symbol = :symbol")
    suspend fun getStockBySymbol(symbol: String): StockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockEntity>)

    @Delete
    suspend fun deleteStock(stock: StockEntity)

    @Query("DELETE FROM stocks WHERE quantity = 0 AND cost = 0.0")
    suspend fun deleteWatchlistStocks()
}

@Dao
interface TradeDao {
    @Query("SELECT * FROM trade_history ORDER BY dateMillis DESC")
    fun getAllTrades(): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trade_history ORDER BY dateMillis DESC")
    suspend fun getAllTradesSync(): List<TradeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrades(trades: List<TradeEntity>)

    @Query("DELETE FROM trade_history")
    suspend fun clearHistory()
}

@Dao
interface CashDao {
    @Query("SELECT * FROM cash WHERE id = 1")
    fun getCash(): Flow<CashEntity?>

    @Query("SELECT * FROM cash WHERE id = 1")
    suspend fun getCashSync(): CashEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCash(cash: CashEntity)
}

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_list ORDER BY addedAtMillis DESC")
    fun getAllFocusStocks(): Flow<List<FocusEntity>>

    @Query("SELECT * FROM focus_list ORDER BY addedAtMillis DESC")
    suspend fun getAllFocusStocksSync(): List<FocusEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusStock(focusStock: FocusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusStocks(focusStocks: List<FocusEntity>)

    @Delete
    suspend fun deleteFocusStock(focusStock: FocusEntity)

    @Query("SELECT * FROM focus_list WHERE symbol = :symbol")
    suspend fun getFocusStockBySymbol(symbol: String): FocusEntity?

    @Query("DELETE FROM focus_list WHERE symbol NOT IN (SELECT symbol FROM stocks WHERE quantity > 0)")
    suspend fun clearFocusList()
}

@Entity(tableName = "discipline_checklist")
@Serializable
data class ChecklistEntity(
    @PrimaryKey val id: Int = 1,
    val lastResetDate: String = "",
    val lastResetWeek: Int = 0,
    val lastResetMonth: Int = 0,
    val swingDailyDone: Boolean = false,
    val swingWeeklyDone: Boolean = false,
    val swingAiDone: Boolean = false,
    val divWeeklyDone: Boolean = false,
    val divWeeklyPricesDone: Boolean = false,
    val divMonthlyDone: Boolean = false,
    val divAiDone: Boolean = false
)

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM discipline_checklist WHERE id = 1")
    fun getChecklistFlow(): Flow<ChecklistEntity?>

    @Query("SELECT * FROM discipline_checklist WHERE id = 1")
    suspend fun getChecklist(): ChecklistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: ChecklistEntity)
}

@Database(entities = [StockEntity::class, TradeEntity::class, CashEntity::class, FocusEntity::class, ChecklistEntity::class], version = 16)
abstract class StockDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun tradeDao(): TradeDao
    abstract fun cashDao(): CashDao
    abstract fun focusDao(): FocusDao
    abstract fun checklistDao(): ChecklistDao

    companion object {
        @Volatile
        private var INSTANCE: StockDatabase? = null

        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stocks ADD COLUMN tradePurpose TEXT NOT NULL DEFAULT 'SWING'")
            }
        }

        val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stocks ADD COLUMN buyFees REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE stocks ADD COLUMN dividendPerShare REAL")
            }
        }

        val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `discipline_checklist` (
                        `id` INTEGER NOT NULL, 
                        `lastResetDate` TEXT NOT NULL, 
                        `lastResetWeek` INTEGER NOT NULL, 
                        `lastResetMonth` INTEGER NOT NULL, 
                        `swingDailyDone` INTEGER NOT NULL, 
                        `swingWeeklyDone` INTEGER NOT NULL, 
                        `swingAiDone` INTEGER NOT NULL, 
                        `divWeeklyDone` INTEGER NOT NULL, 
                        `divWeeklyPricesDone` INTEGER NOT NULL, 
                        `divMonthlyDone` INTEGER NOT NULL, 
                        `divAiDone` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stocks ADD COLUMN stopLoss REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE stocks ADD COLUMN playbookNote TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: android.content.Context): StockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StockDatabase::class.java,
                    "stock_database"
                )
                .addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
