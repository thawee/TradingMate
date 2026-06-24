package apincer.mobile.tradings.`data`

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
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
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TradeDao_Impl(
  __db: RoomDatabase,
) : TradeDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTradeEntity: EntityInsertAdapter<TradeEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfTradeEntity = object : EntityInsertAdapter<TradeEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `trade_history` (`id`,`symbol`,`buyPrice`,`sellPrice`,`quantity`,`netProfitPercent`,`netProfitBaht`,`dateMillis`,`note`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TradeEntity) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.symbol)
        statement.bindDouble(3, entity.buyPrice)
        statement.bindDouble(4, entity.sellPrice)
        statement.bindLong(5, entity.quantity.toLong())
        statement.bindDouble(6, entity.netProfitPercent)
        statement.bindDouble(7, entity.netProfitBaht)
        statement.bindLong(8, entity.dateMillis)
        statement.bindText(9, entity.note)
      }
    }
  }

  public override suspend fun insertTrade(trade: TradeEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTradeEntity.insert(_connection, trade)
  }

  public override suspend fun insertTrades(trades: List<TradeEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTradeEntity.insert(_connection, trades)
  }

  public override fun getAllTrades(): Flow<List<TradeEntity>> {
    val _sql: String = "SELECT * FROM trade_history ORDER BY dateMillis DESC"
    return createFlow(__db, false, arrayOf("trade_history")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfBuyPrice: Int = getColumnIndexOrThrow(_stmt, "buyPrice")
        val _columnIndexOfSellPrice: Int = getColumnIndexOrThrow(_stmt, "sellPrice")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfNetProfitPercent: Int = getColumnIndexOrThrow(_stmt, "netProfitPercent")
        val _columnIndexOfNetProfitBaht: Int = getColumnIndexOrThrow(_stmt, "netProfitBaht")
        val _columnIndexOfDateMillis: Int = getColumnIndexOrThrow(_stmt, "dateMillis")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _result: MutableList<TradeEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TradeEntity
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpBuyPrice: Double
          _tmpBuyPrice = _stmt.getDouble(_columnIndexOfBuyPrice)
          val _tmpSellPrice: Double
          _tmpSellPrice = _stmt.getDouble(_columnIndexOfSellPrice)
          val _tmpQuantity: Int
          _tmpQuantity = _stmt.getLong(_columnIndexOfQuantity).toInt()
          val _tmpNetProfitPercent: Double
          _tmpNetProfitPercent = _stmt.getDouble(_columnIndexOfNetProfitPercent)
          val _tmpNetProfitBaht: Double
          _tmpNetProfitBaht = _stmt.getDouble(_columnIndexOfNetProfitBaht)
          val _tmpDateMillis: Long
          _tmpDateMillis = _stmt.getLong(_columnIndexOfDateMillis)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          _item = TradeEntity(_tmpId,_tmpSymbol,_tmpBuyPrice,_tmpSellPrice,_tmpQuantity,_tmpNetProfitPercent,_tmpNetProfitBaht,_tmpDateMillis,_tmpNote)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllTradesSync(): List<TradeEntity> {
    val _sql: String = "SELECT * FROM trade_history ORDER BY dateMillis DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfBuyPrice: Int = getColumnIndexOrThrow(_stmt, "buyPrice")
        val _columnIndexOfSellPrice: Int = getColumnIndexOrThrow(_stmt, "sellPrice")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfNetProfitPercent: Int = getColumnIndexOrThrow(_stmt, "netProfitPercent")
        val _columnIndexOfNetProfitBaht: Int = getColumnIndexOrThrow(_stmt, "netProfitBaht")
        val _columnIndexOfDateMillis: Int = getColumnIndexOrThrow(_stmt, "dateMillis")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _result: MutableList<TradeEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TradeEntity
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpBuyPrice: Double
          _tmpBuyPrice = _stmt.getDouble(_columnIndexOfBuyPrice)
          val _tmpSellPrice: Double
          _tmpSellPrice = _stmt.getDouble(_columnIndexOfSellPrice)
          val _tmpQuantity: Int
          _tmpQuantity = _stmt.getLong(_columnIndexOfQuantity).toInt()
          val _tmpNetProfitPercent: Double
          _tmpNetProfitPercent = _stmt.getDouble(_columnIndexOfNetProfitPercent)
          val _tmpNetProfitBaht: Double
          _tmpNetProfitBaht = _stmt.getDouble(_columnIndexOfNetProfitBaht)
          val _tmpDateMillis: Long
          _tmpDateMillis = _stmt.getLong(_columnIndexOfDateMillis)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          _item = TradeEntity(_tmpId,_tmpSymbol,_tmpBuyPrice,_tmpSellPrice,_tmpQuantity,_tmpNetProfitPercent,_tmpNetProfitBaht,_tmpDateMillis,_tmpNote)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearHistory() {
    val _sql: String = "DELETE FROM trade_history"
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
