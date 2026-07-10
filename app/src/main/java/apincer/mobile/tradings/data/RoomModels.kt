package apincer.mobile.tradings.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class TradingBackup(
    val watchlistSymbols: List<String>,
    val portfolioItems: List<SimplePortfolio>,
    val cashBalance: Double
)

@Serializable
data class SimplePortfolio(
    val symbol: String,
    val cost: Double,
    val quantity: Int,
    val tradePurpose: String
)

@Entity(tableName = "portfolio")
@Serializable
data class PortfolioEntity(
    @PrimaryKey val symbol: String,
    val cost: Double = 0.0,
    val quantity: Int = 0,
    val tradePurpose: String = "SWING",
    val buyFees: Double = 0.0,
    val stopLoss: Double = 0.0,
    val playbookNote: String = "",
    val peakPrice: Double = 0.0
)

@Entity(tableName = "portfolio_snapshot")
@Serializable
data class PortfolioSnapshotEntity(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val totalValue: Double,
    val totalCost: Double,
    val cashBalance: Double
)

@Entity(tableName = "stock_cache")
@Serializable
data class StockCacheEntity(
    @PrimaryKey val symbol: String,
    val name: String? = null,
    val nameTH: String? = null,
    val businessDescription: String? = null,
    val sector: String? = null,
    val industry: String? = null,
    val dividendPerShare: Double? = null,
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
    val netProfitMargin: Double? = null,
    val profitGrowth3Y: Double? = null,
    val lastUpdated: String? = null
)

@Entity(tableName = "stock_signal")
@Serializable
data class StockSignalEntity(
    @PrimaryKey val symbol: String,
    val rsi: Double? = null,
    val macdHist: Double? = null,
    val signalType: String? = null, // BUY, SELL, NEUTRAL
    val signalReason: String? = null,
    val signalDescription: String? = null,
    val lastUpdated: String? = null
)

data class StockAggregate(
    @Embedded val portfolio: PortfolioEntity,
    @Relation(
        parentColumn = "symbol",
        entityColumn = "symbol"
    )
    val cache: StockCacheEntity?,
    @Relation(
        parentColumn = "symbol",
        entityColumn = "symbol"
    )
    val signal: StockSignalEntity?
) {
    val symbol: String get() = portfolio.symbol
    val name: String? get() = cache?.name
    val nameTH: String? get() = cache?.nameTH
    val businessDescription: String? get() = cache?.businessDescription
    val sector: String? get() = cache?.sector
    val industry: String? get() = cache?.industry
    val cost: Double get() = portfolio.cost
    val quantity: Int get() = portfolio.quantity
    val tradePurpose: String get() = portfolio.tradePurpose
    val buyFees: Double get() = portfolio.buyFees
    val dividendPerShare: Double? get() = cache?.dividendPerShare
    val lastPrice: Double get() = cache?.lastPrice ?: 0.0
    val change: Double get() = cache?.change ?: 0.0
    val percentChange: Double get() = cache?.percentChange ?: 0.0
    val pe: Double? get() = cache?.pe
    val pbv: Double? get() = cache?.pbv
    val roe: Double? get() = cache?.roe
    val eps: Double? get() = cache?.eps
    val netProfit: Double? get() = cache?.netProfit
    val equity: Double? get() = cache?.equity
    val debtToEquity: Double? get() = cache?.debtToEquity
    val dividendYield: Double? get() = cache?.dividendYield
    val dividendDate: String? get() = cache?.dividendDate
    val rsi: Double? get() = signal?.rsi
    val macdHist: Double? get() = signal?.macdHist
    val signalType: String? get() = signal?.signalType
    val signalReason: String? get() = signal?.signalReason
    val signalDescription: String? get() = signal?.signalDescription
    val lastUpdated: String? get() = cache?.lastUpdated ?: signal?.lastUpdated
    val stopLoss: Double get() = portfolio.stopLoss
    val playbookNote: String get() = portfolio.playbookNote
    val peakPrice: Double get() = portfolio.peakPrice
    val netProfitMargin: Double? get() = cache?.netProfitMargin
    val profitGrowth3Y: Double? get() = cache?.profitGrowth3Y

    fun toScrapedStockInfo(): ScrapedStockInfo = ScrapedStockInfo(
        symbol = symbol,
        name = name,
        nameTH = nameTH,
        businessDescription = businessDescription,
        sector = sector,
        industry = industry,
        lastPrice = lastPrice,
        change = change,
        percentChange = percentChange,
        pe = pe,
        pbv = pbv,
        roe = roe,
        eps = eps,
        netProfit = netProfit,
        netProfitMargin = netProfitMargin,
        profitGrowth3Y = profitGrowth3Y,
        equity = equity,
        debtToEquity = debtToEquity,
        dividendYield = dividendYield,
        dividendDate = dividendDate,
        lastUpdated = lastUpdated ?: ""
    )
}

