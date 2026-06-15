package apincer.mobile.tradings.`data`

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
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class CashDao_Impl(
  __db: RoomDatabase,
) : CashDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCashEntity: EntityInsertAdapter<CashEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfCashEntity = object : EntityInsertAdapter<CashEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `cash` (`id`,`balance`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CashEntity) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindDouble(2, entity.balance)
      }
    }
  }

  public override suspend fun updateCash(cash: CashEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfCashEntity.insert(_connection, cash)
  }

  public override fun getCash(): Flow<CashEntity?> {
    val _sql: String = "SELECT * FROM cash WHERE id = 1"
    return createFlow(__db, false, arrayOf("cash")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBalance: Int = getColumnIndexOrThrow(_stmt, "balance")
        val _result: CashEntity?
        if (_stmt.step()) {
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpBalance: Double
          _tmpBalance = _stmt.getDouble(_columnIndexOfBalance)
          _result = CashEntity(_tmpId,_tmpBalance)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getCashSync(): CashEntity? {
    val _sql: String = "SELECT * FROM cash WHERE id = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBalance: Int = getColumnIndexOrThrow(_stmt, "balance")
        val _result: CashEntity?
        if (_stmt.step()) {
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpBalance: Double
          _tmpBalance = _stmt.getDouble(_columnIndexOfBalance)
          _result = CashEntity(_tmpId,_tmpBalance)
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
