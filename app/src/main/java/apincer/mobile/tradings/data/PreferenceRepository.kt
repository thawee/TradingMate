package apincer.mobile.tradings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

    val isPrivacyMode: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[IS_PRIVACY_MODE] ?: false
        }

    suspend fun setPrivacyMode(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[IS_PRIVACY_MODE] = enabled
        }
    }
}
