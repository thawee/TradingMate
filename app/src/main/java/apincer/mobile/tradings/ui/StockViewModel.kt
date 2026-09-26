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
    val playbookMode: PlaybookMode = PlaybookMode.SWING,
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
    private val repository = application.appRepository
    private val preferenceRepository = apincer.mobile.tradings.data.PreferenceRepository(application)
    private val alertPrefs = application.getSharedPreferences("trading_mate_alerts", android.content.Context.MODE_PRIVATE)

    val isAtsEnabled: StateFlow<Boolean> = preferenceRepository.isAtsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val trailingStopPercent: StateFlow<Double> = preferenceRepository.trailingStopPercent
        .stateIn(viewModelScope, SharingStarted.Eagerly, 5.0)

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
                        isSet50 = TradingConstants.SET50_SYMBOLS.contains(stock.symbol.uppercase()),
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
                val explicitStopHit = stock.quantity > 0 && stock.stopLoss > 0.0 &&
                    stock.lastPrice > 0.0 && stock.lastPrice <= stock.stopLoss
                val signal = if (explicitStopHit) {
                    TradeSignal(IndicatorSignal.SELL, "STOP", "Saved stop reached at ฿${stock.stopLoss}")
                } else if (stock.quantity > 0 && stock.portfolio.exitPolicy == "FIXED_TARGET") {
                    val decision = apincer.mobile.tradings.domain.ExitPolicyEvaluator.evaluate(
                        stock.portfolio.toTradePlan(), stock.lastPrice,
                        stock.macdHist, stock.sma50,
                        TechnicalAnalysis.isNearExDividendDate(info.dividendDate ?: stock.dividendDate)
                    )
                    if (decision != null) TradeSignal(IndicatorSignal.SELL,
                        decision.reason.name, decision.description)
                    else TradeSignal(IndicatorSignal.NEUTRAL, "Saved plan active",
                        "No saved exit level or early invalidation trigger reached")
                } else rawSignal

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

    private val _playbookMode = MutableStateFlow(PlaybookMode.SWING)
    val playbookMode: StateFlow<PlaybookMode> = _playbookMode

    private val _marketRegime = MutableStateFlow(TechnicalAnalysis.MarketRegime.NEUTRAL)
    val marketRegime: StateFlow<TechnicalAnalysis.MarketRegime> = _marketRegime

    val alertRoutineState: StateFlow<AlertRoutineState> = 
        combine(_playbookMode, watchlistInfo, _checklist, trailingStopPercent, _marketRegime) { mode, watchlist, checklist, tsPercent, marketRegime ->
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
                    if (stock.portfolio.stopLoss > 0.0 && stock.info.lastPrice > 0.0 &&
                        stock.info.lastPrice <= stock.portfolio.stopLoss &&
                        stock.portfolio.portfolio.exitPolicy != "FIXED_TARGET") {
                        dividendSellAlerts.add(SellAlertData(stock,
                            "Saved stop reached at ฿${stock.portfolio.stopLoss}"))
                    }
                    val yield = stock.info.dividendYield
                    val roe = stock.info.roe

                    if (roe != null && roe < 15.0) {
                        dividendSellAlerts.add(SellAlertData(stock, "Fundamentals Break (ROE < 15%)"))
                    }

                    if (yield == null || yield >= TradingConstants.DIVIDEND_YIELD_PROTECTION) {
                        applySwingLogic = false
                        // Fix #3: Deep drawdown guardrail even for protected dividend stocks
                        val drawdown = if (stock.portfolio.cost > 0)
                            ((stock.info.lastPrice - stock.portfolio.cost) / stock.portfolio.cost) * 100
                            else 0.0
                        if (drawdown <= TradingConstants.DIVIDEND_DEEP_DRAWDOWN_PERCENT) {
                            dividendSellAlerts.add(SellAlertData(stock, "⚠️ Deep Drawdown (${String.format(java.util.Locale.ENGLISH, "%.1f", drawdown)}%) — Review Hold Thesis"))
                        }
                    } else {
                        swingSellAlerts.add(SellAlertData(stock, "Yield Dropped (< 3%) (Transition to Swing)"))
                        applySwingLogic = true
                    }
                }

                if (stock.portfolio.portfolio.exitPolicy == "FIXED_TARGET") {
                        val decision = apincer.mobile.tradings.domain.ExitPolicyEvaluator.evaluate(
                            stock.portfolio.portfolio.toTradePlan(), stock.info.lastPrice,
                            stock.portfolio.macdHist, stock.portfolio.sma50,
                            TechnicalAnalysis.isNearExDividendDate(stock.info.dividendDate)
                        )
                        if (decision != null) {
                            val alerts = if (tradePurpose == "DIVIDEND") dividendSellAlerts else swingSellAlerts
                            alerts.add(SellAlertData(stock, decision.description))
                        }
                } else if (applySwingLogic) {
                    val netProfit = stock.netProfitPercent
                    val netProfitBaht = TechnicalAnalysis.calculatePositionNetProfitBaht(
                        stock.portfolio.cost,
                        stock.info.lastPrice,
                        stock.portfolio.quantity,
                        stock.portfolio.buyFees,
                        isAtsEnabled.value
                    )
                    val rsi = stock.portfolio.rsi ?: 50.0

                    val targetAlerts = swingSellAlerts
                    
                    val currentPrice = stock.info.lastPrice
                    val cost = stock.portfolio.cost
                    val peakPrice = stock.portfolio.peakPrice
                    val explicitStopLoss = stock.portfolio.stopLoss
                    val maxPeak = maxOf(cost, peakPrice)
                    val dropFromPeak = if (maxPeak > 0) ((currentPrice - maxPeak) / maxPeak) * 100 else 0.0

                    // Fix #2: Scale absolute threshold with position size (at least 3% of position, min ₿500)
                    val positionValue = cost * stock.portfolio.quantity
                    val minTakeProfitBaht = maxOf(TradingConstants.TAKE_PROFIT_MIN_BAHT, positionValue * 0.03)
                    if (netProfit >= TradingConstants.TAKE_PROFIT_PERCENT || netProfitBaht >= minTakeProfitBaht) {
                        val reason = if (netProfit >= TradingConstants.TAKE_PROFIT_PERCENT) {
                            "Take Profit (Gain >= ${TradingConstants.TAKE_PROFIT_PERCENT}%)"
                        } else {
                            "Take Profit (P/L > ฿${String.format(java.util.Locale.ENGLISH, "%,.0f", minTakeProfitBaht)})"
                        }
                        targetAlerts.add(SellAlertData(stock, reason))
                    } else if (dropFromPeak <= -tsPercent) {
                        val stopLabel = if (peakPrice > cost) "Trailing Stop Loss (Drop <= -$tsPercent% from peak)" else "Stop Loss (Drop <= -$tsPercent%)"
                        targetAlerts.add(SellAlertData(stock, stopLabel))
                    } else if (explicitStopLoss > 0 && currentPrice <= explicitStopLoss) {
                        targetAlerts.add(SellAlertData(stock, "Stop Loss (Price <= $explicitStopLoss)"))
                    } else if (netProfit > 0.0 && rsi >= 65.0) {
                        targetAlerts.add(SellAlertData(stock, "Overbought (RSI >= 65)"))
                    } else if (stock.signal?.type == IndicatorSignal.SELL) {
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
                        !isTechnicalCacheExpired(it.signal?.lastUpdated)
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
                                    val needsIndicators = latestStock.rsi == null || latestStock.macdHist == null || isTechnicalCacheExpired(latestStock.signal?.lastUpdated)

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
                        !isTechnicalCacheExpired(it.signal?.lastUpdated)
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
                                    val needsIndicators = latestStock.rsi == null || latestStock.macdHist == null || isTechnicalCacheExpired(latestStock.signal?.lastUpdated)

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
                        isSet50 = TradingConstants.SET50_SYMBOLS.contains(symbol.uppercase()),
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
                    val explicitStopHit = portfolio != null && portfolio.quantity > 0 &&
                        portfolio.stopLoss > 0.0 && updatedInfo.lastPrice > 0.0 &&
                        updatedInfo.lastPrice <= portfolio.stopLoss
                    val signal = when {
                        explicitStopHit -> TradeSignal(IndicatorSignal.SELL, "STOP",
                            "Saved stop reached at ฿${portfolio!!.stopLoss}")
                        portfolio != null && portfolio.quantity > 0 && portfolio.exitPolicy == "FIXED_TARGET" -> {
                            val decision = apincer.mobile.tradings.domain.ExitPolicyEvaluator.evaluate(
                                portfolio.toTradePlan(), updatedInfo.lastPrice, macd.third, sma50,
                                TechnicalAnalysis.isNearExDividendDate(updatedInfo.dividendDate))
                            if (decision != null) TradeSignal(IndicatorSignal.SELL,
                                decision.reason.name, decision.description)
                            else TradeSignal(IndicatorSignal.NEUTRAL, "Saved plan active",
                                "No saved exit level or early invalidation trigger reached")
                        }
                        else -> rawSignal
                    }

                    val zone = TechnicalAnalysis.getTradingZone(rsi, macd.third, updatedInfo.lastPrice, sma50, sma200, bb)

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
