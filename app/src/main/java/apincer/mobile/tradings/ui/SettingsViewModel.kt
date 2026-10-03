package apincer.mobile.tradings.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import apincer.mobile.tradings.data.PreferenceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferenceRepository = PreferenceRepository(application)

    val targetMonthlyDividend: StateFlow<Double> = 
        preferenceRepository.targetMonthlyDividend.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 10000.0
        )

    fun updateTargetMonthlyDividend(amount: Double) {
        viewModelScope.launch {
            preferenceRepository.setTargetMonthlyDividend(amount.coerceAtLeast(0.0))
        }
    }

    val priceAlertThreshold: StateFlow<Double> = 
        preferenceRepository.priceAlertThreshold.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 10.0
        )

    fun updatePriceAlertThreshold(percent: Double) {
        viewModelScope.launch {
            preferenceRepository.setPriceAlertThreshold(percent.coerceIn(1.0, 50.0))
        }
    }

    val dividendAlertWindow: StateFlow<Int> = 
        preferenceRepository.dividendAlertWindow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 14
        )

    fun updateDividendAlertWindow(days: Int) {
        viewModelScope.launch {
            preferenceRepository.setDividendAlertWindow(days.coerceIn(1, 365))
        }
    }

    val isDividendAlertEndYear: StateFlow<Boolean> = 
        preferenceRepository.isDividendAlertEndYear.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun toggleDividendAlertEndYear() {
        viewModelScope.launch {
            preferenceRepository.setDividendAlertEndYear(!isDividendAlertEndYear.value)
        }
    }

    val isAtsEnabled: StateFlow<Boolean> =
        preferenceRepository.isAtsEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true   // default: ATS registered → minimum fee waived
        )

    fun toggleAtsEnabled() {
        viewModelScope.launch {
            preferenceRepository.setAtsEnabled(!isAtsEnabled.value)
        }
    }

    val targetCorePercent: StateFlow<Double> =
        preferenceRepository.targetCorePercent.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = apincer.mobile.tradings.domain.CoreSatellite.DEFAULT_TARGET_CORE_PERCENT
        )

    fun updateTargetCorePercent(percent: Double) {
        viewModelScope.launch {
            preferenceRepository.setTargetCorePercent(percent.coerceIn(0.0, 100.0))
        }
    }

    val monthlyDcaAmount: StateFlow<Double> =
        preferenceRepository.monthlyDcaAmount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    fun updateMonthlyDcaAmount(amount: Double) {
        viewModelScope.launch {
            preferenceRepository.setMonthlyDcaAmount(amount.coerceAtLeast(0.0))
        }
    }

    val dcaDayOfMonth: StateFlow<Int> =
        preferenceRepository.dcaDayOfMonth.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = apincer.mobile.tradings.domain.CoreSatellite.DEFAULT_DCA_DAY
        )

    fun updateDcaDayOfMonth(day: Int) {
        viewModelScope.launch {
            preferenceRepository.setDcaDayOfMonth(day.coerceIn(1, 31))
        }
    }

    val isEntryAlertsEnabled: StateFlow<Boolean> =
        preferenceRepository.isEntryAlertsEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun toggleEntryAlertsEnabled() {
        viewModelScope.launch {
            preferenceRepository.setEntryAlertsEnabled(!isEntryAlertsEnabled.value)
        }
    }

    val maxRiskPerTrade: StateFlow<Double> = 
        preferenceRepository.maxRiskPerTrade.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1.0
        )

    fun updateMaxRiskPerTrade(percent: Double) {
        viewModelScope.launch {
            preferenceRepository.setMaxRiskPerTrade(percent.coerceIn(0.1, 10.0))
        }
    }

    val maxOpenExposure: StateFlow<Double> = 
        preferenceRepository.maxOpenExposure.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 5.0
        )

    fun updateMaxOpenExposure(percent: Double) {
        viewModelScope.launch {
            preferenceRepository.setMaxOpenExposure(percent.coerceIn(1.0, 50.0))
        }
    }

    val maxPortfolioAllocation: StateFlow<Double> = 
        preferenceRepository.maxPortfolioAllocation.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 15.0
        )

    fun updateMaxPortfolioAllocation(percent: Double) {
        viewModelScope.launch {
            preferenceRepository.setMaxPortfolioAllocation(percent.coerceIn(2.0, 50.0))
        }
    }

    val maxSectorAllocation: StateFlow<Double> =
        preferenceRepository.maxSectorAllocation.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 30.0
        )

    fun updateMaxSectorAllocation(percent: Double) {
        viewModelScope.launch {
            preferenceRepository.setMaxSectorAllocation(percent.coerceIn(10.0, 80.0))
        }
    }

    val citTaxRate: StateFlow<Double> =
        preferenceRepository.citTaxRate.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 20.0
        )

    val coreMix: StateFlow<Pair<String?, Double>> =
        preferenceRepository.coreMix.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null to 50.0)

    fun updateCoreMix(symbol: String?, percent: Double) {
        viewModelScope.launch { preferenceRepository.setCoreMix(symbol, percent) }
    }

    val assessableIncome: StateFlow<Double?> =
        preferenceRepository.assessableIncome.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val otherRetirementContributions: StateFlow<Double> =
        preferenceRepository.otherRetirementContributions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val taxFundPurchases: StateFlow<List<apincer.mobile.tradings.domain.TaxFunds.Purchase>> =
        preferenceRepository.taxFundPurchases.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateTaxFundProfile(income: Double?, otherRetirement: Double) {
        viewModelScope.launch { preferenceRepository.setTaxFundProfile(income, maxOf(0.0, otherRetirement)) }
    }

    fun addTaxFundPurchase(purchase: apincer.mobile.tradings.domain.TaxFunds.Purchase) {
        viewModelScope.launch { preferenceRepository.setTaxFundPurchases(preferenceRepository.taxFundPurchases.first() + purchase) }
    }

    fun removeTaxFundPurchase(purchase: apincer.mobile.tradings.domain.TaxFunds.Purchase) {
        viewModelScope.launch { preferenceRepository.setTaxFundPurchases(preferenceRepository.taxFundPurchases.first() - purchase) }
    }


    val personalTaxRate: StateFlow<Double?> =
        preferenceRepository.personalTaxRate.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun updatePersonalTaxRate(rate: Double?) {
        viewModelScope.launch {
            preferenceRepository.setPersonalTaxRate(rate?.coerceIn(0.0, 35.0))
        }
    }

    fun updateCitTaxRate(rate: Double) {
        viewModelScope.launch {
            preferenceRepository.setCitTaxRate(rate.coerceIn(0.0, 30.0))
        }
    }

    val minRiskRewardRatio: StateFlow<Double> = 
        preferenceRepository.minRiskRewardRatio.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 2.0
        )

    fun updateMinRiskRewardRatio(ratio: Double) {
        if (!ratio.isFinite()) return
        viewModelScope.launch {
            preferenceRepository.setMinRiskRewardRatio(ratio.coerceIn(1.0, 10.0))
        }
    }

    val geminiApiKey: StateFlow<String> =
        preferenceRepository.geminiApiKey.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    fun updateGeminiApiKey(key: String) {
        viewModelScope.launch {
            preferenceRepository.setGeminiApiKey(key)
        }
    }

    val geminiModel: StateFlow<String> =
        preferenceRepository.geminiModel.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "gemini-3.6-flash"
        )

    fun updateGeminiModel(modelId: String) {
        viewModelScope.launch {
            preferenceRepository.setGeminiModel(modelId)
        }
    }
}
