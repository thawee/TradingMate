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
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class FocusDao_Impl(
  __db: RoomDatabase,
) : FocusDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfFocusEntity: EntityInsertAdapter<FocusEntity>

  private val __deleteAdapterOfFocusEntity: EntityDeleteOrUpdateAdapter<FocusEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfFocusEntity = object : EntityInsertAdapter<FocusEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `focus_list` (`symbol`,`startPrice`,`addedAtMillis`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: FocusEntity) {
        statement.bindText(1, entity.symbol)
        statement.bindDouble(2, entity.startPrice)
        statement.bindLong(3, entity.addedAtMillis)
      }
    }
    this.__deleteAdapterOfFocusEntity = object : EntityDeleteOrUpdateAdapter<FocusEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `focus_list` WHERE `symbol` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: FocusEntity) {
        statement.bindText(1, entity.symbol)
      }
    }
  }

  public override suspend fun insertFocusStock(focusStock: FocusEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfFocusEntity.insert(_connection, focusStock)
  }

  public override suspend fun deleteFocusStock(focusStock: FocusEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfFocusEntity.handle(_connection, focusStock)
  }

  public override fun getAllFocusStocks(): Flow<List<FocusEntity>> {
    val _sql: String = "SELECT * FROM focus_list ORDER BY addedAtMillis DESC"
    return createFlow(__db, false, arrayOf("focus_list")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfStartPrice: Int = getColumnIndexOrThrow(_stmt, "startPrice")
        val _columnIndexOfAddedAtMillis: Int = getColumnIndexOrThrow(_stmt, "addedAtMillis")
        val _result: MutableList<FocusEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: FocusEntity
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpStartPrice: Double
          _tmpStartPrice = _stmt.getDouble(_columnIndexOfStartPrice)
          val _tmpAddedAtMillis: Long
          _tmpAddedAtMillis = _stmt.getLong(_columnIndexOfAddedAtMillis)
          _item = FocusEntity(_tmpSymbol,_tmpStartPrice,_tmpAddedAtMillis)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getFocusStockBySymbol(symbol: String): FocusEntity? {
    val _sql: String = "SELECT * FROM focus_list WHERE symbol = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, symbol)
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfStartPrice: Int = getColumnIndexOrThrow(_stmt, "startPrice")
        val _columnIndexOfAddedAtMillis: Int = getColumnIndexOrThrow(_stmt, "addedAtMillis")
        val _result: FocusEntity?
        if (_stmt.step()) {
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpStartPrice: Double
          _tmpStartPrice = _stmt.getDouble(_columnIndexOfStartPrice)
          val _tmpAddedAtMillis: Long
          _tmpAddedAtMillis = _stmt.getLong(_columnIndexOfAddedAtMillis)
          _result = FocusEntity(_tmpSymbol,_tmpStartPrice,_tmpAddedAtMillis)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
