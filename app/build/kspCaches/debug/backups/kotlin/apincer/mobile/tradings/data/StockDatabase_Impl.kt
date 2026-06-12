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

  private val _focusDao: Lazy<FocusDao> = lazy {
    FocusDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(13, "bb27f39f5ae27ac0ed0bbd2572566115", "7dc0994c8fda98e3ac0f2e902cb37ba7") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `stocks` (`symbol` TEXT NOT NULL, `name` TEXT, `nameTH` TEXT, `businessDescription` TEXT, `sector` TEXT, `industry` TEXT, `cost` REAL NOT NULL, `quantity` INTEGER NOT NULL, `tradePurpose` TEXT NOT NULL, `lastPrice` REAL NOT NULL, `change` REAL NOT NULL, `percentChange` REAL NOT NULL, `pe` REAL, `pbv` REAL, `roe` REAL, `eps` REAL, `netProfit` REAL, `equity` REAL, `debtToEquity` REAL, `dividendYield` REAL, `dividendDate` TEXT, `rsi` REAL, `macdHist` REAL, `signalType` TEXT, `signalReason` TEXT, `signalDescription` TEXT, `lastUpdated` TEXT, PRIMARY KEY(`symbol`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `trade_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `symbol` TEXT NOT NULL, `buyPrice` REAL NOT NULL, `sellPrice` REAL NOT NULL, `quantity` INTEGER NOT NULL, `netProfitPercent` REAL NOT NULL, `netProfitBaht` REAL NOT NULL, `dateMillis` INTEGER NOT NULL, `note` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `cash` (`id` INTEGER NOT NULL, `balance` REAL NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `focus_list` (`symbol` TEXT NOT NULL, `startPrice` REAL NOT NULL, `targetPrice` REAL NOT NULL, `addedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`symbol`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'bb27f39f5ae27ac0ed0bbd2572566115')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `stocks`")
        connection.execSQL("DROP TABLE IF EXISTS `trade_history`")
        connection.execSQL("DROP TABLE IF EXISTS `cash`")
        connection.execSQL("DROP TABLE IF EXISTS `focus_list`")
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
        val _columnsStocks: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsStocks.put("symbol", TableInfo.Column("symbol", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("name", TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("nameTH", TableInfo.Column("nameTH", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("businessDescription", TableInfo.Column("businessDescription", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("sector", TableInfo.Column("sector", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("industry", TableInfo.Column("industry", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("cost", TableInfo.Column("cost", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("quantity", TableInfo.Column("quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("tradePurpose", TableInfo.Column("tradePurpose", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("lastPrice", TableInfo.Column("lastPrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("change", TableInfo.Column("change", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("percentChange", TableInfo.Column("percentChange", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("pe", TableInfo.Column("pe", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("pbv", TableInfo.Column("pbv", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("roe", TableInfo.Column("roe", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("eps", TableInfo.Column("eps", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("netProfit", TableInfo.Column("netProfit", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("equity", TableInfo.Column("equity", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("debtToEquity", TableInfo.Column("debtToEquity", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("dividendYield", TableInfo.Column("dividendYield", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("dividendDate", TableInfo.Column("dividendDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("rsi", TableInfo.Column("rsi", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("macdHist", TableInfo.Column("macdHist", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("signalType", TableInfo.Column("signalType", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("signalReason", TableInfo.Column("signalReason", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("signalDescription", TableInfo.Column("signalDescription", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStocks.put("lastUpdated", TableInfo.Column("lastUpdated", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysStocks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesStocks: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoStocks: TableInfo = TableInfo("stocks", _columnsStocks, _foreignKeysStocks, _indicesStocks)
        val _existingStocks: TableInfo = read(connection, "stocks")
        if (!_infoStocks.equals(_existingStocks)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |stocks(apincer.mobile.tradings.data.StockEntity).
              | Expected:
              |""".trimMargin() + _infoStocks + """
              |
              | Found:
              |""".trimMargin() + _existingStocks)
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
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "stocks", "trade_history", "cash", "focus_list")
  }

  public override fun clearAllTables() {
    super.performClear(false, "stocks", "trade_history", "cash", "focus_list")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(StockDao::class, StockDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(TradeDao::class, TradeDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CashDao::class, CashDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(FocusDao::class, FocusDao_Impl.getRequiredConverters())
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

  public override fun focusDao(): FocusDao = _focusDao.value
}
