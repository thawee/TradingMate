package apincer.mobile.tradings.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class StockDao_Impl(
  __db: RoomDatabase,
) : StockDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfStockEntity: EntityInsertAdapter<StockEntity>

  private val __deleteAdapterOfStockEntity: EntityDeleteOrUpdateAdapter<StockEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfStockEntity = object : EntityInsertAdapter<StockEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `stocks` (`symbol`,`name`,`nameTH`,`businessDescription`,`sector`,`industry`,`cost`,`quantity`,`lastPrice`,`change`,`percentChange`,`pe`,`pbv`,`roe`,`eps`,`netProfit`,`equity`,`debtToEquity`,`dividendYield`,`dividendDate`,`rsi`,`macdHist`,`signalType`,`signalReason`,`signalDescription`,`lastUpdated`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: StockEntity) {
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
        statement.bindDouble(7, entity.cost)
        statement.bindLong(8, entity.quantity.toLong())
        statement.bindDouble(9, entity.lastPrice)
        statement.bindDouble(10, entity.change)
        statement.bindDouble(11, entity.percentChange)
        val _tmpPe: Double? = entity.pe
        if (_tmpPe == null) {
          statement.bindNull(12)
        } else {
          statement.bindDouble(12, _tmpPe)
        }
        val _tmpPbv: Double? = entity.pbv
        if (_tmpPbv == null) {
          statement.bindNull(13)
        } else {
          statement.bindDouble(13, _tmpPbv)
        }
        val _tmpRoe: Double? = entity.roe
        if (_tmpRoe == null) {
          statement.bindNull(14)
        } else {
          statement.bindDouble(14, _tmpRoe)
        }
        val _tmpEps: Double? = entity.eps
        if (_tmpEps == null) {
          statement.bindNull(15)
        } else {
          statement.bindDouble(15, _tmpEps)
        }
        val _tmpNetProfit: Double? = entity.netProfit
        if (_tmpNetProfit == null) {
          statement.bindNull(16)
        } else {
          statement.bindDouble(16, _tmpNetProfit)
        }
        val _tmpEquity: Double? = entity.equity
        if (_tmpEquity == null) {
          statement.bindNull(17)
        } else {
          statement.bindDouble(17, _tmpEquity)
        }
        val _tmpDebtToEquity: Double? = entity.debtToEquity
        if (_tmpDebtToEquity == null) {
          statement.bindNull(18)
        } else {
          statement.bindDouble(18, _tmpDebtToEquity)
        }
        val _tmpDividendYield: Double? = entity.dividendYield
        if (_tmpDividendYield == null) {
          statement.bindNull(19)
        } else {
          statement.bindDouble(19, _tmpDividendYield)
        }
        val _tmpDividendDate: String? = entity.dividendDate
        if (_tmpDividendDate == null) {
          statement.bindNull(20)
        } else {
          statement.bindText(20, _tmpDividendDate)
        }
        val _tmpRsi: Double? = entity.rsi
        if (_tmpRsi == null) {
          statement.bindNull(21)
        } else {
          statement.bindDouble(21, _tmpRsi)
        }
        val _tmpMacdHist: Double? = entity.macdHist
        if (_tmpMacdHist == null) {
          statement.bindNull(22)
        } else {
          statement.bindDouble(22, _tmpMacdHist)
        }
        val _tmpSignalType: String? = entity.signalType
        if (_tmpSignalType == null) {
          statement.bindNull(23)
        } else {
          statement.bindText(23, _tmpSignalType)
        }
        val _tmpSignalReason: String? = entity.signalReason
        if (_tmpSignalReason == null) {
          statement.bindNull(24)
        } else {
          statement.bindText(24, _tmpSignalReason)
        }
        val _tmpSignalDescription: String? = entity.signalDescription
        if (_tmpSignalDescription == null) {
          statement.bindNull(25)
        } else {
          statement.bindText(25, _tmpSignalDescription)
        }
        val _tmpLastUpdated: String? = entity.lastUpdated
        if (_tmpLastUpdated == null) {
          statement.bindNull(26)
        } else {
          statement.bindText(26, _tmpLastUpdated)
        }
      }
    }
    this.__deleteAdapterOfStockEntity = object : EntityDeleteOrUpdateAdapter<StockEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `stocks` WHERE `symbol` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: StockEntity) {
        statement.bindText(1, entity.symbol)
      }
    }
  }

  public override suspend fun insertStock(stock: StockEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfStockEntity.insert(_connection, stock)
  }

  public override suspend fun deleteStock(stock: StockEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfStockEntity.handle(_connection, stock)
  }

  public override fun getAllStocks(): Flow<List<StockEntity>> {
    val _sql: String = "SELECT * FROM stocks"
    return createFlow(__db, false, arrayOf("stocks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfNameTH: Int = getColumnIndexOrThrow(_stmt, "nameTH")
        val _columnIndexOfBusinessDescription: Int = getColumnIndexOrThrow(_stmt, "businessDescription")
        val _columnIndexOfSector: Int = getColumnIndexOrThrow(_stmt, "sector")
        val _columnIndexOfIndustry: Int = getColumnIndexOrThrow(_stmt, "industry")
        val _columnIndexOfCost: Int = getColumnIndexOrThrow(_stmt, "cost")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
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
        val _columnIndexOfRsi: Int = getColumnIndexOrThrow(_stmt, "rsi")
        val _columnIndexOfMacdHist: Int = getColumnIndexOrThrow(_stmt, "macdHist")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signalType")
        val _columnIndexOfSignalReason: Int = getColumnIndexOrThrow(_stmt, "signalReason")
        val _columnIndexOfSignalDescription: Int = getColumnIndexOrThrow(_stmt, "signalDescription")
        val _columnIndexOfLastUpdated: Int = getColumnIndexOrThrow(_stmt, "lastUpdated")
        val _result: MutableList<StockEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: StockEntity
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
          val _tmpCost: Double
          _tmpCost = _stmt.getDouble(_columnIndexOfCost)
          val _tmpQuantity: Int
          _tmpQuantity = _stmt.getLong(_columnIndexOfQuantity).toInt()
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
          _item = StockEntity(_tmpSymbol,_tmpName,_tmpNameTH,_tmpBusinessDescription,_tmpSector,_tmpIndustry,_tmpCost,_tmpQuantity,_tmpLastPrice,_tmpChange,_tmpPercentChange,_tmpPe,_tmpPbv,_tmpRoe,_tmpEps,_tmpNetProfit,_tmpEquity,_tmpDebtToEquity,_tmpDividendYield,_tmpDividendDate,_tmpRsi,_tmpMacdHist,_tmpSignalType,_tmpSignalReason,_tmpSignalDescription,_tmpLastUpdated)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getStockBySymbol(symbol: String): StockEntity? {
    val _sql: String = "SELECT * FROM stocks WHERE symbol = ?"
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
        val _columnIndexOfCost: Int = getColumnIndexOrThrow(_stmt, "cost")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
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
        val _columnIndexOfRsi: Int = getColumnIndexOrThrow(_stmt, "rsi")
        val _columnIndexOfMacdHist: Int = getColumnIndexOrThrow(_stmt, "macdHist")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signalType")
        val _columnIndexOfSignalReason: Int = getColumnIndexOrThrow(_stmt, "signalReason")
        val _columnIndexOfSignalDescription: Int = getColumnIndexOrThrow(_stmt, "signalDescription")
        val _columnIndexOfLastUpdated: Int = getColumnIndexOrThrow(_stmt, "lastUpdated")
        val _result: StockEntity?
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
          val _tmpCost: Double
          _tmpCost = _stmt.getDouble(_columnIndexOfCost)
          val _tmpQuantity: Int
          _tmpQuantity = _stmt.getLong(_columnIndexOfQuantity).toInt()
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
          _result = StockEntity(_tmpSymbol,_tmpName,_tmpNameTH,_tmpBusinessDescription,_tmpSector,_tmpIndustry,_tmpCost,_tmpQuantity,_tmpLastPrice,_tmpChange,_tmpPercentChange,_tmpPe,_tmpPbv,_tmpRoe,_tmpEps,_tmpNetProfit,_tmpEquity,_tmpDebtToEquity,_tmpDividendYield,_tmpDividendDate,_tmpRsi,_tmpMacdHist,_tmpSignalType,_tmpSignalReason,_tmpSignalDescription,_tmpLastUpdated)
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
    val _sql: String = "DELETE FROM stocks WHERE quantity = 0"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
