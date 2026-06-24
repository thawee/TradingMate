package apincer.mobile.tradings.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import apincer.mobile.tradings.data.PreferenceRepository
import kotlinx.coroutines.flow.SharingStarted
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
            preferenceRepository.setTargetMonthlyDividend(amount)
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
            preferenceRepository.setPriceAlertThreshold(percent)
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
            preferenceRepository.setDividendAlertWindow(days)
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

    val isPrivacyMode: StateFlow<Boolean> = 
        preferenceRepository.isPrivacyMode.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun updatePrivacyMode(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setPrivacyMode(enabled)
        }
    }

    val trailingStopPercent: StateFlow<Double> = 
        preferenceRepository.trailingStopPercent.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 5.0
        )

    fun updateTrailingStopPercent(percent: Double) {
        viewModelScope.launch {
            preferenceRepository.setTrailingStopPercent(percent)
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

    val maxRiskPerTrade: StateFlow<Double> = 
        preferenceRepository.maxRiskPerTrade.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1.0
        )

    fun updateMaxRiskPerTrade(percent: Double) {
        viewModelScope.launch {
            preferenceRepository.setMaxRiskPerTrade(percent)
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
            preferenceRepository.setMaxOpenExposure(percent)
        }
    }

    val maxPortfolioAllocation: StateFlow<Double> = 
        preferenceRepository.maxPortfolioAllocation.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 10.0
        )

    fun updateMaxPortfolioAllocation(percent: Double) {
        viewModelScope.launch {
            preferenceRepository.setMaxPortfolioAllocation(percent)
        }
    }

    val minRiskRewardRatio: StateFlow<Double> = 
        preferenceRepository.minRiskRewardRatio.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 2.0
        )

    fun updateMinRiskRewardRatio(ratio: Double) {
        viewModelScope.launch {
            preferenceRepository.setMinRiskRewardRatio(ratio)
        }
    }
}