@Entity(
    tableName = "trade_history",
    indices = [Index(value = ["dateMillis"])]
)
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


@Entity(tableName = "cash_transaction")
@Serializable
data class CashTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // "DEPOSIT", "WITHDRAWAL", "CORRECTION", "FEE", "DIVIDEND"
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = ""
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
    @Transaction
    @Query("SELECT * FROM portfolio")
    fun getAllStocks(): Flow<List<StockAggregate>>

    @Transaction
    @Query("SELECT * FROM portfolio")
    suspend fun getAllStocksSync(): List<StockAggregate>

    @Transaction
    @Query("SELECT * FROM portfolio WHERE symbol = :symbol")
    suspend fun getStockBySymbol(symbol: String): StockAggregate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortfolio(portfolio: PortfolioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: StockCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: StockSignalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortfolios(portfolios: List<PortfolioEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaches(caches: List<StockCacheEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignals(signals: List<StockSignalEntity>)

    @Delete
    suspend fun deletePortfolio(portfolio: PortfolioEntity)

    @Delete
    suspend fun deleteCache(cache: StockCacheEntity)

    @Delete
    suspend fun deleteSignal(signal: StockSignalEntity)

    @Query("DELETE FROM portfolio WHERE quantity = 0 AND cost = 0.0")
    suspend fun deleteWatchlistStocks()
    
    @Query("SELECT * FROM portfolio")
    suspend fun getAllPortfoliosSync(): List<PortfolioEntity>
    
    @Query("SELECT * FROM stock_cache")
    suspend fun getAllCachesSync(): List<StockCacheEntity>
    
    @Query("SELECT * FROM stock_signal")
    suspend fun getAllSignalsSync(): List<StockSignalEntity>
    
    @Query("SELECT * FROM portfolio WHERE symbol = :symbol")
    suspend fun getPortfolioBySymbol(symbol: String): PortfolioEntity?
    
    @Query("SELECT * FROM stock_cache WHERE symbol = :symbol")
    suspend fun getCacheBySymbol(symbol: String): StockCacheEntity?
    
    @Query("SELECT * FROM stock_signal WHERE symbol = :symbol")
    suspend fun getSignalBySymbol(symbol: String): StockSignalEntity?
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

    @Delete
    suspend fun deleteTrade(trade: TradeEntity)
}


@Dao
interface CashTransactionDao {
    @Query("SELECT * FROM cash_transaction ORDER BY dateMillis DESC")
    fun getAllTransactions(): Flow<List<CashTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CashTransactionEntity)
    
    @Delete
    suspend fun deleteTransaction(transaction: CashTransactionEntity)
}

@Dao
interface CashDao {
    @Query("SELECT * FROM cash WHERE id = 1")
    fun getCash(): Flow<CashEntity?>

    @Query("SELECT * FROM cash WHERE id = 1")
    suspend fun getCashSync(): CashEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCash(cash: CashEntity)

    @Query("UPDATE cash SET balance = balance + :amount WHERE id = 1")
    suspend fun adjustCashBy(amount: Double)
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

    @Query("DELETE FROM focus_list WHERE symbol NOT IN (SELECT symbol FROM portfolio WHERE quantity > 0)")
    suspend fun clearFocusList()
}

@Entity(tableName = "discipline_checklist")
@Serializable
data class ChecklistEntity(
    @PrimaryKey val id: Int = 1,
    val lastResetDate: String = "",
    val swingDailyDone: Boolean = false,
    val swingWeeklyDone: Boolean = false,
    val swingAiDone: Boolean = false
)

@Entity(tableName = "dividend_history")
@Serializable
data class DividendHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val dateMillis: Long,
    val amountPerShare: Double,
    val sharesHeld: Int,
    val totalReceived: Double,
    val taxDeducted: Double
)

