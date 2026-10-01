package apincer.mobile.tradings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferenceRepository(private val context: Context) {
    private val TARGET_MONTHLY_DIVIDEND = doublePreferencesKey("target_monthly_dividend")
    private val PRICE_ALERT_THRESHOLD = doublePreferencesKey("price_alert_threshold")
    private val DIVIDEND_ALERT_WINDOW = intPreferencesKey("dividend_alert_window")
    private val IS_DIVIDEND_ALERT_END_YEAR = booleanPreferencesKey("is_dividend_alert_end_year")
    private val IS_PRIVACY_MODE = booleanPreferencesKey("is_privacy_mode")
    private val IS_ATS_ENABLED = booleanPreferencesKey("is_ats_enabled")
    private val IS_ENTRY_ALERTS_ENABLED = booleanPreferencesKey("is_entry_alerts_enabled")
    private val TRAILING_STOP_PERCENT = doublePreferencesKey("trailing_stop_percent")

    private val MAX_RISK_PER_TRADE = doublePreferencesKey("max_risk_per_trade")
    private val MAX_OPEN_EXPOSURE = doublePreferencesKey("max_open_exposure")
    private val MAX_PORTFOLIO_ALLOCATION = doublePreferencesKey("max_portfolio_allocation")
    private val MAX_SECTOR_ALLOCATION = doublePreferencesKey("max_sector_allocation")
    private val CIT_TAX_RATE = doublePreferencesKey("cit_tax_rate")
    private val MIN_RISK_REWARD_RATIO = doublePreferencesKey("min_risk_reward_ratio")
    private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    private val GEMINI_MODEL = stringPreferencesKey("gemini_model")
    val targetMonthlyDividend: Flow<Double> = context.settingsDataStore.data
        .map { preferences ->
            preferences[TARGET_MONTHLY_DIVIDEND] ?: 10000.0
        }

    suspend fun setTargetMonthlyDividend(amount: Double) {
        context.settingsDataStore.edit { preferences ->
            preferences[TARGET_MONTHLY_DIVIDEND] = amount
        }
    }

    val priceAlertThreshold: Flow<Double> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PRICE_ALERT_THRESHOLD] ?: 10.0
        }

    suspend fun setPriceAlertThreshold(percent: Double) {
        context.settingsDataStore.edit { preferences ->
            preferences[PRICE_ALERT_THRESHOLD] = percent
        }
    }

    val dividendAlertWindow: Flow<Int> = context.settingsDataStore.data
        .map { preferences ->
            preferences[DIVIDEND_ALERT_WINDOW] ?: 14
        }

    suspend fun setDividendAlertWindow(days: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[DIVIDEND_ALERT_WINDOW] = days
        }
    }

    val isDividendAlertEndYear: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[IS_DIVIDEND_ALERT_END_YEAR] ?: false
        }

    suspend fun setDividendAlertEndYear(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[IS_DIVIDEND_ALERT_END_YEAR] = enabled
        }
    }

    // ATS (Automatic Transfer System) + E-Statement: waives the ฿50/day minimum commission
    val isAtsEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[IS_ATS_ENABLED] ?: true   // default: waived (most users register ATS)
        }

    suspend fun setAtsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[IS_ATS_ENABLED] = enabled
        }
    }

    // Entry (BUY / POTENTIAL) signal notifications. Off by default: the market-wide backtest
    // (tools/backtest/report.md) shows no edge for entry signals. Risk exits are always on.
    val isEntryAlertsEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[IS_ENTRY_ALERTS_ENABLED] ?: false
        }

    suspend fun setEntryAlertsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[IS_ENTRY_ALERTS_ENABLED] = enabled
        }
    }

    val trailingStopPercent: Flow<Double> = context.settingsDataStore.data
        .map { preferences ->
            preferences[TRAILING_STOP_PERCENT] ?: 5.0
        }

    suspend fun setTrailingStopPercent(percent: Double) {
        context.settingsDataStore.edit { preferences ->
            preferences[TRAILING_STOP_PERCENT] = percent
        }
    }

    val maxRiskPerTrade: Flow<Double> = context.settingsDataStore.data
        .map { preferences ->
            preferences[MAX_RISK_PER_TRADE] ?: 1.0
        }

    suspend fun setMaxRiskPerTrade(percent: Double) {
        context.settingsDataStore.edit { preferences ->
            preferences[MAX_RISK_PER_TRADE] = percent
        }
    }

    val maxOpenExposure: Flow<Double> = context.settingsDataStore.data
        .map { preferences ->
            preferences[MAX_OPEN_EXPOSURE] ?: 5.0
        }

    suspend fun setMaxOpenExposure(percent: Double) {
        context.settingsDataStore.edit { preferences ->
            preferences[MAX_OPEN_EXPOSURE] = percent
        }
    }

    val maxPortfolioAllocation: Flow<Double> = context.settingsDataStore.data
        .map { preferences ->
            preferences[MAX_PORTFOLIO_ALLOCATION] ?: 15.0
        }

    suspend fun setMaxPortfolioAllocation(percent: Double) {
        context.settingsDataStore.edit { preferences ->
            preferences[MAX_PORTFOLIO_ALLOCATION] = percent
        }
    }

    val maxSectorAllocation: Flow<Double> = context.settingsDataStore.data
        .map { preferences ->
            preferences[MAX_SECTOR_ALLOCATION] ?: 30.0
        }

    suspend fun setMaxSectorAllocation(percent: Double) {
        context.settingsDataStore.edit { preferences ->
            preferences[MAX_SECTOR_ALLOCATION] = percent
        }
    }

    val citTaxRate: Flow<Double> = context.settingsDataStore.data
        .map { preferences ->
            preferences[CIT_TAX_RATE] ?: 20.0
        }

    suspend fun setCitTaxRate(rate: Double) {
        context.settingsDataStore.edit { preferences ->
            preferences[CIT_TAX_RATE] = rate
        }
    }

    val minRiskRewardRatio: Flow<Double> = context.settingsDataStore.data
        .map { preferences ->
            preferences[MIN_RISK_REWARD_RATIO]?.takeIf { it.isFinite() && it in 1.0..10.0 } ?: 2.0
        }

    suspend fun setMinRiskRewardRatio(ratio: Double) {
        context.settingsDataStore.edit { preferences ->
            preferences[MIN_RISK_REWARD_RATIO] = ratio
        }
    }

    val geminiApiKey: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[GEMINI_API_KEY] ?: ""
        }

    suspend fun setGeminiApiKey(key: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[GEMINI_API_KEY] = key
        }
    }

    /** Selected free-tier Gemini model id, e.g. "gemini-3.6-flash". Defaults to the balanced Flash model. */
    val geminiModel: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[GEMINI_MODEL] ?: "gemini-3.6-flash"
        }

    suspend fun setGeminiModel(modelId: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[GEMINI_MODEL] = modelId
        }
    }
}
