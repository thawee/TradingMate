package apincer.mobile.tradings.`data`

import androidx.collection.ArrayMap
import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndex
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.room.util.recursiveFetchArrayMap
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class StockDao_Impl(
  __db: RoomDatabase,
) : StockDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfPortfolioEntity: EntityInsertAdapter<PortfolioEntity>

  private val __insertAdapterOfStockCacheEntity: EntityInsertAdapter<StockCacheEntity>

  private val __insertAdapterOfStockSignalEntity: EntityInsertAdapter<StockSignalEntity>

  private val __deleteAdapterOfPortfolioEntity: EntityDeleteOrUpdateAdapter<PortfolioEntity>

  private val __deleteAdapterOfStockCacheEntity: EntityDeleteOrUpdateAdapter<StockCacheEntity>

  private val __deleteAdapterOfStockSignalEntity: EntityDeleteOrUpdateAdapter<StockSignalEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfPortfolioEntity = object : EntityInsertAdapter<PortfolioEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `portfolio` (`symbol`,`cost`,`quantity`,`tradePurpose`,`buyFees`,`stopLoss`,`playbookNote`,`peakPrice`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PortfolioEntity) {
        statement.bindText(1, entity.symbol)
        statement.bindDouble(2, entity.cost)
        statement.bindLong(3, entity.quantity.toLong())
        statement.bindText(4, entity.tradePurpose)
        statement.bindDouble(5, entity.buyFees)
        statement.bindDouble(6, entity.stopLoss)
        statement.bindText(7, entity.playbookNote)
        statement.bindDouble(8, entity.peakPrice)
      }
    }
    this.__insertAdapterOfStockCacheEntity = object : EntityInsertAdapter<StockCacheEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `stock_cache` (`symbol`,`name`,`nameTH`,`businessDescription`,`sector`,`industry`,`dividendPerShare`,`lastPrice`,`change`,`percentChange`,`pe`,`pbv`,`roe`,`eps`,`netProfit`,`equity`,`debtToEquity`,`dividendYield`,`dividendDate`,`netProfitMargin`,`profitGrowth3Y`,`lastUpdated`,`volume`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: StockCacheEntity) {
        statement.bindText(1, entity.symbol)
        val _tmpName: String? = entity.name
        if (_tmpName == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpName)
        }
        val _tmpNameTH: String? = entity.nameTH
        if (_tmpNameTH == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpNameTH)
        }
        val _tmpBusinessDescription: String? = entity.businessDescription
        if (_tmpBusinessDescription == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpBusinessDescription)
        }
        val _tmpSector: String? = entity.sector
        if (_tmpSector == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpSector)
        }
        val _tmpIndustry: String? = entity.industry
        if (_tmpIndustry == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpIndustry)
        }
        val _tmpDividendPerShare: Double? = entity.dividendPerShare
        if (_tmpDividendPerShare == null) {
          statement.bindNull(7)
        } else {
          statement.bindDouble(7, _tmpDividendPerShare)
        }
        statement.bindDouble(8, entity.lastPrice)
        statement.bindDouble(9, entity.change)
        statement.bindDouble(10, entity.percentChange)
        val _tmpPe: Double? = entity.pe
        if (_tmpPe == null) {
          statement.bindNull(11)
        } else {
          statement.bindDouble(11, _tmpPe)
        }
        val _tmpPbv: Double? = entity.pbv
        if (_tmpPbv == null) {
          statement.bindNull(12)
        } else {
          statement.bindDouble(12, _tmpPbv)
        }
        val _tmpRoe: Double? = entity.roe
        if (_tmpRoe == null) {
          statement.bindNull(13)
        } else {
          statement.bindDouble(13, _tmpRoe)
        }
        val _tmpEps: Double? = entity.eps
        if (_tmpEps == null) {
          statement.bindNull(14)
        } else {
          statement.bindDouble(14, _tmpEps)
        }
        val _tmpNetProfit: Double? = entity.netProfit
        if (_tmpNetProfit == null) {
          statement.bindNull(15)
        } else {
          statement.bindDouble(15, _tmpNetProfit)
        }
        val _tmpEquity: Double? = entity.equity
        if (_tmpEquity == null) {
          statement.bindNull(16)
        } else {
          statement.bindDouble(16, _tmpEquity)
        }
        val _tmpDebtToEquity: Double? = entity.debtToEquity
        if (_tmpDebtToEquity == null) {
          statement.bindNull(17)
        } else {
          statement.bindDouble(17, _tmpDebtToEquity)
        }
        val _tmpDividendYield: Double? = entity.dividendYield
        if (_tmpDividendYield == null) {
          statement.bindNull(18)
        } else {
          statement.bindDouble(18, _tmpDividendYield)
        }
        val _tmpDividendDate: String? = entity.dividendDate
        if (_tmpDividendDate == null) {
          statement.bindNull(19)
        } else {
          statement.bindText(19, _tmpDividendDate)
        }
        val _tmpNetProfitMargin: Double? = entity.netProfitMargin
        if (_tmpNetProfitMargin == null) {
          statement.bindNull(20)
        } else {
          statement.bindDouble(20, _tmpNetProfitMargin)
        }
        val _tmpProfitGrowth3Y: Double? = entity.profitGrowth3Y
        if (_tmpProfitGrowth3Y == null) {
          statement.bindNull(21)
        } else {
          statement.bindDouble(21, _tmpProfitGrowth3Y)
        }
        val _tmpLastUpdated: String? = entity.lastUpdated
        if (_tmpLastUpdated == null) {
          statement.bindNull(22)
        } else {
          statement.bindText(22, _tmpLastUpdated)
        }
        val _tmpVolume: Long? = entity.volume
        if (_tmpVolume == null) {
          statement.bindNull(23)
        } else {
          statement.bindLong(23, _tmpVolume)
        }
      }
    }
    this.__insertAdapterOfStockSignalEntity = object : EntityInsertAdapter<StockSignalEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `stock_signal` (`symbol`,`rsi`,`macdHist`,`signalType`,`signalReason`,`signalDescription`,`lastUpdated`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: StockSignalEntity) {
        statement.bindText(1, entity.symbol)
        val _tmpRsi: Double? = entity.rsi
        if (_tmpRsi == null) {
          statement.bindNull(2)
        } else {
          statement.bindDouble(2, _tmpRsi)
        }
        val _tmpMacdHist: Double? = entity.macdHist
        if (_tmpMacdHist == null) {
          statement.bindNull(3)
        } else {
          statement.bindDouble(3, _tmpMacdHist)
        }
        val _tmpSignalType: String? = entity.signalType
        if (_tmpSignalType == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpSignalType)
        }
        val _tmpSignalReason: String? = entity.signalReason
        if (_tmpSignalReason == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpSignalReason)
        }
        val _tmpSignalDescription: String? = entity.signalDescription
        if (_tmpSignalDescription == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpSignalDescription)
        }
        val _tmpLastUpdated: String? = entity.lastUpdated
        if (_tmpLastUpdated == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpLastUpdated)
        }
      }
    }
    this.__deleteAdapterOfPortfolioEntity = object : EntityDeleteOrUpdateAdapter<PortfolioEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `portfolio` WHERE `symbol` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: PortfolioEntity) {
        statement.bindText(1, entity.symbol)
      }
    }
    this.__deleteAdapterOfStockCacheEntity = object : EntityDeleteOrUpdateAdapter<StockCacheEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `stock_cache` WHERE `symbol` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: StockCacheEntity) {
        statement.bindText(1, entity.symbol)
      }
    }
    this.__deleteAdapterOfStockSignalEntity = object : EntityDeleteOrUpdateAdapter<StockSignalEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `stock_signal` WHERE `symbol` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: StockSignalEntity) {
        statement.bindText(1, entity.symbol)
      }
    }
  }

  public override suspend fun insertPortfolio(portfolio: PortfolioEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfPortfolioEntity.insert(_connection, portfolio)
  }

  public override suspend fun insertCache(cache: StockCacheEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfStockCacheEntity.insert(_connection, cache)
  }

  public override suspend fun insertSignal(signal: StockSignalEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfStockSignalEntity.insert(_connection, signal)
  }

  public override suspend fun insertPortfolios(portfolios: List<PortfolioEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfPortfolioEntity.insert(_connection, portfolios)
  }

  public override suspend fun insertCaches(caches: List<StockCacheEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfStockCacheEntity.insert(_connection, caches)
  }

  public override suspend fun insertSignals(signals: List<StockSignalEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfStockSignalEntity.insert(_connection, signals)
  }

  public override suspend fun deletePortfolio(portfolio: PortfolioEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfPortfolioEntity.handle(_connection, portfolio)
  }

  public override suspend fun deleteCache(cache: StockCacheEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfStockCacheEntity.handle(_connection, cache)
  }

  public override suspend fun deleteSignal(signal: StockSignalEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfStockSignalEntity.handle(_connection, signal)
  }

  public override fun getAllStocks(): Flow<List<StockAggregate>> {
    val _sql: String = "SELECT * FROM portfolio"
    return createFlow(__db, true, arrayOf("stock_cache", "stock_signal", "portfolio")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfCost: Int = getColumnIndexOrThrow(_stmt, "cost")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfTradePurpose: Int = getColumnIndexOrThrow(_stmt, "tradePurpose")
        val _columnIndexOfBuyFees: Int = getColumnIndexOrThrow(_stmt, "buyFees")
        val _columnIndexOfStopLoss: Int = getColumnIndexOrThrow(_stmt, "stopLoss")
        val _columnIndexOfPlaybookNote: Int = getColumnIndexOrThrow(_stmt, "playbookNote")
        val _columnIndexOfPeakPrice: Int = getColumnIndexOrThrow(_stmt, "peakPrice")
        val _collectionCache: ArrayMap<String, StockCacheEntity?> = ArrayMap<String, StockCacheEntity?>()
        val _collectionSignal: ArrayMap<String, StockSignalEntity?> = ArrayMap<String, StockSignalEntity?>()
        while (_stmt.step()) {
          val _tmpKey: String
          _tmpKey = _stmt.getText(_columnIndexOfSymbol)
          _collectionCache.put(_tmpKey, null)
          val _tmpKey_1: String
          _tmpKey_1 = _stmt.getText(_columnIndexOfSymbol)
          _collectionSignal.put(_tmpKey_1, null)
        }
        _stmt.reset()
        __fetchRelationshipstockCacheAsapincerMobileTradingsDataStockCacheEntity(_connection, _collectionCache)
        __fetchRelationshipstockSignalAsapincerMobileTradingsDataStockSignalEntity(_connection, _collectionSignal)
        val _result: MutableList<StockAggregate> = mutableListOf()
        while (_stmt.step()) {
          val _item: StockAggregate
          val _tmpPortfolio: PortfolioEntity
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpCost: Double
          _tmpCost = _stmt.getDouble(_columnIndexOfCost)
          val _tmpQuantity: Int
          _tmpQuantity = _stmt.getLong(_columnIndexOfQuantity).toInt()
          val _tmpTradePurpose: String
          _tmpTradePurpose = _stmt.getText(_columnIndexOfTradePurpose)
          val _tmpBuyFees: Double
          _tmpBuyFees = _stmt.getDouble(_columnIndexOfBuyFees)
          val _tmpStopLoss: Double
          _tmpStopLoss = _stmt.getDouble(_columnIndexOfStopLoss)
          val _tmpPlaybookNote: String
          _tmpPlaybookNote = _stmt.getText(_columnIndexOfPlaybookNote)
          val _tmpPeakPrice: Double
          _tmpPeakPrice = _stmt.getDouble(_columnIndexOfPeakPrice)
          _tmpPortfolio = PortfolioEntity(_tmpSymbol,_tmpCost,_tmpQuantity,_tmpTradePurpose,_tmpBuyFees,_tmpStopLoss,_tmpPlaybookNote,_tmpPeakPrice)
          val _tmpCache: StockCacheEntity?
          val _tmpKey_2: String
          _tmpKey_2 = _stmt.getText(_columnIndexOfSymbol)
          _tmpCache = _collectionCache.get(_tmpKey_2)
          val _tmpSignal: StockSignalEntity?
          val _tmpKey_3: String
          _tmpKey_3 = _stmt.getText(_columnIndexOfSymbol)
          _tmpSignal = _collectionSignal.get(_tmpKey_3)
          _item = StockAggregate(_tmpPortfolio,_tmpCache,_tmpSignal)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllStocksSync(): List<StockAggregate> {
    val _sql: String = "SELECT * FROM portfolio"
    return performSuspending(__db, true, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfCost: Int = getColumnIndexOrThrow(_stmt, "cost")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfTradePurpose: Int = getColumnIndexOrThrow(_stmt, "tradePurpose")
        val _columnIndexOfBuyFees: Int = getColumnIndexOrThrow(_stmt, "buyFees")
        val _columnIndexOfStopLoss: Int = getColumnIndexOrThrow(_stmt, "stopLoss")
        val _columnIndexOfPlaybookNote: Int = getColumnIndexOrThrow(_stmt, "playbookNote")
        val _columnIndexOfPeakPrice: Int = getColumnIndexOrThrow(_stmt, "peakPrice")
        val _collectionCache: ArrayMap<String, StockCacheEntity?> = ArrayMap<String, StockCacheEntity?>()
        val _collectionSignal: ArrayMap<String, StockSignalEntity?> = ArrayMap<String, StockSignalEntity?>()
        while (_stmt.step()) {
          val _tmpKey: String
          _tmpKey = _stmt.getText(_columnIndexOfSymbol)
          _collectionCache.put(_tmpKey, null)
          val _tmpKey_1: String
          _tmpKey_1 = _stmt.getText(_columnIndexOfSymbol)
          _collectionSignal.put(_tmpKey_1, null)
        }
        _stmt.reset()
        __fetchRelationshipstockCacheAsapincerMobileTradingsDataStockCacheEntity(_connection, _collectionCache)
        __fetchRelationshipstockSignalAsapincerMobileTradingsDataStockSignalEntity(_connection, _collectionSignal)
        val _result: MutableList<StockAggregate> = mutableListOf()
        while (_stmt.step()) {
          val _item: StockAggregate
          val _tmpPortfolio: PortfolioEntity
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpCost: Double
          _tmpCost = _stmt.getDouble(_columnIndexOfCost)
          val _tmpQuantity: Int
          _tmpQuantity = _stmt.getLong(_columnIndexOfQuantity).toInt()
          val _tmpTradePurpose: String
          _tmpTradePurpose = _stmt.getText(_columnIndexOfTradePurpose)
          val _tmpBuyFees: Double
          _tmpBuyFees = _stmt.getDouble(_columnIndexOfBuyFees)
          val _tmpStopLoss: Double
          _tmpStopLoss = _stmt.getDouble(_columnIndexOfStopLoss)
          val _tmpPlaybookNote: String
          _tmpPlaybookNote = _stmt.getText(_columnIndexOfPlaybookNote)
          val _tmpPeakPrice: Double
          _tmpPeakPrice = _stmt.getDouble(_columnIndexOfPeakPrice)
          _tmpPortfolio = PortfolioEntity(_tmpSymbol,_tmpCost,_tmpQuantity,_tmpTradePurpose,_tmpBuyFees,_tmpStopLoss,_tmpPlaybookNote,_tmpPeakPrice)
          val _tmpCache: StockCacheEntity?
          val _tmpKey_2: String
          _tmpKey_2 = _stmt.getText(_columnIndexOfSymbol)
          _tmpCache = _collectionCache.get(_tmpKey_2)
          val _tmpSignal: StockSignalEntity?
          val _tmpKey_3: String
          _tmpKey_3 = _stmt.getText(_columnIndexOfSymbol)
          _tmpSignal = _collectionSignal.get(_tmpKey_3)
          _item = StockAggregate(_tmpPortfolio,_tmpCache,_tmpSignal)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getStockBySymbol(symbol: String): StockAggregate? {
    val _sql: String = "SELECT * FROM portfolio WHERE symbol = ?"
    return performSuspending(__db, true, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, symbol)
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfCost: Int = getColumnIndexOrThrow(_stmt, "cost")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfTradePurpose: Int = getColumnIndexOrThrow(_stmt, "tradePurpose")
        val _columnIndexOfBuyFees: Int = getColumnIndexOrThrow(_stmt, "buyFees")
        val _columnIndexOfStopLoss: Int = getColumnIndexOrThrow(_stmt, "stopLoss")
        val _columnIndexOfPlaybookNote: Int = getColumnIndexOrThrow(_stmt, "playbookNote")
        val _columnIndexOfPeakPrice: Int = getColumnIndexOrThrow(_stmt, "peakPrice")
        val _collectionCache: ArrayMap<String, StockCacheEntity?> = ArrayMap<String, StockCacheEntity?>()
        val _collectionSignal: ArrayMap<String, StockSignalEntity?> = ArrayMap<String, StockSignalEntity?>()
        while (_stmt.step()) {
          val _tmpKey: String
          _tmpKey = _stmt.getText(_columnIndexOfSymbol)
          _collectionCache.put(_tmpKey, null)
          val _tmpKey_1: String
          _tmpKey_1 = _stmt.getText(_columnIndexOfSymbol)
          _collectionSignal.put(_tmpKey_1, null)
        }
        _stmt.reset()
        __fetchRelationshipstockCacheAsapincerMobileTradingsDataStockCacheEntity(_connection, _collectionCache)
        __fetchRelationshipstockSignalAsapincerMobileTradingsDataStockSignalEntity(_connection, _collectionSignal)
        val _result: StockAggregate?
        if (_stmt.step()) {
          val _tmpPortfolio: PortfolioEntity
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpCost: Double
          _tmpCost = _stmt.getDouble(_columnIndexOfCost)
          val _tmpQuantity: Int
          _tmpQuantity = _stmt.getLong(_columnIndexOfQuantity).toInt()
          val _tmpTradePurpose: String
          _tmpTradePurpose = _stmt.getText(_columnIndexOfTradePurpose)
          val _tmpBuyFees: Double
          _tmpBuyFees = _stmt.getDouble(_columnIndexOfBuyFees)
          val _tmpStopLoss: Double
          _tmpStopLoss = _stmt.getDouble(_columnIndexOfStopLoss)
          val _tmpPlaybookNote: String
          _tmpPlaybookNote = _stmt.getText(_columnIndexOfPlaybookNote)
          val _tmpPeakPrice: Double
          _tmpPeakPrice = _stmt.getDouble(_columnIndexOfPeakPrice)
          _tmpPortfolio = PortfolioEntity(_tmpSymbol,_tmpCost,_tmpQuantity,_tmpTradePurpose,_tmpBuyFees,_tmpStopLoss,_tmpPlaybookNote,_tmpPeakPrice)
          val _tmpCache: StockCacheEntity?
          val _tmpKey_2: String
          _tmpKey_2 = _stmt.getText(_columnIndexOfSymbol)
          _tmpCache = _collectionCache.get(_tmpKey_2)
          val _tmpSignal: StockSignalEntity?
          val _tmpKey_3: String
          _tmpKey_3 = _stmt.getText(_columnIndexOfSymbol)
          _tmpSignal = _collectionSignal.get(_tmpKey_3)
          _result = StockAggregate(_tmpPortfolio,_tmpCache,_tmpSignal)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllPortfoliosSync(): List<PortfolioEntity> {
    val _sql: String = "SELECT * FROM portfolio"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfCost: Int = getColumnIndexOrThrow(_stmt, "cost")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfTradePurpose: Int = getColumnIndexOrThrow(_stmt, "tradePurpose")
        val _columnIndexOfBuyFees: Int = getColumnIndexOrThrow(_stmt, "buyFees")
        val _columnIndexOfStopLoss: Int = getColumnIndexOrThrow(_stmt, "stopLoss")
        val _columnIndexOfPlaybookNote: Int = getColumnIndexOrThrow(_stmt, "playbookNote")
        val _columnIndexOfPeakPrice: Int = getColumnIndexOrThrow(_stmt, "peakPrice")
        val _result: MutableList<PortfolioEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PortfolioEntity
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpCost: Double
          _tmpCost = _stmt.getDouble(_columnIndexOfCost)
          val _tmpQuantity: Int
          _tmpQuantity = _stmt.getLong(_columnIndexOfQuantity).toInt()
          val _tmpTradePurpose: String
          _tmpTradePurpose = _stmt.getText(_columnIndexOfTradePurpose)
          val _tmpBuyFees: Double
          _tmpBuyFees = _stmt.getDouble(_columnIndexOfBuyFees)
          val _tmpStopLoss: Double
          _tmpStopLoss = _stmt.getDouble(_columnIndexOfStopLoss)
          val _tmpPlaybookNote: String
          _tmpPlaybookNote = _stmt.getText(_columnIndexOfPlaybookNote)
          val _tmpPeakPrice: Double
          _tmpPeakPrice = _stmt.getDouble(_columnIndexOfPeakPrice)
          _item = PortfolioEntity(_tmpSymbol,_tmpCost,_tmpQuantity,_tmpTradePurpose,_tmpBuyFees,_tmpStopLoss,_tmpPlaybookNote,_tmpPeakPrice)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllCachesSync(): List<StockCacheEntity> {
    val _sql: String = "SELECT * FROM stock_cache"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfNameTH: Int = getColumnIndexOrThrow(_stmt, "nameTH")
        val _columnIndexOfBusinessDescription: Int = getColumnIndexOrThrow(_stmt, "businessDescription")
        val _columnIndexOfSector: Int = getColumnIndexOrThrow(_stmt, "sector")
        val _columnIndexOfIndustry: Int = getColumnIndexOrThrow(_stmt, "industry")
        val _columnIndexOfDividendPerShare: Int = getColumnIndexOrThrow(_stmt, "dividendPerShare")
        val _columnIndexOfLastPrice: Int = getColumnIndexOrThrow(_stmt, "lastPrice")
        val _columnIndexOfChange: Int = getColumnIndexOrThrow(_stmt, "change")
        val _columnIndexOfPercentChange: Int = getColumnIndexOrThrow(_stmt, "percentChange")
        val _columnIndexOfPe: Int = getColumnIndexOrThrow(_stmt, "pe")
        val _columnIndexOfPbv: Int = getColumnIndexOrThrow(_stmt, "pbv")
        val _columnIndexOfRoe: Int = getColumnIndexOrThrow(_stmt, "roe")
        val _columnIndexOfEps: Int = getColumnIndexOrThrow(_stmt, "eps")
        val _columnIndexOfNetProfit: Int = getColumnIndexOrThrow(_stmt, "netProfit")
        val _columnIndexOfEquity: Int = getColumnIndexOrThrow(_stmt, "equity")
        val _columnIndexOfDebtToEquity: Int = getColumnIndexOrThrow(_stmt, "debtToEquity")
        val _columnIndexOfDividendYield: Int = getColumnIndexOrThrow(_stmt, "dividendYield")
        val _columnIndexOfDividendDate: Int = getColumnIndexOrThrow(_stmt, "dividendDate")
        val _columnIndexOfNetProfitMargin: Int = getColumnIndexOrThrow(_stmt, "netProfitMargin")
        val _columnIndexOfProfitGrowth3Y: Int = getColumnIndexOrThrow(_stmt, "profitGrowth3Y")
        val _columnIndexOfLastUpdated: Int = getColumnIndexOrThrow(_stmt, "lastUpdated")
        val _columnIndexOfVolume: Int = getColumnIndexOrThrow(_stmt, "volume")
        val _result: MutableList<StockCacheEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: StockCacheEntity
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpName: String?
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName)
          }
          val _tmpNameTH: String?
          if (_stmt.isNull(_columnIndexOfNameTH)) {
            _tmpNameTH = null
          } else {
            _tmpNameTH = _stmt.getText(_columnIndexOfNameTH)
          }
          val _tmpBusinessDescription: String?
          if (_stmt.isNull(_columnIndexOfBusinessDescription)) {
            _tmpBusinessDescription = null
          } else {
            _tmpBusinessDescription = _stmt.getText(_columnIndexOfBusinessDescription)
          }
          val _tmpSector: String?
          if (_stmt.isNull(_columnIndexOfSector)) {
            _tmpSector = null
          } else {
            _tmpSector = _stmt.getText(_columnIndexOfSector)
          }
          val _tmpIndustry: String?
          if (_stmt.isNull(_columnIndexOfIndustry)) {
            _tmpIndustry = null
          } else {
            _tmpIndustry = _stmt.getText(_columnIndexOfIndustry)
          }
          val _tmpDividendPerShare: Double?
          if (_stmt.isNull(_columnIndexOfDividendPerShare)) {
            _tmpDividendPerShare = null
          } else {
            _tmpDividendPerShare = _stmt.getDouble(_columnIndexOfDividendPerShare)
          }
          val _tmpLastPrice: Double
          _tmpLastPrice = _stmt.getDouble(_columnIndexOfLastPrice)
          val _tmpChange: Double
          _tmpChange = _stmt.getDouble(_columnIndexOfChange)
          val _tmpPercentChange: Double
          _tmpPercentChange = _stmt.getDouble(_columnIndexOfPercentChange)
          val _tmpPe: Double?
          if (_stmt.isNull(_columnIndexOfPe)) {
            _tmpPe = null
          } else {
            _tmpPe = _stmt.getDouble(_columnIndexOfPe)
          }
          val _tmpPbv: Double?
          if (_stmt.isNull(_columnIndexOfPbv)) {
            _tmpPbv = null
          } else {
            _tmpPbv = _stmt.getDouble(_columnIndexOfPbv)
          }
          val _tmpRoe: Double?
          if (_stmt.isNull(_columnIndexOfRoe)) {
            _tmpRoe = null
          } else {
            _tmpRoe = _stmt.getDouble(_columnIndexOfRoe)
          }
          val _tmpEps: Double?
          if (_stmt.isNull(_columnIndexOfEps)) {
            _tmpEps = null
          } else {
            _tmpEps = _stmt.getDouble(_columnIndexOfEps)
          }
          val _tmpNetProfit: Double?
          if (_stmt.isNull(_columnIndexOfNetProfit)) {
            _tmpNetProfit = null
          } else {
            _tmpNetProfit = _stmt.getDouble(_columnIndexOfNetProfit)
          }
          val _tmpEquity: Double?
          if (_stmt.isNull(_columnIndexOfEquity)) {
            _tmpEquity = null
          } else {
            _tmpEquity = _stmt.getDouble(_columnIndexOfEquity)
          }
          val _tmpDebtToEquity: Double?
          if (_stmt.isNull(_columnIndexOfDebtToEquity)) {
            _tmpDebtToEquity = null
          } else {
            _tmpDebtToEquity = _stmt.getDouble(_columnIndexOfDebtToEquity)
          }
          val _tmpDividendYield: Double?
          if (_stmt.isNull(_columnIndexOfDividendYield)) {
            _tmpDividendYield = null
          } else {
            _tmpDividendYield = _stmt.getDouble(_columnIndexOfDividendYield)
          }
          val _tmpDividendDate: String?
          if (_stmt.isNull(_columnIndexOfDividendDate)) {
            _tmpDividendDate = null
          } else {
            _tmpDividendDate = _stmt.getText(_columnIndexOfDividendDate)
          }
          val _tmpNetProfitMargin: Double?
          if (_stmt.isNull(_columnIndexOfNetProfitMargin)) {
            _tmpNetProfitMargin = null
          } else {
            _tmpNetProfitMargin = _stmt.getDouble(_columnIndexOfNetProfitMargin)
          }
          val _tmpProfitGrowth3Y: Double?
          if (_stmt.isNull(_columnIndexOfProfitGrowth3Y)) {
            _tmpProfitGrowth3Y = null
          } else {
            _tmpProfitGrowth3Y = _stmt.getDouble(_columnIndexOfProfitGrowth3Y)
          }
          val _tmpLastUpdated: String?
          if (_stmt.isNull(_columnIndexOfLastUpdated)) {
            _tmpLastUpdated = null
          } else {
            _tmpLastUpdated = _stmt.getText(_columnIndexOfLastUpdated)
          }
          val _tmpVolume: Long?
          if (_stmt.isNull(_columnIndexOfVolume)) {
            _tmpVolume = null
          } else {
            _tmpVolume = _stmt.getLong(_columnIndexOfVolume)
          }
          _item = StockCacheEntity(_tmpSymbol,_tmpName,_tmpNameTH,_tmpBusinessDescription,_tmpSector,_tmpIndustry,_tmpDividendPerShare,_tmpLastPrice,_tmpChange,_tmpPercentChange,_tmpPe,_tmpPbv,_tmpRoe,_tmpEps,_tmpNetProfit,_tmpEquity,_tmpDebtToEquity,_tmpDividendYield,_tmpDividendDate,_tmpNetProfitMargin,_tmpProfitGrowth3Y,_tmpLastUpdated,_tmpVolume)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllSignalsSync(): List<StockSignalEntity> {
    val _sql: String = "SELECT * FROM stock_signal"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfRsi: Int = getColumnIndexOrThrow(_stmt, "rsi")
        val _columnIndexOfMacdHist: Int = getColumnIndexOrThrow(_stmt, "macdHist")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signalType")
        val _columnIndexOfSignalReason: Int = getColumnIndexOrThrow(_stmt, "signalReason")
        val _columnIndexOfSignalDescription: Int = getColumnIndexOrThrow(_stmt, "signalDescription")
        val _columnIndexOfLastUpdated: Int = getColumnIndexOrThrow(_stmt, "lastUpdated")
        val _result: MutableList<StockSignalEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: StockSignalEntity
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpRsi: Double?
          if (_stmt.isNull(_columnIndexOfRsi)) {
            _tmpRsi = null
          } else {
            _tmpRsi = _stmt.getDouble(_columnIndexOfRsi)
          }
          val _tmpMacdHist: Double?
          if (_stmt.isNull(_columnIndexOfMacdHist)) {
            _tmpMacdHist = null
          } else {
            _tmpMacdHist = _stmt.getDouble(_columnIndexOfMacdHist)
          }
          val _tmpSignalType: String?
          if (_stmt.isNull(_columnIndexOfSignalType)) {
            _tmpSignalType = null
          } else {
            _tmpSignalType = _stmt.getText(_columnIndexOfSignalType)
          }
          val _tmpSignalReason: String?
          if (_stmt.isNull(_columnIndexOfSignalReason)) {
            _tmpSignalReason = null
          } else {
            _tmpSignalReason = _stmt.getText(_columnIndexOfSignalReason)
          }
          val _tmpSignalDescription: String?
          if (_stmt.isNull(_columnIndexOfSignalDescription)) {
            _tmpSignalDescription = null
          } else {
            _tmpSignalDescription = _stmt.getText(_columnIndexOfSignalDescription)
          }
          val _tmpLastUpdated: String?
          if (_stmt.isNull(_columnIndexOfLastUpdated)) {
            _tmpLastUpdated = null
          } else {
            _tmpLastUpdated = _stmt.getText(_columnIndexOfLastUpdated)
          }
          _item = StockSignalEntity(_tmpSymbol,_tmpRsi,_tmpMacdHist,_tmpSignalType,_tmpSignalReason,_tmpSignalDescription,_tmpLastUpdated)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getPortfolioBySymbol(symbol: String): PortfolioEntity? {
    val _sql: String = "SELECT * FROM portfolio WHERE symbol = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, symbol)
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfCost: Int = getColumnIndexOrThrow(_stmt, "cost")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfTradePurpose: Int = getColumnIndexOrThrow(_stmt, "tradePurpose")
        val _columnIndexOfBuyFees: Int = getColumnIndexOrThrow(_stmt, "buyFees")
        val _columnIndexOfStopLoss: Int = getColumnIndexOrThrow(_stmt, "stopLoss")
        val _columnIndexOfPlaybookNote: Int = getColumnIndexOrThrow(_stmt, "playbookNote")
        val _columnIndexOfPeakPrice: Int = getColumnIndexOrThrow(_stmt, "peakPrice")
        val _result: PortfolioEntity?
        if (_stmt.step()) {
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpCost: Double
          _tmpCost = _stmt.getDouble(_columnIndexOfCost)
          val _tmpQuantity: Int
          _tmpQuantity = _stmt.getLong(_columnIndexOfQuantity).toInt()
          val _tmpTradePurpose: String
          _tmpTradePurpose = _stmt.getText(_columnIndexOfTradePurpose)
          val _tmpBuyFees: Double
          _tmpBuyFees = _stmt.getDouble(_columnIndexOfBuyFees)
          val _tmpStopLoss: Double
          _tmpStopLoss = _stmt.getDouble(_columnIndexOfStopLoss)
          val _tmpPlaybookNote: String
          _tmpPlaybookNote = _stmt.getText(_columnIndexOfPlaybookNote)
          val _tmpPeakPrice: Double
          _tmpPeakPrice = _stmt.getDouble(_columnIndexOfPeakPrice)
          _result = PortfolioEntity(_tmpSymbol,_tmpCost,_tmpQuantity,_tmpTradePurpose,_tmpBuyFees,_tmpStopLoss,_tmpPlaybookNote,_tmpPeakPrice)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getCacheBySymbol(symbol: String): StockCacheEntity? {
    val _sql: String = "SELECT * FROM stock_cache WHERE symbol = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, symbol)
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfNameTH: Int = getColumnIndexOrThrow(_stmt, "nameTH")
        val _columnIndexOfBusinessDescription: Int = getColumnIndexOrThrow(_stmt, "businessDescription")
        val _columnIndexOfSector: Int = getColumnIndexOrThrow(_stmt, "sector")
        val _columnIndexOfIndustry: Int = getColumnIndexOrThrow(_stmt, "industry")
        val _columnIndexOfDividendPerShare: Int = getColumnIndexOrThrow(_stmt, "dividendPerShare")
        val _columnIndexOfLastPrice: Int = getColumnIndexOrThrow(_stmt, "lastPrice")
        val _columnIndexOfChange: Int = getColumnIndexOrThrow(_stmt, "change")
        val _columnIndexOfPercentChange: Int = getColumnIndexOrThrow(_stmt, "percentChange")
        val _columnIndexOfPe: Int = getColumnIndexOrThrow(_stmt, "pe")
        val _columnIndexOfPbv: Int = getColumnIndexOrThrow(_stmt, "pbv")
        val _columnIndexOfRoe: Int = getColumnIndexOrThrow(_stmt, "roe")
        val _columnIndexOfEps: Int = getColumnIndexOrThrow(_stmt, "eps")
        val _columnIndexOfNetProfit: Int = getColumnIndexOrThrow(_stmt, "netProfit")
        val _columnIndexOfEquity: Int = getColumnIndexOrThrow(_stmt, "equity")
        val _columnIndexOfDebtToEquity: Int = getColumnIndexOrThrow(_stmt, "debtToEquity")
        val _columnIndexOfDividendYield: Int = getColumnIndexOrThrow(_stmt, "dividendYield")
        val _columnIndexOfDividendDate: Int = getColumnIndexOrThrow(_stmt, "dividendDate")
        val _columnIndexOfNetProfitMargin: Int = getColumnIndexOrThrow(_stmt, "netProfitMargin")
        val _columnIndexOfProfitGrowth3Y: Int = getColumnIndexOrThrow(_stmt, "profitGrowth3Y")
        val _columnIndexOfLastUpdated: Int = getColumnIndexOrThrow(_stmt, "lastUpdated")
        val _columnIndexOfVolume: Int = getColumnIndexOrThrow(_stmt, "volume")
        val _result: StockCacheEntity?
        if (_stmt.step()) {
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpName: String?
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName)
          }
          val _tmpNameTH: String?
          if (_stmt.isNull(_columnIndexOfNameTH)) {
            _tmpNameTH = null
          } else {
            _tmpNameTH = _stmt.getText(_columnIndexOfNameTH)
          }
          val _tmpBusinessDescription: String?
          if (_stmt.isNull(_columnIndexOfBusinessDescription)) {
            _tmpBusinessDescription = null
          } else {
            _tmpBusinessDescription = _stmt.getText(_columnIndexOfBusinessDescription)
          }
          val _tmpSector: String?
          if (_stmt.isNull(_columnIndexOfSector)) {
            _tmpSector = null
          } else {
            _tmpSector = _stmt.getText(_columnIndexOfSector)
          }
          val _tmpIndustry: String?
          if (_stmt.isNull(_columnIndexOfIndustry)) {
            _tmpIndustry = null
          } else {
            _tmpIndustry = _stmt.getText(_columnIndexOfIndustry)
          }
          val _tmpDividendPerShare: Double?
          if (_stmt.isNull(_columnIndexOfDividendPerShare)) {
            _tmpDividendPerShare = null
          } else {
            _tmpDividendPerShare = _stmt.getDouble(_columnIndexOfDividendPerShare)
          }
          val _tmpLastPrice: Double
          _tmpLastPrice = _stmt.getDouble(_columnIndexOfLastPrice)
          val _tmpChange: Double
          _tmpChange = _stmt.getDouble(_columnIndexOfChange)
          val _tmpPercentChange: Double
          _tmpPercentChange = _stmt.getDouble(_columnIndexOfPercentChange)
          val _tmpPe: Double?
          if (_stmt.isNull(_columnIndexOfPe)) {
            _tmpPe = null
          } else {
            _tmpPe = _stmt.getDouble(_columnIndexOfPe)
          }
          val _tmpPbv: Double?
          if (_stmt.isNull(_columnIndexOfPbv)) {
            _tmpPbv = null
          } else {
            _tmpPbv = _stmt.getDouble(_columnIndexOfPbv)
          }
          val _tmpRoe: Double?
          if (_stmt.isNull(_columnIndexOfRoe)) {
            _tmpRoe = null
          } else {
            _tmpRoe = _stmt.getDouble(_columnIndexOfRoe)
          }
          val _tmpEps: Double?
          if (_stmt.isNull(_columnIndexOfEps)) {
            _tmpEps = null
          } else {
            _tmpEps = _stmt.getDouble(_columnIndexOfEps)
          }
          val _tmpNetProfit: Double?
          if (_stmt.isNull(_columnIndexOfNetProfit)) {
            _tmpNetProfit = null
          } else {
            _tmpNetProfit = _stmt.getDouble(_columnIndexOfNetProfit)
          }
          val _tmpEquity: Double?
          if (_stmt.isNull(_columnIndexOfEquity)) {
            _tmpEquity = null
          } else {
            _tmpEquity = _stmt.getDouble(_columnIndexOfEquity)
          }
          val _tmpDebtToEquity: Double?
          if (_stmt.isNull(_columnIndexOfDebtToEquity)) {
            _tmpDebtToEquity = null
          } else {
            _tmpDebtToEquity = _stmt.getDouble(_columnIndexOfDebtToEquity)
          }
          val _tmpDividendYield: Double?
          if (_stmt.isNull(_columnIndexOfDividendYield)) {
            _tmpDividendYield = null
          } else {
            _tmpDividendYield = _stmt.getDouble(_columnIndexOfDividendYield)
          }
          val _tmpDividendDate: String?
          if (_stmt.isNull(_columnIndexOfDividendDate)) {
            _tmpDividendDate = null
          } else {
            _tmpDividendDate = _stmt.getText(_columnIndexOfDividendDate)
          }
          val _tmpNetProfitMargin: Double?
          if (_stmt.isNull(_columnIndexOfNetProfitMargin)) {
            _tmpNetProfitMargin = null
          } else {
            _tmpNetProfitMargin = _stmt.getDouble(_columnIndexOfNetProfitMargin)
          }
          val _tmpProfitGrowth3Y: Double?
          if (_stmt.isNull(_columnIndexOfProfitGrowth3Y)) {
            _tmpProfitGrowth3Y = null
          } else {
            _tmpProfitGrowth3Y = _stmt.getDouble(_columnIndexOfProfitGrowth3Y)
          }
          val _tmpLastUpdated: String?
          if (_stmt.isNull(_columnIndexOfLastUpdated)) {
            _tmpLastUpdated = null
          } else {
            _tmpLastUpdated = _stmt.getText(_columnIndexOfLastUpdated)
          }
          val _tmpVolume: Long?
          if (_stmt.isNull(_columnIndexOfVolume)) {
            _tmpVolume = null
          } else {
            _tmpVolume = _stmt.getLong(_columnIndexOfVolume)
          }
          _result = StockCacheEntity(_tmpSymbol,_tmpName,_tmpNameTH,_tmpBusinessDescription,_tmpSector,_tmpIndustry,_tmpDividendPerShare,_tmpLastPrice,_tmpChange,_tmpPercentChange,_tmpPe,_tmpPbv,_tmpRoe,_tmpEps,_tmpNetProfit,_tmpEquity,_tmpDebtToEquity,_tmpDividendYield,_tmpDividendDate,_tmpNetProfitMargin,_tmpProfitGrowth3Y,_tmpLastUpdated,_tmpVolume)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getSignalBySymbol(symbol: String): StockSignalEntity? {
    val _sql: String = "SELECT * FROM stock_signal WHERE symbol = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, symbol)
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfRsi: Int = getColumnIndexOrThrow(_stmt, "rsi")
        val _columnIndexOfMacdHist: Int = getColumnIndexOrThrow(_stmt, "macdHist")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signalType")
        val _columnIndexOfSignalReason: Int = getColumnIndexOrThrow(_stmt, "signalReason")
        val _columnIndexOfSignalDescription: Int = getColumnIndexOrThrow(_stmt, "signalDescription")
        val _columnIndexOfLastUpdated: Int = getColumnIndexOrThrow(_stmt, "lastUpdated")
        val _result: StockSignalEntity?
        if (_stmt.step()) {
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpRsi: Double?
          if (_stmt.isNull(_columnIndexOfRsi)) {
            _tmpRsi = null
          } else {
            _tmpRsi = _stmt.getDouble(_columnIndexOfRsi)
          }
          val _tmpMacdHist: Double?
          if (_stmt.isNull(_columnIndexOfMacdHist)) {
            _tmpMacdHist = null
          } else {
            _tmpMacdHist = _stmt.getDouble(_columnIndexOfMacdHist)
          }
          val _tmpSignalType: String?
          if (_stmt.isNull(_columnIndexOfSignalType)) {
            _tmpSignalType = null
          } else {
            _tmpSignalType = _stmt.getText(_columnIndexOfSignalType)
          }
          val _tmpSignalReason: String?
          if (_stmt.isNull(_columnIndexOfSignalReason)) {
            _tmpSignalReason = null
          } else {
            _tmpSignalReason = _stmt.getText(_columnIndexOfSignalReason)
          }
          val _tmpSignalDescription: String?
          if (_stmt.isNull(_columnIndexOfSignalDescription)) {
            _tmpSignalDescription = null
          } else {
            _tmpSignalDescription = _stmt.getText(_columnIndexOfSignalDescription)
          }
          val _tmpLastUpdated: String?
          if (_stmt.isNull(_columnIndexOfLastUpdated)) {
            _tmpLastUpdated = null
          } else {
            _tmpLastUpdated = _stmt.getText(_columnIndexOfLastUpdated)
          }
          _result = StockSignalEntity(_tmpSymbol,_tmpRsi,_tmpMacdHist,_tmpSignalType,_tmpSignalReason,_tmpSignalDescription,_tmpLastUpdated)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteWatchlistStocks() {
    val _sql: String = "DELETE FROM portfolio WHERE quantity = 0 AND cost = 0.0"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __fetchRelationshipstockCacheAsapincerMobileTradingsDataStockCacheEntity(_connection: SQLiteConnection, _map: ArrayMap<String, StockCacheEntity?>) {
    val __mapKeySet: Set<String> = _map.keys
    if (__mapKeySet.isEmpty()) {
      return
    }
    if (_map.size > 999) {
      recursiveFetchArrayMap(_map, false) { _tmpMap ->
        __fetchRelationshipstockCacheAsapincerMobileTradingsDataStockCacheEntity(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `symbol`,`name`,`nameTH`,`businessDescription`,`sector`,`industry`,`dividendPerShare`,`lastPrice`,`change`,`percentChange`,`pe`,`pbv`,`roe`,`eps`,`netProfit`,`equity`,`debtToEquity`,`dividendYield`,`dividendDate`,`netProfitMargin`,`profitGrowth3Y`,`lastUpdated`,`volume` FROM `stock_cache` WHERE `symbol` IN (")
    val _inputSize: Int = __mapKeySet.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (_item: String in __mapKeySet) {
      _stmt.bindText(_argIndex, _item)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "symbol")
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfSymbol: Int = 0
      val _columnIndexOfName: Int = 1
      val _columnIndexOfNameTH: Int = 2
      val _columnIndexOfBusinessDescription: Int = 3
      val _columnIndexOfSector: Int = 4
      val _columnIndexOfIndustry: Int = 5
      val _columnIndexOfDividendPerShare: Int = 6
      val _columnIndexOfLastPrice: Int = 7
      val _columnIndexOfChange: Int = 8
      val _columnIndexOfPercentChange: Int = 9
      val _columnIndexOfPe: Int = 10
      val _columnIndexOfPbv: Int = 11
      val _columnIndexOfRoe: Int = 12
      val _columnIndexOfEps: Int = 13
      val _columnIndexOfNetProfit: Int = 14
      val _columnIndexOfEquity: Int = 15
      val _columnIndexOfDebtToEquity: Int = 16
      val _columnIndexOfDividendYield: Int = 17
      val _columnIndexOfDividendDate: Int = 18
      val _columnIndexOfNetProfitMargin: Int = 19
      val _columnIndexOfProfitGrowth3Y: Int = 20
      val _columnIndexOfLastUpdated: Int = 21
      val _columnIndexOfVolume: Int = 22
      while (_stmt.step()) {
        val _tmpKey: String
        _tmpKey = _stmt.getText(_itemKeyIndex)
        if (_map.containsKey(_tmpKey)) {
          val _item_1: StockCacheEntity?
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpName: String?
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName)
          }
          val _tmpNameTH: String?
          if (_stmt.isNull(_columnIndexOfNameTH)) {
            _tmpNameTH = null
          } else {
            _tmpNameTH = _stmt.getText(_columnIndexOfNameTH)
          }
          val _tmpBusinessDescription: String?
          if (_stmt.isNull(_columnIndexOfBusinessDescription)) {
            _tmpBusinessDescription = null
          } else {
            _tmpBusinessDescription = _stmt.getText(_columnIndexOfBusinessDescription)
          }
          val _tmpSector: String?
          if (_stmt.isNull(_columnIndexOfSector)) {
            _tmpSector = null
          } else {
            _tmpSector = _stmt.getText(_columnIndexOfSector)
          }
          val _tmpIndustry: String?
          if (_stmt.isNull(_columnIndexOfIndustry)) {
            _tmpIndustry = null
          } else {
            _tmpIndustry = _stmt.getText(_columnIndexOfIndustry)
          }
          val _tmpDividendPerShare: Double?
          if (_stmt.isNull(_columnIndexOfDividendPerShare)) {
            _tmpDividendPerShare = null
          } else {
            _tmpDividendPerShare = _stmt.getDouble(_columnIndexOfDividendPerShare)
          }
          val _tmpLastPrice: Double
          _tmpLastPrice = _stmt.getDouble(_columnIndexOfLastPrice)
          val _tmpChange: Double
          _tmpChange = _stmt.getDouble(_columnIndexOfChange)
          val _tmpPercentChange: Double
          _tmpPercentChange = _stmt.getDouble(_columnIndexOfPercentChange)
          val _tmpPe: Double?
          if (_stmt.isNull(_columnIndexOfPe)) {
            _tmpPe = null
          } else {
            _tmpPe = _stmt.getDouble(_columnIndexOfPe)
          }
          val _tmpPbv: Double?
          if (_stmt.isNull(_columnIndexOfPbv)) {
            _tmpPbv = null
          } else {
            _tmpPbv = _stmt.getDouble(_columnIndexOfPbv)
          }
          val _tmpRoe: Double?
          if (_stmt.isNull(_columnIndexOfRoe)) {
            _tmpRoe = null
          } else {
            _tmpRoe = _stmt.getDouble(_columnIndexOfRoe)
          }
          val _tmpEps: Double?
          if (_stmt.isNull(_columnIndexOfEps)) {
            _tmpEps = null
          } else {
            _tmpEps = _stmt.getDouble(_columnIndexOfEps)
          }
          val _tmpNetProfit: Double?
          if (_stmt.isNull(_columnIndexOfNetProfit)) {
            _tmpNetProfit = null
          } else {
            _tmpNetProfit = _stmt.getDouble(_columnIndexOfNetProfit)
          }
          val _tmpEquity: Double?
          if (_stmt.isNull(_columnIndexOfEquity)) {
            _tmpEquity = null
          } else {
            _tmpEquity = _stmt.getDouble(_columnIndexOfEquity)
          }
          val _tmpDebtToEquity: Double?
          if (_stmt.isNull(_columnIndexOfDebtToEquity)) {
            _tmpDebtToEquity = null
          } else {
            _tmpDebtToEquity = _stmt.getDouble(_columnIndexOfDebtToEquity)
          }
          val _tmpDividendYield: Double?
          if (_stmt.isNull(_columnIndexOfDividendYield)) {
            _tmpDividendYield = null
          } else {
            _tmpDividendYield = _stmt.getDouble(_columnIndexOfDividendYield)
          }
          val _tmpDividendDate: String?
          if (_stmt.isNull(_columnIndexOfDividendDate)) {
            _tmpDividendDate = null
          } else {
            _tmpDividendDate = _stmt.getText(_columnIndexOfDividendDate)
          }
          val _tmpNetProfitMargin: Double?
          if (_stmt.isNull(_columnIndexOfNetProfitMargin)) {
            _tmpNetProfitMargin = null
          } else {
            _tmpNetProfitMargin = _stmt.getDouble(_columnIndexOfNetProfitMargin)
          }
          val _tmpProfitGrowth3Y: Double?
          if (_stmt.isNull(_columnIndexOfProfitGrowth3Y)) {
            _tmpProfitGrowth3Y = null
          } else {
            _tmpProfitGrowth3Y = _stmt.getDouble(_columnIndexOfProfitGrowth3Y)
          }
          val _tmpLastUpdated: String?
          if (_stmt.isNull(_columnIndexOfLastUpdated)) {
            _tmpLastUpdated = null
          } else {
            _tmpLastUpdated = _stmt.getText(_columnIndexOfLastUpdated)
          }
          val _tmpVolume: Long?
          if (_stmt.isNull(_columnIndexOfVolume)) {
            _tmpVolume = null
          } else {
            _tmpVolume = _stmt.getLong(_columnIndexOfVolume)
          }
          _item_1 = StockCacheEntity(_tmpSymbol,_tmpName,_tmpNameTH,_tmpBusinessDescription,_tmpSector,_tmpIndustry,_tmpDividendPerShare,_tmpLastPrice,_tmpChange,_tmpPercentChange,_tmpPe,_tmpPbv,_tmpRoe,_tmpEps,_tmpNetProfit,_tmpEquity,_tmpDebtToEquity,_tmpDividendYield,_tmpDividendDate,_tmpNetProfitMargin,_tmpProfitGrowth3Y,_tmpLastUpdated,_tmpVolume)
          _map.put(_tmpKey, _item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  private fun __fetchRelationshipstockSignalAsapincerMobileTradingsDataStockSignalEntity(_connection: SQLiteConnection, _map: ArrayMap<String, StockSignalEntity?>) {
    val __mapKeySet: Set<String> = _map.keys
    if (__mapKeySet.isEmpty()) {
      return
    }
    if (_map.size > 999) {
      recursiveFetchArrayMap(_map, false) { _tmpMap ->
        __fetchRelationshipstockSignalAsapincerMobileTradingsDataStockSignalEntity(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `symbol`,`rsi`,`macdHist`,`signalType`,`signalReason`,`signalDescription`,`lastUpdated` FROM `stock_signal` WHERE `symbol` IN (")
    val _inputSize: Int = __mapKeySet.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (_item: String in __mapKeySet) {
      _stmt.bindText(_argIndex, _item)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "symbol")
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfSymbol: Int = 0
      val _columnIndexOfRsi: Int = 1
      val _columnIndexOfMacdHist: Int = 2
      val _columnIndexOfSignalType: Int = 3
      val _columnIndexOfSignalReason: Int = 4
      val _columnIndexOfSignalDescription: Int = 5
      val _columnIndexOfLastUpdated: Int = 6
      while (_stmt.step()) {
        val _tmpKey: String
        _tmpKey = _stmt.getText(_itemKeyIndex)
        if (_map.containsKey(_tmpKey)) {
          val _item_1: StockSignalEntity?
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpRsi: Double?
          if (_stmt.isNull(_columnIndexOfRsi)) {
            _tmpRsi = null
          } else {
            _tmpRsi = _stmt.getDouble(_columnIndexOfRsi)
          }
          val _tmpMacdHist: Double?
          if (_stmt.isNull(_columnIndexOfMacdHist)) {
            _tmpMacdHist = null
          } else {
            _tmpMacdHist = _stmt.getDouble(_columnIndexOfMacdHist)
          }
          val _tmpSignalType: String?
          if (_stmt.isNull(_columnIndexOfSignalType)) {
            _tmpSignalType = null
          } else {
            _tmpSignalType = _stmt.getText(_columnIndexOfSignalType)
          }
          val _tmpSignalReason: String?
          if (_stmt.isNull(_columnIndexOfSignalReason)) {
            _tmpSignalReason = null
          } else {
            _tmpSignalReason = _stmt.getText(_columnIndexOfSignalReason)
          }
          val _tmpSignalDescription: String?
          if (_stmt.isNull(_columnIndexOfSignalDescription)) {
            _tmpSignalDescription = null
          } else {
            _tmpSignalDescription = _stmt.getText(_columnIndexOfSignalDescription)
          }
          val _tmpLastUpdated: String?
          if (_stmt.isNull(_columnIndexOfLastUpdated)) {
            _tmpLastUpdated = null
          } else {
            _tmpLastUpdated = _stmt.getText(_columnIndexOfLastUpdated)
          }
          _item_1 = StockSignalEntity(_tmpSymbol,_tmpRsi,_tmpMacdHist,_tmpSignalType,_tmpSignalReason,_tmpSignalDescription,_tmpLastUpdated)
          _map.put(_tmpKey, _item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
