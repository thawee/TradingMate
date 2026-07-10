package apincer.mobile.tradings

import android.app.Application
import apincer.mobile.tradings.data.StockDatabase
import apincer.mobile.tradings.data.StockRepository

/**
 * Application-level singleton for shared infrastructure.
 * Provides a single StockRepository instance shared across all ViewModels,
 * avoiding redundant DAO and database handle creation.
 */
class TradingMateApp : Application() {

    val database by lazy { StockDatabase.getDatabase(this) }

    val repository by lazy {
        StockRepository(
            database = database,
            stockDao = database.stockDao(),
            tradeDao = database.tradeDao(),
            cashDao = database.cashDao(),
            focusDao = database.focusDao(),
            checklistDao = database.checklistDao(),
            dividendDao = database.dividendDao(),
            portfolioSnapshotDao = database.portfolioSnapshotDao(),
            cashTransactionDao = database.cashTransactionDao()
        )
    }
}

/** Convenience extension so ViewModels can access the shared repo via [application]. */
val Application.appRepository: StockRepository
    get() = (this as TradingMateApp).repository