@Dao
interface DividendDao {
    @Query("SELECT * FROM dividend_history ORDER BY dateMillis DESC")
    fun getAllDividends(): Flow<List<DividendHistoryEntity>>

    @Query("SELECT * FROM dividend_history WHERE symbol = :symbol ORDER BY dateMillis DESC")
    fun getDividendsBySymbol(symbol: String): Flow<List<DividendHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDividend(dividend: DividendHistoryEntity)

    @Delete
    suspend fun deleteDividend(dividend: DividendHistoryEntity)
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM discipline_checklist WHERE id = 1")
    fun getChecklistFlow(): Flow<ChecklistEntity?>

    @Query("SELECT * FROM discipline_checklist WHERE id = 1")
    suspend fun getChecklist(): ChecklistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: ChecklistEntity)
}

@Dao
interface PortfolioSnapshotDao {
    @Query("SELECT * FROM portfolio_snapshot ORDER BY date DESC")
    fun getAllSnapshots(): Flow<List<PortfolioSnapshotEntity>>

    @Query("SELECT * FROM portfolio_snapshot ORDER BY date DESC")
    suspend fun getAllSnapshotsSync(): List<PortfolioSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: PortfolioSnapshotEntity)

    @Query("DELETE FROM portfolio_snapshot WHERE date < :beforeDate")
    suspend fun deleteOldSnapshots(beforeDate: String)
}

