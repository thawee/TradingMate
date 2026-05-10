package apincer.mobile.tradings.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val symbol: String,
    val name: String? = null,
    val nameTH: String? = null,
    val businessDescription: String? = null,
    val cost: Double = 0.0,
    val quantity: Int = 0,
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
    val lastUpdated: String? = null
)

@Entity(tableName = "trade_history")
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
data class CashEntity(
    @PrimaryKey val id: Int = 1,
    val balance: Double = 0.0
)

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks")
    fun getAllStocks(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks WHERE symbol = :symbol")
    suspend fun getStockBySymbol(symbol: String): StockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockEntity)

    @Delete
    suspend fun deleteStock(stock: StockEntity)

    @Query("DELETE FROM stocks WHERE quantity = 0")
    suspend fun deleteWatchlistStocks()
}

@Dao
interface TradeDao {
    @Query("SELECT * FROM trade_history ORDER BY dateMillis DESC")
    fun getAllTrades(): Flow<List<TradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeEntity)

    @Query("DELETE FROM trade_history")
    suspend fun clearHistory()
}

@Dao
interface CashDao {
    @Query("SELECT * FROM cash WHERE id = 1")
    fun getCash(): Flow<CashEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCash(cash: CashEntity)
}

@Database(entities = [StockEntity::class, TradeEntity::class, CashEntity::class], version = 8)
abstract class StockDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun tradeDao(): TradeDao
    abstract fun cashDao(): CashDao

    companion object {
        @Volatile
        private var INSTANCE: StockDatabase? = null

        fun getDatabase(context: android.content.Context): StockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StockDatabase::class.java,
                    "stock_database"
                )
                .fallbackToDestructiveMigration() // Simple migration for early dev
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
