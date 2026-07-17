package apincer.mobile.tradings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import apincer.mobile.tradings.R
import apincer.mobile.tradings.domain.IndicatorSignal
import java.util.Locale

enum class WatchlistSortOrder(val label: String) {
    SYMBOL("Symbol"),
    CHANGE("Change %"),
    PROFIT("Profit %"),
    SIGNAL("Signal")
}

enum class WatchlistFilter(val label: String) {
    ALL("All"),
    FOCUS("Focus List"),
    PORTFOLIO("Portfolio"),
    BUY_SIGNAL("Buy Signals")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: StockViewModel,
    settingsViewModel: SettingsViewModel,
    watchlistViewModel: WatchlistViewModel = viewModel(),
    onSelectStock: (String) -> Unit,
    showSnackbar: (String) -> Unit
) {
    val watchlist by viewModel.watchlistInfo.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isPrivacyMode by settingsViewModel.isPrivacyMode.collectAsState()
    val lastSync = watchlist.mapNotNull { it.info.lastUpdated.takeIf { it.isNotBlank() } }.maxOrNull() ?: "---"
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var activeFilter by rememberSaveable { mutableStateOf(WatchlistFilter.ALL) }
    var sortOrder by rememberSaveable { mutableStateOf(WatchlistSortOrder.SYMBOL) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSortAscending by rememberSaveable { mutableStateOf(false) }

    var debouncedSearchQuery by remember { mutableStateOf(searchQuery) }

    LaunchedEffect(searchQuery) {
        kotlinx.coroutines.delay(300) // 300ms debounce
        debouncedSearchQuery = searchQuery
    }

    val processedList = remember(watchlist, activeFilter, sortOrder, debouncedSearchQuery, isSortAscending) {
        var list = when (activeFilter) {
            WatchlistFilter.ALL -> watchlist
            WatchlistFilter.FOCUS -> watchlist.filter { it.isFocused }
            WatchlistFilter.PORTFOLIO -> watchlist.filter { it.portfolio.quantity > 0 }
            WatchlistFilter.BUY_SIGNAL -> watchlist.filter { it.signal?.type == IndicatorSignal.BUY || it.signal?.type == IndicatorSignal.POTENTIAL }
        }

        if (debouncedSearchQuery.isNotBlank()) {
            val q = debouncedSearchQuery.trim().uppercase(Locale.ROOT)
            list = list.filter { 
                it.info.symbol.contains(q) || (it.info.name?.uppercase(Locale.ROOT)?.contains(q) == true)
            }
        }

        // Apply Sorting
        when (sortOrder) {
            WatchlistSortOrder.SYMBOL -> if (isSortAscending) list.sortedBy { it.info.symbol } else list.sortedByDescending { it.info.symbol }
            WatchlistSortOrder.CHANGE -> if (isSortAscending) list.sortedBy { it.info.percentChange } else list.sortedByDescending { it.info.percentChange }
            WatchlistSortOrder.PROFIT -> if (isSortAscending) list.sortedBy { it.netProfitPercent } else list.sortedByDescending { it.netProfitPercent }
            WatchlistSortOrder.SIGNAL -> {
                val selector = { it: StockWatchlistInfo ->
                    when (it.signal?.type) {
                        IndicatorSignal.BUY -> 3
                        IndicatorSignal.POTENTIAL -> 2
                        IndicatorSignal.NEUTRAL -> 1
                        IndicatorSignal.SELL -> 0
                        null -> -1
                    }
                }
                if (isSortAscending) list.sortedBy(selector) else list.sortedByDescending(selector)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { 
                if (isSearching) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter watchlist...", fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { 
                                searchQuery = ""
                                isSearching = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.title_watchlist), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        if (lastSync != "---") {
                            Text(
                                text = stringResource(R.string.label_last_sync, lastSync),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            actions = {
                if (!isSearching) {
                    IconButton(onClick = { isSearching = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Watchlist", modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.CloudDownload, contentDescription = stringResource(R.string.desc_import_set), modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.desc_add_stock), modifier = Modifier.size(24.dp))
                    }
                }
            }
        )

        // Filtering & Sorting Row
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(WatchlistFilter.entries.toList()) { filter ->
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { activeFilter = filter },
                    label = { Text(filter.label, fontSize = 12.sp) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            item {
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { isSortAscending = !isSortAscending },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = "Toggle Sort Direction",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            items(WatchlistSortOrder.entries.toList()) { order ->
                FilterChip(
                    selected = sortOrder == order,
                    onClick = { sortOrder = order },
                    label = { Text(order.label, fontSize = 12.sp) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Monitor Count Display
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (activeFilter == WatchlistFilter.FOCUS) {
                    stringResource(R.string.label_focused_count, processedList.size)
                } else {
                    stringResource(R.string.label_monitoring_count, processedList.size)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
        
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshWatchlistInfo() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (processedList.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().height(220.dp),
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (watchlist.isEmpty()) {
                                    Text(
                                        text = "Your watchlist is empty.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Add your first stock to get started!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Button(
                                        onClick = { showAddDialog = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.title_add_stock))
                                    }
                                } else {
                                    Text(
                                        text = if (activeFilter == WatchlistFilter.FOCUS) stringResource(R.string.label_no_focused_stocks) else stringResource(R.string.label_no_matches_found),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(processedList, key = { it.info.symbol }) { item ->
                        StockItemCard(
                            item = item,
                            onSelect = { onSelectStock(item.info.symbol) },
                            onDelete = { 
                                viewModel.removeFromWatchlist(item.info.symbol) 
                                showSnackbar("Removed ${item.info.symbol} from watchlist")
                            },
                            isPrivacyMode = isPrivacyMode
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddStockDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { symbol ->
                viewModel.addToWatchlist(symbol)
                showSnackbar("Added $symbol to watchlist")
                showAddDialog = false
            },
            watchlistViewModel = watchlistViewModel
        )
    }

    if (showImportDialog) {
        ImportCollectionDialog(
            onDismiss = { showImportDialog = false },
            onImport = { category ->
                watchlistViewModel.importFromCollection(category) {
                    viewModel.refreshWatchlistInfo()
                }
                showSnackbar("Importing $category collection...")
                showImportDialog = false
            },
            watchlistSymbols = watchlist.map { it.info.symbol }.toSet()
        )
    }
}

@Composable
fun ImportCollectionDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    watchlistSymbols: Set<String>
) {
    val collections = apincer.mobile.tradings.data.SetScraper.FALLBACK_COLLECTIONS

    GlassDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.title_import_set),
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.label_import_desc), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            collections.keys.forEach { category ->
                val categorySymbols = collections[category] ?: emptyList()
                val importedCount = categorySymbols.count { watchlistSymbols.contains(it) }
                val isFullyImported = categorySymbols.isNotEmpty() && importedCount == categorySymbols.size
                
                Surface(
                    onClick = { onImport(category) },
                    color = if (isFullyImported) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = category, fontWeight = FontWeight.Black, color = if (isFullyImported) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                            if (categorySymbols.isNotEmpty()) {
                                Text(
                                    text = if (isFullyImported) "All stocks imported" else "$importedCount/${categorySymbols.size} stocks imported",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                        if (isFullyImported) {
                            Icon(Icons.Default.Check, contentDescription = "Imported", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddStockDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    watchlistViewModel: WatchlistViewModel
) {
    var query by remember { mutableStateOf("") }
    val searchResults by watchlistViewModel.searchResults.collectAsState()

    GlassDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.title_add_stock),
        confirmButton = {
            Button(
                onClick = { 
                    if (query.isNotBlank()) onConfirm(query.uppercase()) 
                },
                enabled = query.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.action_add_manually))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { 
                    query = it
                    watchlistViewModel.searchStocks(it)
                },
                label = { Text(stringResource(R.string.label_search_stock)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            if (searchResults.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.label_search_results), 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { stock ->
                        Surface(
                            onClick = { onConfirm(stock.symbol) },
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = stock.symbol, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                    Text(
                                        text = stock.name ?: "", 
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    Icons.Default.Add, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