@Database(
    entities = [
        PortfolioEntity::class, 
        StockCacheEntity::class, 
        StockSignalEntity::class, 
        TradeEntity::class, 
        CashEntity::class, 
        FocusEntity::class, 
        ChecklistEntity::class,
        DividendHistoryEntity::class,
        PortfolioSnapshotEntity::class,
        CashTransactionEntity::class
    ], 
    version = 24
)
abstract class StockDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun tradeDao(): TradeDao
    abstract fun cashDao(): CashDao
    abstract fun cashTransactionDao(): CashTransactionDao
    abstract fun focusDao(): FocusDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun dividendDao(): DividendDao
    abstract fun portfolioSnapshotDao(): PortfolioSnapshotDao

    companion object {
        @Volatile
        private var INSTANCE: StockDatabase? = null

        
        val MIGRATION_23_24 = object : androidx.room.migration.Migration(23, 24) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cash_transaction` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `amount` REAL NOT NULL, 
                        `type` TEXT NOT NULL, 
                        `dateMillis` INTEGER NOT NULL, 
                        `note` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

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

        val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stocks ADD COLUMN netProfitMargin REAL")
                db.execSQL("ALTER TABLE stocks ADD COLUMN profitGrowth3Y REAL")
            }
        }

        val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trade_history_dateMillis ON trade_history(dateMillis)")
            }
        }

        val MIGRATION_18_19 = object : androidx.room.migration.Migration(18, 19) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Create new tables
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `portfolio` (
                        `symbol` TEXT NOT NULL, 
                        `cost` REAL NOT NULL, 
                        `quantity` INTEGER NOT NULL, 
                        `tradePurpose` TEXT NOT NULL, 
                        `buyFees` REAL NOT NULL, 
                        `stopLoss` REAL NOT NULL, 
                        `playbookNote` TEXT NOT NULL, 
                        PRIMARY KEY(`symbol`)
                    )
                """.trimIndent())
                
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stock_cache` (
                        `symbol` TEXT NOT NULL, 
                        `name` TEXT, 
                        `nameTH` TEXT, 
                        `businessDescription` TEXT, 
                        `sector` TEXT, 
                        `industry` TEXT, 
                        `dividendPerShare` REAL, 
                        `lastPrice` REAL NOT NULL, 
                        `change` REAL NOT NULL, 
                        `percentChange` REAL NOT NULL, 
                        `pe` REAL, 
                        `pbv` REAL, 
                        `roe` REAL, 
                        `eps` REAL, 
                        `netProfit` REAL, 
                        `equity` REAL, 
                        `debtToEquity` REAL, 
                        `dividendYield` REAL, 
                        `dividendDate` TEXT, 
                        `netProfitMargin` REAL, 
                        `profitGrowth3Y` REAL, 
                        `lastUpdated` TEXT, 
                        PRIMARY KEY(`symbol`)
                    )
                """.trimIndent())
                
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stock_signal` (
                        `symbol` TEXT NOT NULL, 
                        `rsi` REAL, 
                        `macdHist` REAL, 
                        `signalType` TEXT, 
                        `signalReason` TEXT, 
                        `signalDescription` TEXT, 
                        `lastUpdated` TEXT, 
                        PRIMARY KEY(`symbol`)
                    )
                """.trimIndent())

                // Copy data from old stocks table
                db.execSQL("""
                    INSERT INTO portfolio (symbol, cost, quantity, tradePurpose, buyFees, stopLoss, playbookNote)
                    SELECT symbol, cost, quantity, tradePurpose, buyFees, stopLoss, playbookNote FROM stocks
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO stock_cache (symbol, name, nameTH, businessDescription, sector, industry, dividendPerShare, lastPrice, change, percentChange, pe, pbv, roe, eps, netProfit, equity, debtToEquity, dividendYield, dividendDate, netProfitMargin, profitGrowth3Y, lastUpdated)
                    SELECT symbol, name, nameTH, businessDescription, sector, industry, dividendPerShare, lastPrice, change, percentChange, pe, pbv, roe, eps, netProfit, equity, debtToEquity, dividendYield, dividendDate, netProfitMargin, profitGrowth3Y, lastUpdated FROM stocks
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO stock_signal (symbol, rsi, macdHist, signalType, signalReason, signalDescription, lastUpdated)
                    SELECT symbol, rsi, macdHist, signalType, signalReason, signalDescription, lastUpdated FROM stocks
                """.trimIndent())

                // Drop old stocks table
                db.execSQL("DROP TABLE stocks")
            }
        }

        val MIGRATION_19_20 = object : androidx.room.migration.Migration(19, 20) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Create new checklist table without DIVIDEND columns and reset week/month
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `discipline_checklist_new` (
                        `id` INTEGER NOT NULL, 
                        `lastResetDate` TEXT NOT NULL, 
                        `swingDailyDone` INTEGER NOT NULL, 
                        `swingWeeklyDone` INTEGER NOT NULL, 
                        `swingAiDone` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                
                // Copy data from old table
                db.execSQL("""
                    INSERT INTO discipline_checklist_new (id, lastResetDate, swingDailyDone, swingWeeklyDone, swingAiDone)
                    SELECT id, lastResetDate, swingDailyDone, swingWeeklyDone, swingAiDone FROM discipline_checklist
                """.trimIndent())
                
                // Drop old table
                db.execSQL("DROP TABLE discipline_checklist")
                
                // Rename new table
                db.execSQL("ALTER TABLE discipline_checklist_new RENAME TO discipline_checklist")
            }
        }

        val MIGRATION_20_21 = object : androidx.room.migration.Migration(20, 21) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE portfolio ADD COLUMN peakPrice REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_21_22 = object : androidx.room.migration.Migration(21, 22) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `dividend_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `symbol` TEXT NOT NULL, 
                        `dateMillis` INTEGER NOT NULL, 
                        `amountPerShare` REAL NOT NULL, 
                        `sharesHeld` INTEGER NOT NULL, 
                        `totalReceived` REAL NOT NULL, 
                        `taxDeducted` REAL NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_22_23 = object : androidx.room.migration.Migration(22, 23) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `portfolio_snapshot` (
                        `date` TEXT NOT NULL, 
                        `totalValue` REAL NOT NULL, 
                        `totalCost` REAL NOT NULL, 
                        `cashBalance` REAL NOT NULL, 
                        PRIMARY KEY(`date`)
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: android.content.Context): StockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StockDatabase::class.java,
                    "stock_database"
                )
                .addMigrations(
                    MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, 
                    MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, 
                    MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
                    MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
