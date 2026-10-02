package apincer.mobile.tradings

import android.app.Application
import kotlinx.coroutines.launch
import apincer.mobile.tradings.data.StockDatabase
import apincer.mobile.tradings.data.StockRepository

/**
 * Application-level singleton for shared infrastructure.
 * Provides a single StockRepository instance shared across all ViewModels,
 * avoiding redundant DAO and database handle creation.
 */
class TradingMateApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Newest SET holiday / SET50 lists: cached copy now, refreshed in the background.
        apincer.mobile.tradings.util.MarketListsSync.loadCached(this)
        Thread { apincer.mobile.tradings.util.MarketListsSync.refreshIfStale(this) }.start()
        // Keep the core-fund list (TDEX plus an optional second ETF) in step with Settings.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            apincer.mobile.tradings.data.PreferenceRepository(this@TradingMateApp).coreMix.collect { (symbol, pct) ->
                apincer.mobile.tradings.domain.CoreSatellite.configure(symbol, pct)
            }
        }
    }

    private var _database: apincer.mobile.tradings.data.StockDatabase? = null
    private var _repository: apincer.mobile.tradings.data.StockRepository? = null

    val repository: apincer.mobile.tradings.data.StockRepository
        get() {
            val db = apincer.mobile.tradings.data.StockDatabase.getDatabase(this)
            if (_database !== db) {
                _database = db
                _repository = apincer.mobile.tradings.data.StockRepository(
                    database = db,
                    stockDao = db.stockDao(),
                    tradeDao = db.tradeDao(),
                    cashDao = db.cashDao(),
                    focusDao = db.focusDao(),
                    checklistDao = db.checklistDao(),
                    dividendDao = db.dividendDao(),
                    portfolioSnapshotDao = db.portfolioSnapshotDao(),
                    cashTransactionDao = db.cashTransactionDao()
                )
            }
            return _repository!!
        }
}

/** Convenience extension so ViewModels can access the shared repo via [application]. */
val Application.appRepository: StockRepository
    get() = (this as TradingMateApp).repository
