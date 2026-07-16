package apincer.mobile.tradings.`data`

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class StockDatabase_Impl : StockDatabase() {
  private val _stockDao: Lazy<StockDao> = lazy {
    StockDao_Impl(this)
  }

  private val _tradeDao: Lazy<TradeDao> = lazy {
    TradeDao_Impl(this)
  }

  private val _cashDao: Lazy<CashDao> = lazy {
    CashDao_Impl(this)
  }

  private val _cashTransactionDao: Lazy<CashTransactionDao> = lazy {
    CashTransactionDao_Impl(this)
  }

  private val _focusDao: Lazy<FocusDao> = lazy {
    FocusDao_Impl(this)
  }

  private val _checklistDao: Lazy<ChecklistDao> = lazy {
    ChecklistDao_Impl(this)
  }

  private val _dividendDao: Lazy<DividendDao> = lazy {
    DividendDao_Impl(this)
  }

  private val _portfolioSnapshotDao: Lazy<PortfolioSnapshotDao> = lazy {
    PortfolioSnapshotDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(25, "9da92a332a14c758c6f9f922bfed932d", "15e4cec9e4b9fb0829c4b1cf347d6a73") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `portfolio` (`symbol` TEXT NOT NULL, `cost` REAL NOT NULL, `quantity` INTEGER NOT NULL, `tradePurpose` TEXT NOT NULL, `buyFees` REAL NOT NULL, `stopLoss` REAL NOT NULL, `playbookNote` TEXT NOT NULL, `peakPrice` REAL NOT NULL, PRIMARY KEY(`symbol`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `stock_cache` (`symbol` TEXT NOT NULL, `name` TEXT, `nameTH` TEXT, `businessDescription` TEXT, `sector` TEXT, `industry` TEXT, `dividendPerShare` REAL, `lastPrice` REAL NOT NULL, `change` REAL NOT NULL, `percentChange` REAL NOT NULL, `pe` REAL, `pbv` REAL, `roe` REAL, `eps` REAL, `netProfit` REAL, `equity` REAL, `debtToEquity` REAL, `dividendYield` REAL, `dividendDate` TEXT, `netProfitMargin` REAL, `profitGrowth3Y` REAL, `lastUpdated` TEXT, `volume` INTEGER, PRIMARY KEY(`symbol`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `stock_signal` (`symbol` TEXT NOT NULL, `rsi` REAL, `macdHist` REAL, `signalType` TEXT, `signalReason` TEXT, `signalDescription` TEXT, `lastUpdated` TEXT, PRIMARY KEY(`symbol`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `trade_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `symbol` TEXT NOT NULL, `buyPrice` REAL NOT NULL, `sellPrice` REAL NOT NULL, `quantity` INTEGER NOT NULL, `netProfitPercent` REAL NOT NULL, `netProfitBaht` REAL NOT NULL, `dateMillis` INTEGER NOT NULL, `note` TEXT NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trade_history_dateMillis` ON `trade_history` (`dateMillis`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `cash` (`id` INTEGER NOT NULL, `balance` REAL NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `focus_list` (`symbol` TEXT NOT NULL, `startPrice` REAL NOT NULL, `targetPrice` REAL NOT NULL, `addedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`symbol`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `discipline_checklist` (`id` INTEGER NOT NULL, `lastResetDate` TEXT NOT NULL, `swingDailyDone` INTEGER NOT NULL, `swingWeeklyDone` INTEGER NOT NULL, `swingAiDone` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `dividend_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `symbol` TEXT NOT NULL, `dateMillis` INTEGER NOT NULL, `amountPerShare` REAL NOT NULL, `sharesHeld` INTEGER NOT NULL, `totalReceived` REAL NOT NULL, `taxDeducted` REAL NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `portfolio_snapshot` (`date` TEXT NOT NULL, `totalValue` REAL NOT NULL, `totalCost` REAL NOT NULL, `cashBalance` REAL NOT NULL, PRIMARY KEY(`date`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `cash_transaction` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `dateMillis` INTEGER NOT NULL, `note` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '9da92a332a14c758c6f9f922bfed932d')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `portfolio`")
        connection.execSQL("DROP TABLE IF EXISTS `stock_cache`")
        connection.execSQL("DROP TABLE IF EXISTS `stock_signal`")
        connection.execSQL("DROP TABLE IF EXISTS `trade_history`")
        connection.execSQL("DROP TABLE IF EXISTS `cash`")
        connection.execSQL("DROP TABLE IF EXISTS `focus_list`")
        connection.execSQL("DROP TABLE IF EXISTS `discipline_checklist`")
        connection.execSQL("DROP TABLE IF EXISTS `dividend_history`")
        connection.execSQL("DROP TABLE IF EXISTS `portfolio_snapshot`")
        connection.execSQL("DROP TABLE IF EXISTS `cash_transaction`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsPortfolio: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPortfolio.put("symbol", TableInfo.Column("symbol", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPortfolio.put("cost", TableInfo.Column("cost", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPortfolio.put("quantity", TableInfo.Column("quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPortfolio.put("tradePurpose", TableInfo.Column("tradePurpose", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPortfolio.put("buyFees", TableInfo.Column("buyFees", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPortfolio.put("stopLoss", TableInfo.Column("stopLoss", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPortfolio.put("playbookNote", TableInfo.Column("playbookNote", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPortfolio.put("peakPrice", TableInfo.Column("peakPrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPortfolio: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPortfolio: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPortfolio: TableInfo = TableInfo("portfolio", _columnsPortfolio, _foreignKeysPortfolio, _indicesPortfolio)
        val _existingPortfolio: TableInfo = read(connection, "portfolio")
        if (!_infoPortfolio.equals(_existingPortfolio)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |portfolio(apincer.mobile.tradings.data.PortfolioEntity).
              | Expected:
              |""".trimMargin() + _infoPortfolio + """
              |
              | Found:
              |""".trimMargin() + _existingPortfolio)
        }
        val _columnsStockCache: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsStockCache.put("symbol", TableInfo.Column("symbol", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("name", TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("nameTH", TableInfo.Column("nameTH", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("businessDescription", TableInfo.Column("businessDescription", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("sector", TableInfo.Column("sector", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("industry", TableInfo.Column("industry", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("dividendPerShare", TableInfo.Column("dividendPerShare", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("lastPrice", TableInfo.Column("lastPrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("change", TableInfo.Column("change", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("percentChange", TableInfo.Column("percentChange", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("pe", TableInfo.Column("pe", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("pbv", TableInfo.Column("pbv", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("roe", TableInfo.Column("roe", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("eps", TableInfo.Column("eps", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("netProfit", TableInfo.Column("netProfit", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("equity", TableInfo.Column("equity", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("debtToEquity", TableInfo.Column("debtToEquity", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("dividendYield", TableInfo.Column("dividendYield", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("dividendDate", TableInfo.Column("dividendDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("netProfitMargin", TableInfo.Column("netProfitMargin", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("profitGrowth3Y", TableInfo.Column("profitGrowth3Y", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("lastUpdated", TableInfo.Column("lastUpdated", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockCache.put("volume", TableInfo.Column("volume", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysStockCache: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesStockCache: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoStockCache: TableInfo = TableInfo("stock_cache", _columnsStockCache, _foreignKeysStockCache, _indicesStockCache)
        val _existingStockCache: TableInfo = read(connection, "stock_cache")
        if (!_infoStockCache.equals(_existingStockCache)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |stock_cache(apincer.mobile.tradings.data.StockCacheEntity).
              | Expected:
              |""".trimMargin() + _infoStockCache + """
              |
              | Found:
              |""".trimMargin() + _existingStockCache)
        }
        val _columnsStockSignal: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsStockSignal.put("symbol", TableInfo.Column("symbol", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockSignal.put("rsi", TableInfo.Column("rsi", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockSignal.put("macdHist", TableInfo.Column("macdHist", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockSignal.put("signalType", TableInfo.Column("signalType", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockSignal.put("signalReason", TableInfo.Column("signalReason", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockSignal.put("signalDescription", TableInfo.Column("signalDescription", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockSignal.put("lastUpdated", TableInfo.Column("lastUpdated", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysStockSignal: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesStockSignal: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoStockSignal: TableInfo = TableInfo("stock_signal", _columnsStockSignal, _foreignKeysStockSignal, _indicesStockSignal)
        val _existingStockSignal: TableInfo = read(connection, "stock_signal")
        if (!_infoStockSignal.equals(_existingStockSignal)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |stock_signal(apincer.mobile.tradings.data.StockSignalEntity).
              | Expected:
              |""".trimMargin() + _infoStockSignal + """
              |
              | Found:
              |""".trimMargin() + _existingStockSignal)
        }
        val _columnsTradeHistory: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTradeHistory.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTradeHistory.put("symbol", TableInfo.Column("symbol", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTradeHistory.put("buyPrice", TableInfo.Column("buyPrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTradeHistory.put("sellPrice", TableInfo.Column("sellPrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTradeHistory.put("quantity", TableInfo.Column("quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTradeHistory.put("netProfitPercent", TableInfo.Column("netProfitPercent", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTradeHistory.put("netProfitBaht", TableInfo.Column("netProfitBaht", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTradeHistory.put("dateMillis", TableInfo.Column("dateMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTradeHistory.put("note", TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTradeHistory: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTradeHistory: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesTradeHistory.add(TableInfo.Index("index_trade_history_dateMillis", false, listOf("dateMillis"), listOf("ASC")))
        val _infoTradeHistory: TableInfo = TableInfo("trade_history", _columnsTradeHistory, _foreignKeysTradeHistory, _indicesTradeHistory)
        val _existingTradeHistory: TableInfo = read(connection, "trade_history")
        if (!_infoTradeHistory.equals(_existingTradeHistory)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |trade_history(apincer.mobile.tradings.data.TradeEntity).
              | Expected:
              |""".trimMargin() + _infoTradeHistory + """
              |
              | Found:
              |""".trimMargin() + _existingTradeHistory)
        }
        val _columnsCash: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCash.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCash.put("balance", TableInfo.Column("balance", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCash: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCash: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCash: TableInfo = TableInfo("cash", _columnsCash, _foreignKeysCash, _indicesCash)
        val _existingCash: TableInfo = read(connection, "cash")
        if (!_infoCash.equals(_existingCash)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |cash(apincer.mobile.tradings.data.CashEntity).
              | Expected:
              |""".trimMargin() + _infoCash + """
              |
              | Found:
              |""".trimMargin() + _existingCash)
        }
        val _columnsFocusList: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFocusList.put("symbol", TableInfo.Column("symbol", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusList.put("startPrice", TableInfo.Column("startPrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusList.put("targetPrice", TableInfo.Column("targetPrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusList.put("addedAtMillis", TableInfo.Column("addedAtMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFocusList: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFocusList: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoFocusList: TableInfo = TableInfo("focus_list", _columnsFocusList, _foreignKeysFocusList, _indicesFocusList)
        val _existingFocusList: TableInfo = read(connection, "focus_list")
        if (!_infoFocusList.equals(_existingFocusList)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |focus_list(apincer.mobile.tradings.data.FocusEntity).
              | Expected:
              |""".trimMargin() + _infoFocusList + """
              |
              | Found:
              |""".trimMargin() + _existingFocusList)
        }
        val _columnsDisciplineChecklist: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsDisciplineChecklist.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDisciplineChecklist.put("lastResetDate", TableInfo.Column("lastResetDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDisciplineChecklist.put("swingDailyDone", TableInfo.Column("swingDailyDone", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDisciplineChecklist.put("swingWeeklyDone", TableInfo.Column("swingWeeklyDone", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDisciplineChecklist.put("swingAiDone", TableInfo.Column("swingAiDone", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysDisciplineChecklist: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesDisciplineChecklist: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoDisciplineChecklist: TableInfo = TableInfo("discipline_checklist", _columnsDisciplineChecklist, _foreignKeysDisciplineChecklist, _indicesDisciplineChecklist)
        val _existingDisciplineChecklist: TableInfo = read(connection, "discipline_checklist")
        if (!_infoDisciplineChecklist.equals(_existingDisciplineChecklist)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |discipline_checklist(apincer.mobile.tradings.data.ChecklistEntity).
              | Expected:
              |""".trimMargin() + _infoDisciplineChecklist + """
              |
              | Found:
              |""".trimMargin() + _existingDisciplineChecklist)
        }
        val _columnsDividendHistory: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsDividendHistory.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDividendHistory.put("symbol", TableInfo.Column("symbol", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDividendHistory.put("dateMillis", TableInfo.Column("dateMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDividendHistory.put("amountPerShare", TableInfo.Column("amountPerShare", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDividendHistory.put("sharesHeld", TableInfo.Column("sharesHeld", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDividendHistory.put("totalReceived", TableInfo.Column("totalReceived", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDividendHistory.put("taxDeducted", TableInfo.Column("taxDeducted", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysDividendHistory: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesDividendHistory: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoDividendHistory: TableInfo = TableInfo("dividend_history", _columnsDividendHistory, _foreignKeysDividendHistory, _indicesDividendHistory)
        val _existingDividendHistory: TableInfo = read(connection, "dividend_history")
        if (!_infoDividendHistory.equals(_existingDividendHistory)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |dividend_history(apincer.mobile.tradings.data.DividendHistoryEntity).
              | Expected:
              |""".trimMargin() + _infoDividendHistory + """
              |
              | Found:
              |""".trimMargin() + _existingDividendHistory)
        }
        val _columnsPortfolioSnapshot: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPortfolioSnapshot.put("date", TableInfo.Column("date", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPortfolioSnapshot.put("totalValue", TableInfo.Column("totalValue", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPortfolioSnapshot.put("totalCost", TableInfo.Column("totalCost", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPortfolioSnapshot.put("cashBalance", TableInfo.Column("cashBalance", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPortfolioSnapshot: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPortfolioSnapshot: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPortfolioSnapshot: TableInfo = TableInfo("portfolio_snapshot", _columnsPortfolioSnapshot, _foreignKeysPortfolioSnapshot, _indicesPortfolioSnapshot)
        val _existingPortfolioSnapshot: TableInfo = read(connection, "portfolio_snapshot")
        if (!_infoPortfolioSnapshot.equals(_existingPortfolioSnapshot)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |portfolio_snapshot(apincer.mobile.tradings.data.PortfolioSnapshotEntity).
              | Expected:
              |""".trimMargin() + _infoPortfolioSnapshot + """
              |
              | Found:
              |""".trimMargin() + _existingPortfolioSnapshot)
        }
        val _columnsCashTransaction: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCashTransaction.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCashTransaction.put("amount", TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCashTransaction.put("type", TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCashTransaction.put("dateMillis", TableInfo.Column("dateMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCashTransaction.put("note", TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCashTransaction: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCashTransaction: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCashTransaction: TableInfo = TableInfo("cash_transaction", _columnsCashTransaction, _foreignKeysCashTransaction, _indicesCashTransaction)
        val _existingCashTransaction: TableInfo = read(connection, "cash_transaction")
        if (!_infoCashTransaction.equals(_existingCashTransaction)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |cash_transaction(apincer.mobile.tradings.data.CashTransactionEntity).
              | Expected:
              |""".trimMargin() + _infoCashTransaction + """
              |
              | Found:
              |""".trimMargin() + _existingCashTransaction)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "portfolio", "stock_cache", "stock_signal", "trade_history", "cash", "focus_list", "discipline_checklist", "dividend_history", "portfolio_snapshot", "cash_transaction")
  }

  public override fun clearAllTables() {
    super.performClear(false, "portfolio", "stock_cache", "stock_signal", "trade_history", "cash", "focus_list", "discipline_checklist", "dividend_history", "portfolio_snapshot", "cash_transaction")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(StockDao::class, StockDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(TradeDao::class, TradeDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CashDao::class, CashDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CashTransactionDao::class, CashTransactionDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(FocusDao::class, FocusDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ChecklistDao::class, ChecklistDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(DividendDao::class, DividendDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(PortfolioSnapshotDao::class, PortfolioSnapshotDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun stockDao(): StockDao = _stockDao.value

  public override fun tradeDao(): TradeDao = _tradeDao.value

  public override fun cashDao(): CashDao = _cashDao.value

  public override fun cashTransactionDao(): CashTransactionDao = _cashTransactionDao.value

  public override fun focusDao(): FocusDao = _focusDao.value

  public override fun checklistDao(): ChecklistDao = _checklistDao.value

  public override fun dividendDao(): DividendDao = _dividendDao.value

  public override fun portfolioSnapshotDao(): PortfolioSnapshotDao = _portfolioSnapshotDao.value
}
