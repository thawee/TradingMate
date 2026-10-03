package apincer.mobile.tradings.ui

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import apincer.mobile.tradings.appRepository
import apincer.mobile.tradings.data.ChecklistEntity
import apincer.mobile.tradings.data.ScrapedStockInfo
import apincer.mobile.tradings.data.SetScraper
import apincer.mobile.tradings.data.SimplePortfolio
import apincer.mobile.tradings.data.StockAggregate
import apincer.mobile.tradings.data.TradingBackup
import apincer.mobile.tradings.domain.BollingerBands
import apincer.mobile.tradings.domain.IndicatorSignal
import apincer.mobile.tradings.domain.TechnicalAnalysis
import apincer.mobile.tradings.domain.TradingConstants
import apincer.mobile.tradings.domain.TradeSignal
import apincer.mobile.tradings.domain.TradingZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class AlertRoutineState(
    val playbookMode: PlaybookMode = PlaybookMode.DIVIDEND,
    val swingSellAlerts: List<SellAlertData> = emptyList(),
    val dividendSellAlerts: List<SellAlertData> = emptyList(),
    val combinedSwingPlays: List<StockWatchlistInfo> = emptyList(),
    val speculativePlays: List<StockWatchlistInfo> = emptyList(),
    val dividendPlays: List<StockWatchlistInfo> = emptyList(),
    val portfolioItems: List<StockWatchlistInfo> = emptyList(),
    val checklist: ChecklistEntity = ChecklistEntity(),
    val marketRegime: TechnicalAnalysis.MarketRegime = TechnicalAnalysis.MarketRegime.NEUTRAL
) {
    val activeAlerts: List<SellAlertData>
        get() = if (playbookMode == PlaybookMode.SWING) swingSellAlerts else dividendSellAlerts

    val activeCandidatesCount: Int
        get() = if (playbookMode == PlaybookMode.SWING) combinedSwingPlays.size else dividendPlays.size

    val exitAlertsCount: Int
        get() = activeAlerts.size

    val step1Done: Boolean
        get() = checklist.swingDailyDone

    val step2Done: Boolean
        get() = checklist.swingWeeklyDone

    val step3Done: Boolean
        get() = checklist.swingAiDone
}

sealed class StockUiState {
    object Initial : StockUiState()
    object Loading : StockUiState()
    data class Success(
        val stockInfo: ScrapedStockInfo,
        val portfolio: StockAggregate?,
        val sma50: Double?,
        val sma200: Double?,
        val bb: BollingerBands?,
        val isVolumeSurge: Boolean,
        val rsi: Double?,
        val macd: Triple<Double?, Double?, Double?>,
        val signal: TradeSignal,
        val netProfitPercent: Double? = null,
        val returns: Map<Int, Double> = emptyMap(),
        val zone: TradingZone,
        val buyingPrice: Double? = null,
        val sellingPrice: Double? = null,
        val isFocused: Boolean = false,
        val focusStartPrice: Double = 0.0,
        val focusTargetPrice: Double = 0.0,
        val historicalPrices: List<Double> = emptyList()
    ) : StockUiState()
    data class Error(val message: String, val symbol: String) : StockUiState()
}

data class StockWatchlistInfo(
    val info: ScrapedStockInfo,
    val portfolio: StockAggregate,
    val netProfitPercent: Double = 0.0,
    val signal: TradeSignal? = null,
    val isFocused: Boolean = false,
    val focusStartPrice: Double? = null,
    val focusTargetPrice: Double? = null,
    val focusMovementPercent: Double? = null,
    val buyPriceTarget: Double? = null,
    val sellPriceTarget: Double? = null
)

