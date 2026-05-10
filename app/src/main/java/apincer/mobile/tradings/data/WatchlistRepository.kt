package apincer.mobile.tradings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "watchlist")

class WatchlistRepository(private val context: Context) {
    private val WATCHLIST_KEY = stringSetPreferencesKey("stock_symbols")

    val watchlist: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[WATCHLIST_KEY] ?: emptySet()
        }

    suspend fun addStock(symbol: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[WATCHLIST_KEY] ?: emptySet()
            preferences[WATCHLIST_KEY] = current + symbol.uppercase()
        }
    }

    suspend fun removeStock(symbol: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[WATCHLIST_KEY] ?: emptySet()
            preferences[WATCHLIST_KEY] = current - symbol.uppercase()
        }
    }
}