data class StockFocusInfo(
    val symbol: String,
    val startPrice: Double,
    val targetPrice: Double,
    val currentPrice: Double,
    val movementPercent: Double,
    val addedAtMillis: Long,
    val info: ScrapedStockInfo? = null
)

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository get() = getApplication<apincer.mobile.tradings.TradingMateApp>().repository
    private val preferenceRepository = apincer.mobile.tradings.data.PreferenceRepository(application)
    private val alertPrefs = application.getSharedPreferences("trading_mate_alerts", android.content.Context.MODE_PRIVATE)

    val isAtsEnabled: StateFlow<Boolean> = preferenceRepository.isAtsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val cashBalance: StateFlow<Double> = repository.cashBalance
        .map { it?.balance ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    private val _isAfternoonScanAvailable = MutableStateFlow(getAfternoonScanAvailable())
    val isAfternoonScanAvailable: StateFlow<Boolean> = _isAfternoonScanAvailable

    private fun getAfternoonScanAvailable(): Boolean {
        val tz = java.util.TimeZone.getTimeZone("Asia/Bangkok")
        val now = java.util.Calendar.getInstance(tz)
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(now.time)
        return alertPrefs.getBoolean("afternoon_scan_available_$todayStr", false)
    }

    fun refreshAfternoonScanFlag() {
        _isAfternoonScanAvailable.value = getAfternoonScanAvailable()
    }

    fun clearAfternoonScanFlag() {
        val tz = java.util.TimeZone.getTimeZone("Asia/Bangkok")
        val now = java.util.Calendar.getInstance(tz)
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(now.time)
        alertPrefs.edit().remove("afternoon_scan_available_$todayStr").apply()
        _isAfternoonScanAvailable.value = false
    }


    fun exportBackup(
        contentResolver: android.content.ContentResolver,
        uri: android.net.Uri,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stocks = repository.getAllStocksSync()
                val cashBalanceVal = repository.getCashSync()?.balance ?: 0.0
                val tradeHistory = repository.getAllTradesSync()
                val adviceEvents = repository.getAllAdviceEventsSync()
                val cashTransactions = repository.getAllCashTransactionsSync()
                val dividendHistory = repository.getAllDividendsSync()
                val portfolioSnapshots = repository.getAllSnapshotsSync()
                
                val watchlistSymbols = stocks.map { it.portfolio.symbol }
                val portfolioItems = stocks
                    .filter { it.portfolio.quantity > 0 }
                    .map {
                        SimplePortfolio(
                            symbol = it.portfolio.symbol,
                            cost = it.portfolio.cost,
                            quantity = it.portfolio.quantity,
                            tradePurpose = it.portfolio.tradePurpose,
                            buyFees = it.portfolio.buyFees,
                            stopLoss = it.portfolio.stopLoss,
                            targetPrice = it.portfolio.targetPrice,
                            plannedEntryPrice = it.portfolio.plannedEntryPrice,
                            planId = it.portfolio.planId,
                            planVersion = it.portfolio.planVersion,
                            planCreatedAtMillis = it.portfolio.planCreatedAtMillis,
                            planSource = it.portfolio.planSource,
                            exitPolicy = it.portfolio.exitPolicy,
                            playbookNote = it.portfolio.playbookNote,
                            peakPrice = it.portfolio.peakPrice
                        )
                    }
                
                val backup = TradingBackup(
                    watchlistSymbols = watchlistSymbols,
                    portfolioItems = portfolioItems,
                    cashBalance = cashBalanceVal,
                    tradeHistory = tradeHistory,
                    adviceEvents = adviceEvents,
                    cashTransactions = cashTransactions,
                    dividendHistory = dividendHistory,
                    portfolioSnapshots = portfolioSnapshots
                )
                
                val jsonString = Json.encodeToString(TradingBackup.serializer(), backup)
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                } ?: throw java.io.IOException("Failed to open output stream")
                
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    fun importBackup(
        contentResolver: android.content.ContentResolver,
        uri: android.net.Uri,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes().decodeToString()
                } ?: throw java.io.IOException("Failed to open input stream")
                
                val backup = Json.decodeFromString(TradingBackup.serializer(), jsonString)
                repository.restoreBackup(backup)
                
                // Trigger refresh to update UI State Flows
                refreshWatchlistInfo()
                
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    private val _uiState = MutableStateFlow<StockUiState>(StockUiState.Initial)
    val uiState: StateFlow<StockUiState> = _uiState

    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError

    fun clearRefreshError() {
        _refreshError.value = null
    }

    private val _searchResults = MutableStateFlow<List<ScrapedStockInfo>>(emptyList())
    val searchResults: StateFlow<List<ScrapedStockInfo>> = _searchResults

    val watchlistInfo: StateFlow<List<StockWatchlistInfo>> = 
        combine(repository.allStocks, repository.allFocusStocks, isAtsEnabled) { stocks, focusStocks, atsEnabled ->
            stocks.map { stock ->
                val focus = focusStocks.find { it.symbol == stock.symbol }
                val info = stock.toScrapedStockInfo()
                val netProfit = if (stock.cost > 0 && stock.quantity > 0) {
                    TechnicalAnalysis.calculatePositionNetProfitPercent(stock.cost, stock.lastPrice,
                        stock.quantity, stock.buyFees, atsEnabled)
                } else 0.0

                val rawSignal = if (stock.rsi != null && stock.macdHist != null && stock.lastPrice > 0) {
                    TechnicalAnalysis.getDetailedSignal(
                        rsi = stock.rsi,
                        macdHist = stock.macdHist,
                        lastPrice = stock.lastPrice,
                        sma50 = stock.sma50,
                        sma200 = stock.sma200,
                        bb = stock.bb,
                        isVolumeSurge = stock.isVolumeSurge,
                        obvRising = stock.obvRising,
                        atrPercent = stock.atr?.takeIf { stock.lastPrice > 0 }?.let { it / stock.lastPrice * 100 },
                        adx = stock.adx,
                        stochK = stock.stochK,
                        stochD = stock.stochD,
                        mfi = stock.mfi,
                        userCost = if (stock.cost > 0) stock.cost else null,
                        userQuantity = if (stock.quantity > 0) stock.quantity else null,
                        isFundamentalGood = false,
                        tradePurpose = stock.tradePurpose,
                        dividendYield = stock.dividendYield,
                        roe = stock.roe,
                        peakPrice = if (stock.peakPrice > 0) stock.peakPrice else null,
                        isSet50 = apincer.mobile.tradings.domain.MarketLists.isSet50(stock.symbol.uppercase()),
                        // stopLoss is stored as a positive baht PRICE; convert to the negative
                        // percent-vs-cost that getDetailedSignal expects for the override
                        userStopLoss = if (stock.stopLoss > 0 && stock.cost > 0 && stock.stopLoss < stock.cost) {
                            ((stock.stopLoss - stock.cost) / stock.cost) * 100
                        } else null,
                        relativeStrength = stock.relativeStrength,
                        nvdrNetVolume = stock.nvdrNetVolume,
                        nvdrNetValue = stock.nvdrNetValue,
                        isNearXdDate = TechnicalAnalysis.isNearExDividendDate(info.dividendDate ?: stock.dividendDate),
                        isWeeklyTrendBullish = stock.weeklyTrendBullish,
                        userBuyFees = stock.buyFees,
                        atsEnabled = atsEnabled
                    )
                } else if (stock.signalType != null) {
                    TradeSignal(
                        type = runCatching { IndicatorSignal.valueOf(stock.signalType!!) }.getOrDefault(IndicatorSignal.NEUTRAL),
                        reason = stock.signalReason ?: "",
                        description = stock.signalDescription ?: ""
                    )
                } else null
                val signal = apincer.mobile.tradings.domain.HoldingSignal.resolve(stock.symbol, stock.quantity,
                    stock.tradePurpose, stock.stopLoss, stock.lastPrice, stock.portfolio.exitPolicy,
                    { stock.portfolio.toTradePlan() }, rawSignal)

                val focusMovement = if (focus != null && focus.startPrice != 0.0) {
                    ((stock.lastPrice - focus.startPrice) / focus.startPrice) * 100
                } else null

                StockWatchlistInfo(
                    info = info, 
                    portfolio = stock, 
                    netProfitPercent = netProfit, 
                    signal = signal,
                    isFocused = focus != null,
                    focusStartPrice = focus?.startPrice,
                    focusTargetPrice = focus?.targetPrice,
                    focusMovementPercent = focusMovement
                )
            }.sortedWith(
                compareByDescending<StockWatchlistInfo> { it.portfolio.quantity > 0 }
                    .thenByDescending { if (it.portfolio.quantity > 0) it.netProfitPercent else 0.0 }
                    .thenBy { it.info.symbol }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val focusListInfo: StateFlow<List<StockFocusInfo>> = 
        combine(repository.allFocusStocks, repository.allStocks) { focusStocks, stocks ->
            focusStocks.map { focus ->
                val stock = stocks.find { it.symbol == focus.symbol }
                val currentPrice = stock?.lastPrice ?: focus.startPrice
                val movement = if (focus.startPrice != 0.0) ((currentPrice - focus.startPrice) / focus.startPrice) * 100 else 0.0

                StockFocusInfo(
                    symbol = focus.symbol,
                    startPrice = focus.startPrice,
                    targetPrice = focus.targetPrice,
                    currentPrice = currentPrice,
                    movementPercent = movement,
                    addedAtMillis = focus.addedAtMillis,
                    info = stock?.toScrapedStockInfo()
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing



    private val _checklist = MutableStateFlow(ChecklistEntity())
    val checklist: StateFlow<ChecklistEntity> = _checklist

    // Dividend first: the only tested rule that beat TDEX lives there; Swing is technical context.
    private val _playbookMode = MutableStateFlow(PlaybookMode.DIVIDEND)
    val playbookMode: StateFlow<PlaybookMode> = _playbookMode

    private val _marketRegime = MutableStateFlow(TechnicalAnalysis.MarketRegime.NEUTRAL)
    val marketRegime: StateFlow<TechnicalAnalysis.MarketRegime> = _marketRegime

    // Dividend cut/suspension reasons for Dividend-purpose holdings (symbol -> reason), refreshed once a day.
    private val _dividendCuts = MutableStateFlow<Map<String, String>>(emptyMap())
    private var dividendCutsCheckedFor: Pair<String, Set<String>>? = null

    /** Fetches three years of dividend events for [symbols] at most once a day per symbol set. */
    fun refreshDividendCuts(symbols: Set<String>) {
        val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok"))
        val key = today.toString() to symbols
        if (symbols.isEmpty() || dividendCutsCheckedFor == key) return
        dividendCutsCheckedFor = key
        viewModelScope.launch(Dispatchers.IO) {
            _dividendCuts.value = symbols.mapNotNull { symbol ->
                apincer.mobile.tradings.domain.DividendCut.check(SetScraper.fetchDividendEvents(symbol, "3y"), today)
                    ?.let { symbol to it }
            }.toMap()
        }
    }

    val alertRoutineState: StateFlow<AlertRoutineState> = 
        combine(_playbookMode, watchlistInfo, _checklist, _marketRegime, _dividendCuts) { mode, watchlist, checklist, marketRegime, dividendCuts ->
            val portfolioItems = watchlist.filter { it.portfolio.quantity > 0 }

            val isQual = StockDna::isQual
            val isVal = StockDna::isVal
            val isDiv = StockDna::isDiv
            val isMom = StockDna::isMom
            val isSup = StockDna::isSup
            val isGapUp = StockDna::isGapUp
            val isLiquid = StockDna::preFilter // liquidity + 52-week-low trap gate

            val dividendPlays = watchlist.filter(StockDna::isDividendCandidate)
                .sortedWith(
                    compareBy<StockWatchlistInfo> {
                        when (it.signal?.type) {
                            IndicatorSignal.BUY -> 0
                            IndicatorSignal.POTENTIAL -> 1
                            IndicatorSignal.NEUTRAL -> 2
                            else -> 3
                        }
                    }.thenByDescending {
                        it.info.dividendYield ?: 0.0
                    }
                )

            val isMarketBearish = marketRegime == TechnicalAnalysis.MarketRegime.BEARISH

            val swingPlays = watchlist.filter { isLiquid(it) && isQual(it) && StockDna.isSwingCandidate(it, isMarketBearish) }
                .sortedWith(
                    compareBy<StockWatchlistInfo> {
                        when (it.signal?.type) {
                            IndicatorSignal.BUY -> 0
                            IndicatorSignal.POTENTIAL -> 1
                            else -> 2
                        }
                    }.thenBy {
                        it.portfolio.rsi ?: 100.0
                    }.thenByDescending {
                        isQual(it)
                    }
                )

            val gapPlays = watchlist.filter {
                isLiquid(it) && isGapUp(it) && StockDna.isSwingCandidate(it, isMarketBearish)
            }.sortedByDescending { it.info.percentChange }
            // Speculative Watch: liquid but Quality-failing stocks with a live BUY/POTENTIAL
            // signal. Includes early/unconfirmed setups (MACD histogram not yet positive) —
            // these are sorted after MACD-confirmed ones since they carry extra risk.
            val speculativePlays = watchlist.filter {
                isLiquid(it) && !isQual(it) && isSup(it)
            }.sortedWith(
                compareBy<StockWatchlistInfo> {
                    when (it.signal?.type) {
                        IndicatorSignal.BUY -> 0
                        IndicatorSignal.POTENTIAL -> 1
                        else -> 2
                    }
                }.thenByDescending {
                    // MACD-confirmed momentum ranks above unconfirmed (macdHist <= 0)
                    (it.portfolio.macdHist ?: 0.0) > 0.0
                }
            )
            val combinedSwingPlays = (swingPlays + gapPlays).distinctBy { it.info.symbol }
                .sortedWith(
                    compareBy<StockWatchlistInfo> {
                        when (it.signal?.type) {
                            IndicatorSignal.BUY -> 0
                            IndicatorSignal.POTENTIAL -> 1
                            else -> 2
                        }
                    }.thenByDescending {
                        it.info.percentChange
                    }.thenBy {
                        it.portfolio.rsi ?: 100.0
                    }
                )

            val swingSellAlerts = mutableListOf<SellAlertData>()
            val dividendSellAlerts = mutableListOf<SellAlertData>()

            portfolioItems.forEach { stock ->
                val tradePurpose = stock.portfolio.tradePurpose

                var applySwingLogic = true

                if (tradePurpose == "DIVIDEND") {
                    // Tested high-yield rule: exits are a saved stop or leaving the ranked list; the rest are review notes.
                    applySwingLogic = false
                    val fixedPlan = stock.portfolio.portfolio.exitPolicy == "FIXED_TARGET"
                    val ranked = _highYieldList.value?.second?.map { it.symbol.uppercase() }?.toSet()
                    apincer.mobile.tradings.domain.DividendExitPolicy.notes(
                        price = stock.info.lastPrice, costPerShare = stock.portfolio.cost,
                        savedStop = if (fixedPlan) 0.0 else stock.portfolio.stopLoss,
                        yieldPercent = stock.info.dividendYield, roe = stock.info.roe,
                        inHighYieldList = ranked?.let { stock.info.symbol.uppercase() in it },
                        dividendCut = dividendCuts[stock.info.symbol.uppercase()]
                    ).takeIf { it.isNotEmpty() }?.let { notes ->
                        // One alert per holding (alerts are de-duplicated by symbol): exits first, then reviews.
                        dividendSellAlerts.add(SellAlertData(stock, notes.joinToString("\n") { it.reason }))
                    }
                }

                if (stock.portfolio.portfolio.exitPolicy == "FIXED_TARGET") {
                        val decision = apincer.mobile.tradings.domain.ExitPolicyEvaluator.evaluate(stock.portfolio.portfolio.toTradePlan(), stock.info.lastPrice)
                        if (decision != null) {
                            val alerts = if (tradePurpose == "DIVIDEND") dividendSellAlerts else swingSellAlerts
                            alerts.add(SellAlertData(stock, decision.description))
                        }
                } else if (applySwingLogic) {
                    val targetAlerts = swingSellAlerts
                    
                    val currentPrice = stock.info.lastPrice
                    val explicitStopLoss = stock.portfolio.stopLoss

                    // Take-profit and overbought exits come from the R-based signal (target = 2R,
                    // overbought exits only after +1R), so they are not re-derived here.
                    // The stop the user saved comes first, so Advisor and the Portfolio card name the same exit.
                    if (explicitStopLoss > 0 && currentPrice <= explicitStopLoss) {
                        targetAlerts.add(SellAlertData(stock, "Saved stop ฿${String.format(java.util.Locale.ENGLISH, "%.2f", explicitStopLoss)} reached"))
                    } else if (stock.signal?.type == IndicatorSignal.SELL) {
                        // Engine exits, including the volatility trailing stop once a trade has gained 1R.
                        targetAlerts.add(SellAlertData(stock, stock.signal.reason))
                    }
                }
            }


            // Fix 5: Deduplicate sell alerts by symbol — a stock with FIXED_TARGET policy
            // could fire both the ExitPolicyEvaluator path and the swing logic path, producing
            // duplicate alerts for the same position. Keep only the first (highest-priority) entry.
            AlertRoutineState(
                playbookMode = mode,
                swingSellAlerts = swingSellAlerts.distinctBy { it.stock.info.symbol },
                dividendSellAlerts = dividendSellAlerts.distinctBy { it.stock.info.symbol },
                combinedSwingPlays = combinedSwingPlays,
                speculativePlays = speculativePlays,
                dividendPlays = dividendPlays,
                portfolioItems = portfolioItems,
                checklist = checklist,
                marketRegime = marketRegime
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AlertRoutineState()
        )

    fun setPlaybookMode(mode: PlaybookMode) {
        _playbookMode.value = mode
    }

    fun toggleAlertRoutineStep(step: Int) {
        val current = alertRoutineState.value
        when (step) {
            1 -> updateChecklistState { it.copy(swingDailyDone = !current.checklist.swingDailyDone) }
            2 -> updateChecklistState { it.copy(swingWeeklyDone = !current.checklist.swingWeeklyDone) }
            3 -> updateChecklistState { it.copy(swingAiDone = !current.checklist.swingAiDone) }
        }
    }

    fun markAlertRoutineStepDone(step: Int) {
        when (step) {
            1 -> updateChecklistState { it.copy(swingDailyDone = true) }
            2 -> updateChecklistState { it.copy(swingWeeklyDone = true) }
            3 -> updateChecklistState { it.copy(swingAiDone = true) }
        }
    }

    fun updateChecklistState(update: (ChecklistEntity) -> ChecklistEntity) {
        viewModelScope.launch {
            val current = _checklist.value
            val next = update(current)
            repository.updateChecklist(next)
        }
    }

    private fun checkAndResetChecklist(existing: ChecklistEntity): ChecklistEntity {
        val zoneId = java.time.ZoneId.of("Asia/Bangkok")
        val now = java.time.ZonedDateTime.now(zoneId)
        // Treat the trading day as starting at 16:30 (market close)
        val disciplineDateTime = if (now.toLocalTime().isBefore(java.time.LocalTime.of(16, 30))) {
            now.minusDays(1)
        } else {
            now
        }
        val todayStr = disciplineDateTime.toLocalDate().toString()
        // ISO week string (year + week number) for weekly boundary detection
        val weekStr = disciplineDateTime.toLocalDate().let {
            val week = it.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            val year = it.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)
            "$year-W$week"
        }
        val lastWeekStr = existing.lastResetDate?.let { dateStr ->
            runCatching {
                val date = java.time.LocalDate.parse(dateStr)
                val week = date.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                val year = date.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)
                "$year-W$week"
            }.getOrNull()
        }

        var updated = existing.copy(lastResetDate = todayStr)

        if (existing.lastResetDate != todayStr) {
            // Always reset daily items when date changes
            updated = updated.copy(
                swingDailyDone = false,
                swingAiDone = false
            )
            // Only reset weekly item when the ISO week changes
            if (lastWeekStr != weekStr) {
                updated = updated.copy(swingWeeklyDone = false)
            }
        }
        return updated
    }

    init {
        // Observe checklist and handle resets
        viewModelScope.launch {
            repository.checklist.collect { entity ->
                val currentChecklist = entity ?: ChecklistEntity()
                val targetChecklist = checkAndResetChecklist(currentChecklist)
                if (targetChecklist != currentChecklist || entity == null) {
                    repository.updateChecklist(targetChecklist)
                }
                _checklist.value = targetChecklist
            }
        }
        // Fetch SET index regime in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val setHistory = SetScraper.fetchSetIndexHistory()
                _marketRegime.value = TechnicalAnalysis.getMarketRegime(setHistory.map { it.close })
            } catch (e: Exception) {
                android.util.Log.w("StockViewModel", "Failed to fetch initial SET Index regime", e)
            }
        }
        // Trigger background refresh on start
        refreshWatchlistInfo()
        // Refresh afternoon scan flag
        refreshAfternoonScanFlag()
    }

    fun refreshWatchlistInfo() {
        viewModelScope.launch {
            if (!_isRefreshing.compareAndSet(false, true)) return@launch
            _refreshError.value = null
            try {
                val isMarketOpen = TechnicalAnalysis.getMarketStatus() != apincer.mobile.tradings.domain.MarketStatus.CLOSED
                val cachedStocks = repository.getAllStocksSync()
                if (!isMarketOpen && cachedStocks.isNotEmpty() && cachedStocks.all {
                    !isCacheExpired(it.lastUpdated) && !isCacheExpired(it.cache?.fundamentalsUpdatedAt) &&
                        !needsTechnicalRefresh(it)
                }) {
                    android.util.Log.d("StockViewModel", "Market is closed and data is up-to-date. Skipping refresh.")
                    return@launch
                }

                val stocks = cachedStocks
                if (stocks.isEmpty()) return@launch
                val failedSymbols = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

                // 1. Ultra-Fast Batch Update (Prices, Changes, basic ratios)
                withContext(Dispatchers.IO) {
                    try {
                        val setHistory = SetScraper.fetchSetIndexHistory()
                        _marketRegime.value = TechnicalAnalysis.getMarketRegime(setHistory.map { it.close })
                    } catch (e: Exception) {
                        android.util.Log.w("StockViewModel", "Failed to update SET Index regime in refresh", e)
                    }

                    val batchResults = try {
                        SetScraper.fetchBatchQuotes(stocks.map { it.symbol })
                    } catch (e: Exception) {
                        android.util.Log.e("StockViewModel", "Error fetching batch quotes", e)
                        emptyList()
                    }
                    failedSymbols.addAll(stocks.map { it.symbol }.filter { symbol ->
                        batchResults.none { it.symbol.equals(symbol, ignoreCase = true) }
                    })
                    batchResults.forEach { updated ->
                        stocks.find { it.symbol == updated.symbol }?.let { original ->
                            if (!updated.lastPrice.isFinite() || updated.lastPrice <= 0.0) {
                                failedSymbols.add(original.symbol)
                                return@let
                            }
                            val cache = original.cache ?: apincer.mobile.tradings.data.StockCacheEntity(original.symbol)
                            repository.updateStockCache(cache.copy(
                                name = updated.name ?: cache.name,
                                lastPrice = updated.lastPrice,
                                change = updated.change,
                                percentChange = updated.percentChange,
                                pe = updated.pe ?: cache.pe,
                                pbv = updated.pbv ?: cache.pbv,
                                dividendYield = updated.dividendYield ?: cache.dividendYield,
                                volume = updated.volume ?: cache.volume,
                                lastUpdated = updated.lastUpdated
                            ))
                            
                            // Update peak price for trailing stop loss
                            if (original.portfolio.quantity > 0 && updated.lastPrice > original.portfolio.peakPrice) {
                                repository.updatePortfolio(original.portfolio.copy(peakPrice = updated.lastPrice))
                            }
                        }
                    }
                }

                // 2. Deep Update (Technical Analysis & Fundamentals from SET)
                val semaphore = Semaphore(3) 
                withContext(Dispatchers.IO) {
                    val jobs = stocks.map { stock ->
                        async {
                            semaphore.withPermit {
                                try {
                                    val latestStock = repository.getStockBySymbol(stock.symbol) ?: stock
                                    val needsDeepFetch = latestStock.roe == null || latestStock.debtToEquity == null || latestStock.sector == null || isCacheExpired(latestStock.cache?.fundamentalsUpdatedAt)
                                    val needsIndicators = needsTechnicalRefresh(latestStock)

                                    if (needsDeepFetch || needsIndicators) {
                                        val info = if (needsDeepFetch) {
                                            SetScraper.fetchStockInfo(stock.symbol)
                                        } else {
                                            latestStock.toScrapedStockInfo()
                                        }
                                        if (!info.lastPrice.isFinite() || info.lastPrice <= 0.0) {
                                            throw IllegalStateException("No valid price returned")
                                        }

                                        val indicators = if (needsIndicators) {
                                            SetScraper.fetchTechnicalIndicators(stock.symbol)
                                        } else {
                                            apincer.mobile.tradings.domain.Indicators(
                                                sma50 = null,
                                                sma200 = null,
                                                rsi = latestStock.rsi,
                                                macd = null,
                                                signal = null,
                                                histogram = latestStock.macdHist,
                                                bollingerBands = null,
                                                isVolumeSurge = false
                                            )
                                        }
                                        if (needsIndicators && (indicators.rsi == null || indicators.histogram == null)) {
                                            throw IllegalStateException("No valid technical history returned")
                                        }

                                        val signal = if (needsIndicators) {
                                            TechnicalAnalysis.getDetailedSignal(
                                                rsi = indicators.rsi, 
                                                macdHist = indicators.histogram, 
                                                lastPrice = info.lastPrice, 
                                                sma50 = indicators.sma50,
                                                sma200 = indicators.sma200,
                                                bb = indicators.bollingerBands,
                                                isVolumeSurge = indicators.isVolumeSurge,
                                                obvRising = indicators.obvRising,
                                                atrPercent = indicators.atr?.takeIf { info.lastPrice > 0 }?.let { it / info.lastPrice * 100 },
                                                adx = indicators.adx,
                                                stochK = indicators.stochK,
                                                stochD = indicators.stochD,
                                                mfi = indicators.mfi,
                                                userCost = if (stock.cost > 0) stock.cost else null,
                                                userQuantity = if (stock.quantity > 0) stock.quantity else null,
                                                isFundamentalGood = info.isFundamentalGood,
                                                tradePurpose = stock.tradePurpose,
                                                dividendYield = info.dividendYield,
                                                roe = info.roe,
                                                isWeeklyTrendBullish = indicators.weeklyTrendBullish,
                                                userBuyFees = latestStock.buyFees,
                                                atsEnabled = isAtsEnabled.value
                                            )
                                        } else {
                                            TradeSignal(
                                                type = runCatching { IndicatorSignal.valueOf(latestStock.signalType ?: "NEUTRAL") }.getOrDefault(IndicatorSignal.NEUTRAL),
                                                reason = latestStock.signalReason ?: "",
                                                description = latestStock.signalDescription ?: ""
                                            )
                                        }

                                        val cache = latestStock.cache ?: apincer.mobile.tradings.data.StockCacheEntity(latestStock.symbol)
                                        repository.updateStockCache(
                                            cache.copy(
                                                name = info.name ?: cache.name,
                                                businessDescription = info.businessDescription ?: cache.businessDescription,
                                                sector = info.sector ?: cache.sector,
                                                industry = info.industry ?: cache.industry,
                                                lastPrice = info.lastPrice,
                                                change = info.change,
                                                percentChange = info.percentChange,
                                                pe = info.pe ?: cache.pe,
                                                pbv = info.pbv ?: cache.pbv,
                                                roe = info.roe,
                                                eps = info.eps,
                                                netProfit = info.netProfit,
                                                netProfitMargin = info.netProfitMargin ?: cache.netProfitMargin,
                                                profitGrowth3Y = if (needsDeepFetch) info.profitGrowth3Y else cache.profitGrowth3Y,
                                                fundamentalsUpdatedAt = if (needsDeepFetch) info.lastUpdated else cache.fundamentalsUpdatedAt,
                                                equity = info.equity,
                                                debtToEquity = info.debtToEquity,
                                                dividendYield = info.dividendYield ?: cache.dividendYield,
                                                dividendDate = info.dividendDate,
                                                dividendPerShare = if (info.dividendYield != null && info.lastPrice != 0.0) {
                                                    info.lastPrice * (info.dividendYield / 100.0)
                                                } else cache.dividendPerShare,
                                                volume = info.volume ?: cache.volume,
                                                lastUpdated = info.lastUpdated.takeIf { it.isNotBlank() } ?: cache.lastUpdated
                                            )
                                        )

                                        val sig = latestStock.signal ?: apincer.mobile.tradings.data.StockSignalEntity(latestStock.symbol)
                                        repository.updateStockSignal(
                                            sig.copy(
                                                rsi = if (needsIndicators) indicators.rsi else sig.rsi,
                                                macdHist = if (needsIndicators) indicators.histogram else sig.macdHist,
                                                sma50 = if (needsIndicators) indicators.sma50 else sig.sma50,
                                                sma200 = if (needsIndicators) indicators.sma200 else sig.sma200,
                                                bbUpper = if (needsIndicators) indicators.bollingerBands?.upper else sig.bbUpper,
                                                bbMiddle = if (needsIndicators) indicators.bollingerBands?.middle else sig.bbMiddle,
                                                bbLower = if (needsIndicators) indicators.bollingerBands?.lower else sig.bbLower,
                                                isVolumeSurge = if (needsIndicators) indicators.isVolumeSurge else sig.isVolumeSurge,
                                                obvRising = if (needsIndicators) indicators.obvRising else sig.obvRising,
                                                week52Low = if (needsIndicators) indicators.week52Low else sig.week52Low,
                                                weeklyTrendBullish = if (needsIndicators) indicators.weeklyTrendBullish else sig.weeklyTrendBullish,
                                                observationDate = if (needsIndicators) indicators.observationDate else sig.observationDate,
                                                benchmarkDate = if (needsIndicators) indicators.benchmarkDate else sig.benchmarkDate,
                                                week52High = if (needsIndicators) indicators.week52High else sig.week52High,
                                                relativeStrength = if (needsIndicators) indicators.relativeStrength else sig.relativeStrength,
                                                atr = if (needsIndicators) indicators.atr else sig.atr,
                                                adx = if (needsIndicators) indicators.adx else sig.adx,
                                                stochK = if (needsIndicators) indicators.stochK else sig.stochK,
                                                stochD = if (needsIndicators) indicators.stochD else sig.stochD,
                                                mfi = if (needsIndicators) indicators.mfi else sig.mfi,
                                                nvdrNetVolume = info.nvdrNetVolume ?: sig.nvdrNetVolume,
                                                nvdrNetValue = info.nvdrNetValue ?: sig.nvdrNetValue,
                                                signalType = signal.type.name,
                                                signalReason = signal.reason,
                                                signalDescription = signal.description,
                                                lastUpdated = if (needsIndicators && indicators.rsi != null && indicators.histogram != null)
                                                    info.lastUpdated.takeIf { it.isNotBlank() } ?: sig.lastUpdated else sig.lastUpdated
                                            )
                                        )

                                        // Update peak price for trailing stop loss
                                        if (latestStock.portfolio.quantity > 0 && info.lastPrice > latestStock.portfolio.peakPrice) {
                                            repository.updatePortfolio(latestStock.portfolio.copy(peakPrice = info.lastPrice))
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("StockViewModel", "Error deep refreshing stock ${stock.symbol}", e)
                                    failedSymbols.add(stock.symbol)
                                }
                            }
                        }
                    }
                    jobs.awaitAll()
                }
                if (failedSymbols.isNotEmpty()) {
                    _refreshError.value = "Some quotes or indicators could not be refreshed: ${failedSymbols.sorted().joinToString()}. Check the last-sync time."
                }
            } catch (e: Exception) {
                android.util.Log.e("StockViewModel", "Error refreshing watchlist info", e)
                _refreshError.value = e.localizedMessage ?: "Failed to refresh watchlist"
            } finally {
                _isRefreshing.value = false
                apincer.mobile.tradings.widget.notifyWidgetDataChanged(getApplication())
            }
        }
    }

    fun refreshPortfolioOnly() {
        viewModelScope.launch {
            if (!_isRefreshing.compareAndSet(false, true)) return@launch
            _refreshError.value = null
            try {
                val isMarketOpen = TechnicalAnalysis.getMarketStatus() != apincer.mobile.tradings.domain.MarketStatus.CLOSED
                val portfolioStocks = repository.getAllStocksSync()
                    .filter { it.portfolio.quantity > 0 }
                if (portfolioStocks.isEmpty()) return@launch
                if (!isMarketOpen && portfolioStocks.all {
                    !isCacheExpired(it.lastUpdated) && !isCacheExpired(it.cache?.fundamentalsUpdatedAt) &&
                        !needsTechnicalRefresh(it)
                }) return@launch
                val failedSymbols = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

                withContext(Dispatchers.IO) {
                    val batchResults = try {
                        SetScraper.fetchBatchQuotes(portfolioStocks.map { it.symbol })
                    } catch (e: Exception) {
                        emptyList()
                    }
                    failedSymbols.addAll(portfolioStocks.map { it.symbol }.filter { symbol ->
                        batchResults.none { it.symbol.equals(symbol, ignoreCase = true) }
                    })
                    batchResults.forEach { updated ->
                        portfolioStocks.find { it.symbol == updated.symbol }?.let { original ->
                            if (!updated.lastPrice.isFinite() || updated.lastPrice <= 0.0) {
                                failedSymbols.add(original.symbol)
                                return@let
                            }
                            val cache = original.cache ?: apincer.mobile.tradings.data.StockCacheEntity(original.symbol)
                            repository.updateStockCache(cache.copy(
                                name = updated.name ?: cache.name,
                                lastPrice = updated.lastPrice,
                                change = updated.change,
                                percentChange = updated.percentChange,
                                pe = updated.pe ?: cache.pe,
                                pbv = updated.pbv ?: cache.pbv,
                                dividendYield = updated.dividendYield ?: cache.dividendYield,
                                dividendPerShare = if (updated.dividendYield != null && updated.lastPrice != 0.0) {
                                    updated.lastPrice * (updated.dividendYield / 100.0)
                                } else cache.dividendPerShare,
                                volume = updated.volume ?: cache.volume,
                                lastUpdated = updated.lastUpdated
                            ))
                        }
                    }

                    val semaphore = Semaphore(3)
                    val jobs = portfolioStocks.map { stock ->
                        async {
                            semaphore.withPermit {
                                try {
                                    val latestStock = repository.getStockBySymbol(stock.symbol) ?: stock
                                    val needsDeepFetch = latestStock.roe == null || latestStock.debtToEquity == null || latestStock.sector == null || isCacheExpired(latestStock.cache?.fundamentalsUpdatedAt)
                                    val needsIndicators = needsTechnicalRefresh(latestStock)

                                    if (needsDeepFetch || needsIndicators) {
                                        val info = if (needsDeepFetch) {
                                            SetScraper.fetchStockInfo(stock.symbol)
                                        } else {
                                            latestStock.toScrapedStockInfo()
                                        }
                                        if (!info.lastPrice.isFinite() || info.lastPrice <= 0.0) {
                                            throw IllegalStateException("No valid price returned")
                                        }
                                        val indicators = if (needsIndicators) {
                                            SetScraper.fetchTechnicalIndicators(stock.symbol)
                                        } else {
                                            apincer.mobile.tradings.domain.Indicators(
                                                sma50 = null, sma200 = null, rsi = latestStock.rsi,
                                                macd = null, signal = null, histogram = latestStock.macdHist,
                                                bollingerBands = null, isVolumeSurge = false
                                            )
                                        }
                                        if (needsIndicators && (indicators.rsi == null || indicators.histogram == null)) {
                                            throw IllegalStateException("No valid technical history returned")
                                        }
                                        val signal = if (needsIndicators) {
                                            TechnicalAnalysis.getDetailedSignal(
                                                rsi = indicators.rsi, macdHist = indicators.histogram,
                                                lastPrice = info.lastPrice, sma50 = indicators.sma50,
                                                sma200 = indicators.sma200, bb = indicators.bollingerBands,
                                                isVolumeSurge = indicators.isVolumeSurge,
                                                obvRising = indicators.obvRising,
                                                atrPercent = indicators.atr?.takeIf { info.lastPrice > 0 }?.let { it / info.lastPrice * 100 },
                                                adx = indicators.adx,
                                                stochK = indicators.stochK,
                                                stochD = indicators.stochD,
                                                mfi = indicators.mfi,
                                                userCost = if (stock.cost > 0) stock.cost else null,
                                                userQuantity = if (stock.quantity > 0) stock.quantity else null,
                                                isFundamentalGood = info.isFundamentalGood,
                                                tradePurpose = stock.tradePurpose,
                                                dividendYield = info.dividendYield, roe = info.roe,
                                                isWeeklyTrendBullish = indicators.weeklyTrendBullish,
                                                userBuyFees = latestStock.buyFees,
                                                atsEnabled = isAtsEnabled.value
                                            )
                                        } else {
                                            TradeSignal(
                                                type = runCatching { IndicatorSignal.valueOf(latestStock.signalType ?: "NEUTRAL") }.getOrDefault(IndicatorSignal.NEUTRAL),
                                                reason = latestStock.signalReason ?: "",
                                                description = latestStock.signalDescription ?: ""
                                            )
                                        }
                                        val cache = latestStock.cache ?: apincer.mobile.tradings.data.StockCacheEntity(latestStock.symbol)
                                        repository.updateStockCache(cache.copy(
                                            name = info.name ?: cache.name, sector = info.sector ?: cache.sector,
                                            industry = info.industry ?: cache.industry, lastPrice = info.lastPrice,
                                            change = info.change, percentChange = info.percentChange,
                                            pe = info.pe ?: cache.pe, pbv = info.pbv ?: cache.pbv,
                                            roe = info.roe, eps = info.eps, netProfit = info.netProfit,
                                            netProfitMargin = info.netProfitMargin ?: cache.netProfitMargin,
                                            profitGrowth3Y = if (needsDeepFetch) info.profitGrowth3Y else cache.profitGrowth3Y,
                                            fundamentalsUpdatedAt = if (needsDeepFetch) info.lastUpdated else cache.fundamentalsUpdatedAt,
                                            equity = info.equity, debtToEquity = info.debtToEquity,
                                            dividendYield = info.dividendYield ?: cache.dividendYield,
                                            dividendDate = info.dividendDate,
                                            dividendPerShare = if (info.dividendYield != null && info.lastPrice != 0.0) {
                                                info.lastPrice * (info.dividendYield / 100.0)
                                            } else cache.dividendPerShare,
                                            volume = info.volume ?: cache.volume,
                                            lastUpdated = info.lastUpdated.takeIf { it.isNotBlank() } ?: cache.lastUpdated
                                        ))
                                        val sig = latestStock.signal ?: apincer.mobile.tradings.data.StockSignalEntity(latestStock.symbol)
                                        repository.updateStockSignal(sig.copy(
                                            rsi = if (needsIndicators) indicators.rsi else sig.rsi,
                                            macdHist = if (needsIndicators) indicators.histogram else sig.macdHist,
                                            sma50 = if (needsIndicators) indicators.sma50 else sig.sma50,
                                            sma200 = if (needsIndicators) indicators.sma200 else sig.sma200,
                                            bbUpper = if (needsIndicators) indicators.bollingerBands?.upper else sig.bbUpper,
                                            bbMiddle = if (needsIndicators) indicators.bollingerBands?.middle else sig.bbMiddle,
                                            bbLower = if (needsIndicators) indicators.bollingerBands?.lower else sig.bbLower,
                                            isVolumeSurge = if (needsIndicators) indicators.isVolumeSurge else sig.isVolumeSurge,
                                            obvRising = if (needsIndicators) indicators.obvRising else sig.obvRising,
                                            week52Low = if (needsIndicators) indicators.week52Low else sig.week52Low,
                                            weeklyTrendBullish = if (needsIndicators) indicators.weeklyTrendBullish else sig.weeklyTrendBullish,
                                            observationDate = if (needsIndicators) indicators.observationDate else sig.observationDate,
                                            benchmarkDate = if (needsIndicators) indicators.benchmarkDate else sig.benchmarkDate,
                                            week52High = if (needsIndicators) indicators.week52High else sig.week52High,
                                            relativeStrength = if (needsIndicators) indicators.relativeStrength else sig.relativeStrength,
                                            atr = if (needsIndicators) indicators.atr else sig.atr,
                                            adx = if (needsIndicators) indicators.adx else sig.adx,
                                            stochK = if (needsIndicators) indicators.stochK else sig.stochK,
                                            stochD = if (needsIndicators) indicators.stochD else sig.stochD,
                                            mfi = if (needsIndicators) indicators.mfi else sig.mfi,
                                            nvdrNetVolume = info.nvdrNetVolume ?: sig.nvdrNetVolume,
                                            nvdrNetValue = info.nvdrNetValue ?: sig.nvdrNetValue,
                                            signalType = signal.type.name, signalReason = signal.reason,
                                            signalDescription = signal.description,
                                            lastUpdated = if (needsIndicators && indicators.rsi != null && indicators.histogram != null)
                                                info.lastUpdated.takeIf { it.isNotBlank() } ?: sig.lastUpdated else sig.lastUpdated
                                        ))
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("StockViewModel", "Deep refresh failed for ${stock.symbol}: ${e.message}")
                                    failedSymbols.add(stock.symbol)
                                }
                            }
                        }
                    }
                    jobs.awaitAll()
                }
                if (failedSymbols.isNotEmpty()) {
                    _refreshError.value = "Some quotes or indicators could not be refreshed: ${failedSymbols.sorted().joinToString()}. Check the last-sync time."
                }
            } catch (e: Exception) {
                _refreshError.value = e.localizedMessage ?: "Failed to refresh portfolio"
            } finally {
                _isRefreshing.value = false
                apincer.mobile.tradings.widget.notifyWidgetDataChanged(getApplication())
            }
        }
    }

    private fun calculateManualPercent(info: ScrapedStockInfo): ScrapedStockInfo {
        return if (info.percentChange == 0.0 && info.lastPrice != 0.0) {
            val prevClose = info.lastPrice - info.change
            val percent = if (prevClose != 0.0) (info.change / prevClose) * 100 else 0.0
            info.copy(percentChange = percent)
        } else info
    }

    fun addToWatchlist(
        symbol: String,
        cost: Double = 0.0,
        quantity: Int = 0,
        tradePurpose: String = "SWING",
        stopLoss: Double = 0.0,
        playbookNote: String = "",
        isEdit: Boolean = false,
        targetPrice: Double = 0.0,
        recordExecutedFill: Boolean = false,
        onResult: ((Result<Unit>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val fees = if (quantity > 0 && !isEdit) {
                TechnicalAnalysis.calculateFees(cost * quantity, false, isAtsEnabled.value)
            } else 0.0
            try {
                if (isEdit) {
                    // Edit mode: update portfolio fields only, no cash movement
                    if (quantity == 0) {
                        if ((repository.getStockBySymbol(symbol)?.portfolio?.quantity ?: 0) > 0)
                            throw IllegalStateException("Record a sale before removing a holding")
                        repository.removeStock(symbol)
                    } else {
                        val recorded = repository.getStockBySymbol(symbol)?.portfolio
                        if (recorded != null && quantity != recorded.quantity) {
                            throw IllegalStateException("Use Buy or Sell to change the recorded share count")
                        }
                        repository.addStock(symbol, cost, quantity, tradePurpose, fees, stopLoss,
                            playbookNote, targetPrice = targetPrice, revisePlanForEntry = true)
                    }
                } else {
                    // New buy: deduct cash (guarded by balance check in executeBuy)
                    val riskLimits = apincer.mobile.tradings.domain.TradeRiskLimits(
                        preferenceRepository.maxRiskPerTrade.first(),
                        preferenceRepository.maxPortfolioAllocation.first(),
                        preferenceRepository.maxSectorAllocation.first(),
                        TechnicalAnalysis.getRecommendedCashBufferPercent(_marketRegime.value),
                        preferenceRepository.minRiskRewardRatio.first()
                    )
                    repository.executeBuy(symbol, cost, quantity, tradePurpose, fees, stopLoss,
                        playbookNote, targetPrice, riskLimits, isAtsEnabled.value, recordExecutedFill)
                }
                apincer.mobile.tradings.widget.notifyWidgetDataChanged(getApplication())
                onResult?.invoke(Result.success(Unit))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (onResult != null) onResult(Result.failure(e)) else _refreshError.value = e.message
            }
        }
    }

    fun recordAiRecommendations(
        result: apincer.mobile.tradings.domain.AiAnalysisResult,
        plans: Map<String, apincer.mobile.tradings.domain.AiCandidatePlan>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            result.recommendations.forEach { rec ->
                val plan = plans[rec.symbol] ?: return@forEach
                repository.recordAdviceEvent(apincer.mobile.tradings.data.AdviceEventEntity(
                    symbol = plan.symbol, planId = plan.snapshotId, planVersion = 1,
                    kind = "AI_RANKED", timeMillis = System.currentTimeMillis(),
                    entryPrice = plan.entryPrice, stopPrice = plan.stopPrice,
                    targetPrice = plan.targetPrice, quantity = plan.shares,
                    source = "GEMINI", note = rec.reasoning.take(1000)
                ))
            }
        }
    }

    private val _momentumList = MutableStateFlow(apincer.mobile.tradings.domain.MomentumList.fromJson(alertPrefs.getString(MOMENTUM_LIST_KEY, null)))
    /** (ranking date, top 10) for the 6-month momentum list; ranked at most once a day. */
    val momentumList: StateFlow<Pair<String, List<apincer.mobile.tradings.domain.MomentumList.Entry>>?> = _momentumList
    private val _momentumLoading = MutableStateFlow(false)
    val momentumLoading: StateFlow<Boolean> = _momentumLoading

    /** Ranks current SET50 members by 126-session return from dividend-adjusted daily closes. */
    fun refreshMomentumList(force: Boolean = false) {
        val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok")).toString()
        if (_momentumLoading.value || (!force && _momentumList.value?.first == today)) return
        _momentumLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val closes = apincer.mobile.tradings.domain.MarketLists.set50().associateWith { symbol ->
                    SetScraper.fetchHistoricalPrices(symbol, days = 220, dividendAdjusted = true).map { it.close }
                }
                val ranked = apincer.mobile.tradings.domain.MomentumList.rank(closes)
                if (ranked.isNotEmpty()) {
                    alertPrefs.edit().putString(MOMENTUM_LIST_KEY, apincer.mobile.tradings.domain.MomentumList.toJson(today, ranked)).apply()
                    _momentumList.value = today to ranked
                }
            } finally {
                _momentumLoading.value = false
            }
        }
    }

    private val _highYieldList = MutableStateFlow(apincer.mobile.tradings.domain.MomentumList.fromJson(alertPrefs.getString(HIGH_YIELD_LIST_KEY, null)))
    /** (ranking date, top 10 by dividend yield) for the high dividend yield list; ranked at most once a day. */
    val highYieldList: StateFlow<Pair<String, List<apincer.mobile.tradings.domain.MomentumList.Entry>>?> = _highYieldList
    private val _highYieldLoading = MutableStateFlow(false)
    val highYieldLoading: StateFlow<Boolean> = _highYieldLoading

    fun refreshHighYieldList(force: Boolean = false) {
        val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok")).toString()
        if (_highYieldLoading.value || (!force && _highYieldList.value?.first == today)) return
        _highYieldLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val yields = apincer.mobile.tradings.domain.MarketLists.set50().associateWith { symbol ->
                    runCatching { SetScraper.fetchStockInfo(symbol).dividendYield }.getOrNull()
                }
                val ranked = apincer.mobile.tradings.domain.HighYieldList.rank(yields)
                if (ranked.isNotEmpty()) {
                    alertPrefs.edit().putString(HIGH_YIELD_LIST_KEY, apincer.mobile.tradings.domain.MomentumList.toJson(today, ranked)).apply()
                    _highYieldList.value = today to ranked
                }
            } finally {
                _highYieldLoading.value = false
            }
        }
    }

    private val _stopAcks = MutableStateFlow(readStopAcks())
    /** Stop levels the user chose to hold past, by symbol; sell reminders stay quiet until the stop changes. */
    val stopAcks: StateFlow<Map<String, Double>> = _stopAcks

    private fun readStopAcks(): Map<String, Double> = alertPrefs.all.mapNotNull { (k, v) ->
        if (k.startsWith(STOP_ACK_PREFIX)) (v as? String)?.toDoubleOrNull()?.let { k.removePrefix(STOP_ACK_PREFIX) to it } else null
    }.toMap()

    /**
     * Decision once price is through the saved stop: MOVE sets a new stop below the price, HOLD keeps
     * the position deliberately. Both need a reason and are journaled, so ignored alerts become a record.
     */
    fun recordStopDecision(item: StockWatchlistInfo, move: Boolean, newStopInput: Double?, reason: String,
                           onResult: (Result<String>) -> Unit) {
        val symbol = item.info.symbol.uppercase()
        val p = item.portfolio.portfolio
        viewModelScope.launch {
            try {
                val why = reason.trim()
                if (why.length < 3) throw IllegalArgumentException("Write a short reason for the journal")
                val stamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                if (move) {
                    val newStop = newStopInput?.takeIf { it > 0.0 }?.let { apincer.mobile.tradings.domain.SetTick.ceil(it) }
                        ?: throw IllegalArgumentException("Enter a new stop price")
                    if (newStop >= item.info.lastPrice)
                        throw IllegalArgumentException("The new stop must be below the current price ฿${item.info.lastPrice}")
                    val note = listOf(p.playbookNote, "[$stamp] Stop moved ฿${p.stopLoss} → ฿$newStop: $why")
                        .filter { it.isNotBlank() }.joinToString("\n")
                    repository.addStock(symbol, p.cost, p.quantity, p.tradePurpose, 0.0, newStop, note,
                        targetPrice = p.targetPrice)
                    repository.recordAdviceEvent(apincer.mobile.tradings.data.AdviceEventEntity(
                        symbol = symbol, planId = p.planId, planVersion = p.planVersion, kind = "STOP_MOVED",
                        timeMillis = System.currentTimeMillis(), entryPrice = p.cost, stopPrice = newStop,
                        quantity = 0, source = "USER", note = why))
                    alertPrefs.edit().remove(STOP_ACK_PREFIX + symbol).apply()
                    _stopAcks.value = readStopAcks()
                    onResult(Result.success("Stop moved to ฿$newStop"))
                } else {
                    val note = listOf(p.playbookNote, "[$stamp] Held past stop ฿${p.stopLoss}: $why")
                        .filter { it.isNotBlank() }.joinToString("\n")
                    repository.addStock(symbol, p.cost, p.quantity, p.tradePurpose, 0.0, p.stopLoss, note,
                        targetPrice = p.targetPrice)
                    repository.recordAdviceEvent(apincer.mobile.tradings.data.AdviceEventEntity(
                        symbol = symbol, planId = p.planId, planVersion = p.planVersion, kind = "STOP_HOLD",
                        timeMillis = System.currentTimeMillis(), entryPrice = p.cost, stopPrice = p.stopLoss,
                        quantity = 0, source = "USER", note = why))
                    alertPrefs.edit().putString(STOP_ACK_PREFIX + symbol, p.stopLoss.toString()).apply()
                    _stopAcks.value = readStopAcks()
                    onResult(Result.success("Holding $symbol past its stop; reminders paused for this stop"))
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                onResult(Result.failure(e))
            }
        }
    }

    /**
     * Watch-only add. addToWatchlist(symbol) with no shares went through executeBuy, which rejects
     * quantity 0, so the "+" dialog reported success without adding. The symbol is also checked
     * against a live SET quote so typos (e.g. BANPUU) are not saved.
     */
    fun addSymbolToWatchlist(symbol: String, onResult: (Result<String>) -> Unit) {
        val normalized = symbol.trim().uppercase()
        viewModelScope.launch {
            try {
                if (!normalized.matches(Regex("[A-Z0-9&.\\-]{1,12}")))
                    throw IllegalArgumentException("Enter a SET ticker such as PTT or CPALL")
                if (repository.getStockBySymbol(normalized) != null)
                    throw IllegalStateException("$normalized is already in your watchlist")
                val quote = withContext(Dispatchers.IO) { SetScraper.fetchBatchQuotes(listOf(normalized)) }
                    .firstOrNull { it.symbol.equals(normalized, ignoreCase = true) && it.lastPrice > 0.0 }
                    ?: throw IllegalStateException("No SET quote found for $normalized. Check the ticker or your connection.")
                repository.addStockIfMissing(quote.symbol.uppercase())
                refreshWatchlistInfo()
                onResult(Result.success(normalized))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                onResult(Result.failure(e))
            }
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            try {
                repository.removeStock(symbol)
                apincer.mobile.tradings.widget.notifyWidgetDataChanged(getApplication())
            } catch (e: IllegalStateException) {
                _refreshError.value = e.message
            }
        }
    }



    fun resetToInitial() {
        _uiState.value = StockUiState.Initial
    }





    
    fun acceptAiPlan(rec: apincer.mobile.tradings.domain.AiRecommendation, showSnackbar: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val symbol = rec.symbol.uppercase()
                val targetPrice = rec.targetProfit.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                val stopLoss = rec.stopLoss.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                
                if (targetPrice <= 0.0 || stopLoss <= 0.0) {
                    withContext(Dispatchers.Main) {
                        showSnackbar("AI plan missing valid numerical Target or Stop prices.")
                    }
                    return@launch
                }
                
                val existing = repository.allStocks.first().find { it.portfolio.symbol == symbol }?.portfolio
                val entity = existing?.copy(
                    targetPrice = targetPrice,
                    stopLoss = stopLoss,
                    playbookNote = "[AI Plan] ${rec.playbookType}: ${rec.reasoning.take(150)}...",
                    planSource = "GEMINI",
                    planId = java.util.UUID.randomUUID().toString(),
                    planVersion = 1,
                    planCreatedAtMillis = System.currentTimeMillis(),
                    exitPolicy = "FIXED_TARGET",
                    tradePurpose = if (rec.playbookType.contains("Dividend", ignoreCase = true)) "DIVIDEND" else "SWING"
                ) ?: apincer.mobile.tradings.data.PortfolioEntity(
                    symbol = symbol,
                    targetPrice = targetPrice,
                    stopLoss = stopLoss,
                    playbookNote = "[AI Plan] ${rec.playbookType}: ${rec.reasoning.take(150)}...",
                    planSource = "GEMINI",
                    planId = java.util.UUID.randomUUID().toString(),
                    planVersion = 1,
                    planCreatedAtMillis = System.currentTimeMillis(),
                    exitPolicy = "FIXED_TARGET",
                    tradePurpose = if (rec.playbookType.contains("Dividend", ignoreCase = true)) "DIVIDEND" else "SWING"
                )
                
                repository.updatePortfolio(entity)
                
                repository.recordAdviceEvent(apincer.mobile.tradings.data.AdviceEventEntity(
                    symbol = symbol, planId = entity.planId, planVersion = 1,
                    kind = "AI_ACCEPTED", timeMillis = System.currentTimeMillis(),
                    entryPrice = 0.0, stopPrice = stopLoss,
                    targetPrice = targetPrice, quantity = 0,
                    source = "GEMINI", note = "Accepted AI Plan: ${rec.playbookType}"
                ))
                
                withContext(Dispatchers.Main) {
                    showSnackbar("✅ AI Plan Saved for $symbol!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showSnackbar("Failed to accept AI plan: ${e.message}")
                }
            }
        }
    }

    fun fetchStockData(symbol: String) {
        if (symbol.isBlank()) {
            resetToInitial()
            return
        }

        viewModelScope.launch {
            _uiState.value = StockUiState.Loading
            try {
                val result = withContext(Dispatchers.IO) {
                    // 1. Try cached data first (instant)
                    val local = repository.getStockBySymbol(symbol)
                    val cachedInfo = local?.toScrapedStockInfo()
                    val isCacheFresh = cachedInfo?.lastUpdated?.let {
                        !isCacheExpired(it)
                    } == true && !isCacheExpired(local?.cache?.fundamentalsUpdatedAt)

                    // 2. Only call API if cache is stale
                    val info = if (isCacheFresh && cachedInfo != null) {
                        cachedInfo
                    } else {
                        SetScraper.fetchStockInfo(symbol)
                    }
                    if (!info.lastPrice.isFinite() || info.lastPrice <= 0.0) {
                        throw IllegalStateException("No valid quote available for $symbol. Try again later.")
                    }

                    val updatedInfo = calculateManualPercent(info.copy(
                        name = info.name ?: local?.name,
                        businessDescription = info.businessDescription ?: local?.businessDescription,
                        sector = info.sector ?: local?.sector,
                        industry = info.industry ?: local?.industry
                    ))

                    val history = SetScraper.fetchHistoricalPrices(symbol)
                    val prices = history.map { it.close }
                    val volumes = history.map { it.volume }

                    val sma50 = TechnicalAnalysis.calculateSMA(prices, 50)
                    val sma200 = TechnicalAnalysis.calculateSMA(prices, 200)
                    val bb = TechnicalAnalysis.calculateBollingerBands(prices)
                    val isVolumeSurge = TechnicalAnalysis.isVolumeSurge(volumes)
                    val obvRising = TechnicalAnalysis.isObvRising(prices, volumes)
                    val week52 = TechnicalAnalysis.calculate52WeekRange(prices)
                    val relativeStrength = TechnicalAnalysis.calculateRelativeStrengthOnDates(
                        history.map { it.date to it.close },
                        SetScraper.fetchSetIndexHistory().map { it.date to it.close })
                    val highs = history.map { it.high }
                    val lows = history.map { it.low }
                    val atr = TechnicalAnalysis.calculateATR(highs, lows, prices)
                    val adx = TechnicalAnalysis.calculateADX(highs, lows, prices)
                    val stoch = TechnicalAnalysis.calculateStochastic(highs, lows, prices)
                    val mfi = TechnicalAnalysis.calculateMFI(highs, lows, prices, volumes)

                    // Calculate Returns for different periods (prices are oldest→newest)
                    val returns = mutableMapOf<Int, Double>()
                    val periods = listOf(3, 7, 15, 30)
                    periods.forEach { days ->
                        if (prices.size >= days + 1) {
                            val current = prices.last()
                            val past = prices[prices.size - 1 - days]
                            val ret = ((current - past) / past) * 100
                            returns[days] = Math.round(ret * 100.0) / 100.0
                        }
                    }

                    val rsi = TechnicalAnalysis.calculateRSI(prices, 14)
                    val macd = TechnicalAnalysis.calculateMACD(prices)

                    val portfolio = local?.portfolio
                    val netProfit = if (portfolio != null && portfolio.cost > 0 && portfolio.quantity > 0) {
                        TechnicalAnalysis.calculatePositionNetProfitPercent(
                            portfolio.cost, updatedInfo.lastPrice, portfolio.quantity,
                            portfolio.buyFees, isAtsEnabled.value)
                    } else null

                    val rawSignal = TechnicalAnalysis.getDetailedSignal(
                        rsi = rsi, 
                        macdHist = macd.third, 
                        lastPrice = updatedInfo.lastPrice, 
                        sma50 = sma50,
                        sma200 = sma200,
                        bb = bb,
                        isVolumeSurge = isVolumeSurge,
                        obvRising = obvRising,
                        atrPercent = atr?.takeIf { updatedInfo.lastPrice > 0 }?.let { it / updatedInfo.lastPrice * 100 },
                        adx = adx,
                        stochK = stoch?.first,
                        stochD = stoch?.second,
                        mfi = mfi,
                        userCost = portfolio?.cost,
                        userQuantity = portfolio?.quantity,
                        isFundamentalGood = updatedInfo.isFundamentalGood,
                        tradePurpose = portfolio?.tradePurpose ?: "SWING",
                        dividendYield = updatedInfo.dividendYield,
                        roe = updatedInfo.roe,
                        peakPrice = portfolio?.peakPrice?.takeIf { it > 0.0 },
                        isSet50 = apincer.mobile.tradings.domain.MarketLists.isSet50(symbol.uppercase()),
                        userStopLoss = portfolio?.let { held ->
                            if (held.stopLoss > 0.0 && held.cost > 0.0 && held.stopLoss < held.cost)
                                (held.stopLoss - held.cost) / held.cost * 100.0 else null
                        },
                        relativeStrength = relativeStrength,
                        nvdrNetVolume = updatedInfo.nvdrNetVolume,
                        nvdrNetValue = updatedInfo.nvdrNetValue,
                        isNearXdDate = TechnicalAnalysis.isNearExDividendDate(updatedInfo.dividendDate),
                        isWeeklyTrendBullish = TechnicalAnalysis.isWeeklyTrendBullishOnDate(
                            history.map { it.date to it.close },
                            java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok")).toString(),
                            updatedInfo.lastPrice
                        ),
                        userBuyFees = portfolio?.buyFees,
                        atsEnabled = isAtsEnabled.value
                    )
                    val signal = if (portfolio == null) {
                        if (apincer.mobile.tradings.domain.CoreSatellite.isCore(symbol))
                            apincer.mobile.tradings.domain.CoreSatellite.CORE_SIGNAL else rawSignal
                    } else apincer.mobile.tradings.domain.HoldingSignal.resolve(symbol, portfolio.quantity,
                        portfolio.tradePurpose, portfolio.stopLoss, updatedInfo.lastPrice, portfolio.exitPolicy,
                        { portfolio.toTradePlan() }, rawSignal) ?: rawSignal

                    val zone = TechnicalAnalysis.getTradingZone(rsi, macd.third, updatedInfo.lastPrice, sma50, bb)

                    val buyPriceTarget = TechnicalAnalysis.estimatePriceForRSI(prices, 35.0)
                    val sellPriceTarget = TechnicalAnalysis.estimatePriceForRSI(prices, 65.0)

                    val focusEntry = repository.getFocusStock(symbol)
                    val isFocused = focusEntry != null
                    val focusStart = focusEntry?.startPrice ?: 0.0
                    val focusTarget = focusEntry?.targetPrice ?: 0.0

                    // Update cache for this single stock too
                    if (local != null) {
                        val cache = local.cache ?: apincer.mobile.tradings.data.StockCacheEntity(local.portfolio.symbol)
                        repository.updateStockCache(cache.copy(
                            name = updatedInfo.name,
                            businessDescription = updatedInfo.businessDescription,
                            sector = updatedInfo.sector ?: cache.sector,
                            industry = updatedInfo.industry ?: cache.industry,
                            lastPrice = updatedInfo.lastPrice,
                            change = updatedInfo.change,
                            percentChange = updatedInfo.percentChange,
                            pe = updatedInfo.pe,
                            pbv = updatedInfo.pbv,
                            roe = updatedInfo.roe,
                            debtToEquity = updatedInfo.debtToEquity,
                            dividendYield = updatedInfo.dividendYield,
                            dividendDate = updatedInfo.dividendDate,
                            dividendPerShare = if (updatedInfo.dividendYield != null && updatedInfo.lastPrice != 0.0) {
                                updatedInfo.lastPrice * (updatedInfo.dividendYield / 100.0)
                            } else cache.dividendPerShare,
                            netProfitMargin = updatedInfo.netProfitMargin ?: cache.netProfitMargin,
                            profitGrowth3Y = updatedInfo.profitGrowth3Y,
                            fundamentalsUpdatedAt = if (!isCacheFresh) updatedInfo.lastUpdated else cache.fundamentalsUpdatedAt,
                            volume = updatedInfo.volume ?: cache.volume,
                            lastUpdated = updatedInfo.lastUpdated
                        ))
                        val sig = local.signal ?: apincer.mobile.tradings.data.StockSignalEntity(local.portfolio.symbol)
                        repository.updateStockSignal(sig.copy(
                            rsi = rsi,
                            macdHist = macd.third,
                            sma50 = sma50,
                            sma200 = sma200,
                            bbUpper = bb?.upper,
                            bbMiddle = bb?.middle,
                            bbLower = bb?.lower,
                            isVolumeSurge = isVolumeSurge,
                            obvRising = obvRising,
                            week52Low = week52?.first,
                            weeklyTrendBullish = TechnicalAnalysis.isWeeklyTrendBullishOnDate(
                                history.map { it.date to it.close },
                                java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok")).toString(),
                                updatedInfo.lastPrice
                            ),
                            observationDate = history.lastOrNull()?.date,
                            benchmarkDate = SetScraper.fetchSetIndexHistory().lastOrNull()?.date,
                            week52High = week52?.second,
                            relativeStrength = relativeStrength,
                            atr = atr,
                            adx = adx,
                            stochK = stoch?.first,
                            stochD = stoch?.second,
                            mfi = mfi,
                            nvdrNetVolume = updatedInfo.nvdrNetVolume,
                            nvdrNetValue = updatedInfo.nvdrNetValue,
                            signalType = signal.type.name,
                            signalReason = signal.reason,
                            signalDescription = signal.description,
                            lastUpdated = updatedInfo.lastUpdated
                        ))
                    }

                    StockUiState.Success(
                        updatedInfo, 
                        local, 
                        sma50, 
                        sma200,
                        bb,
                        isVolumeSurge,
                        rsi, 
                        macd, 
                        signal, 
                        netProfit, 
                        returns, 
                        zone,
                        buyPriceTarget,
                        sellPriceTarget,
                        isFocused,
                        focusStart,
                        focusTarget,
                        prices.reversed()
                    )
                }
                _uiState.value = result
            } catch (e: Exception) {
                _uiState.value = StockUiState.Error(e.message ?: "Failed to scrape data", symbol)
            }
        }
    }

    private val cacheDateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private fun isCacheExpired(lastUpdated: String?): Boolean {
        if (lastUpdated.isNullOrBlank()) return true
        return try {
            val dateTime = java.time.LocalDateTime.parse(lastUpdated, cacheDateTimeFormatter)
            val diffMs = System.currentTimeMillis() - java.time.ZoneId.systemDefault().let { dateTime.atZone(it).toInstant().toEpochMilli() }
            diffMs > 24L * 60L * 60L * 1000L // 24 Hours
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Indicators must be recomputed when expired or incomplete. A missing observation or
     * benchmark date blocks every swing candidate ("Price or SET benchmark history missing"),
     * so it forces a refresh even inside the cache window.
     */
    private fun needsTechnicalRefresh(stock: apincer.mobile.tradings.data.StockAggregate): Boolean =
        stock.rsi == null || stock.macdHist == null ||
            stock.observationDate == null || stock.benchmarkDate == null ||
            // Mismatched sessions fail the freshness gate; retry rather than wait out the cache.
            stock.observationDate != stock.benchmarkDate ||
            isTechnicalCacheExpired(stock.signal?.lastUpdated)

    private fun isTechnicalCacheExpired(lastUpdated: String?): Boolean {
        if (lastUpdated.isNullOrBlank()) return true
        return try {
            val dateTime = java.time.LocalDateTime.parse(lastUpdated, cacheDateTimeFormatter)
            val diffMs = System.currentTimeMillis() - java.time.ZoneId.systemDefault().let { dateTime.atZone(it).toInstant().toEpochMilli() }
            val isMarketClosed = TechnicalAnalysis.getMarketStatus() == apincer.mobile.tradings.domain.MarketStatus.CLOSED
            if (isMarketClosed) {
                diffMs > 12L * 60L * 60L * 1000L // 12 Hours
            } else {
                diffMs > 15L * 60L * 1000L // 15 Minutes
            }
        } catch (e: Exception) {
            true
        }
    }
}

/** SharedPreferences key prefix ("trading_mate_alerts") for a stop level the user chose to hold past. */
const val STOP_ACK_PREFIX = "stop_ack_"

private const val MOMENTUM_LIST_KEY = "momentum_list_cache"
private const val HIGH_YIELD_LIST_KEY = "high_yield_list_cache"
