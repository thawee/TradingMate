package apincer.mobile.tradings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.R
import apincer.mobile.tradings.data.StockEntity
import apincer.mobile.tradings.domain.IndicatorSignal

enum class WatchlistTab(val label: String) {
    ALL("All"),
    FOCUS("Focus")
}

enum class WatchlistSortOrder(val label: String) {
    SYMBOL("Symbol"),
    CHANGE("Change %"),
    PROFIT("Profit %"),
    SIGNAL("Signal")
}

/*
enum class WatchlistFilter(val label: String) {
    NONE("No Filter"),
    BUY_ONLY("Buy Only"),
    POSITIVE_ONLY("Positive Only"),
    HOLDINGS_ONLY("Holdings Only")
} */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: StockViewModel,
    onSelectStock: (String) -> Unit
) {
    val watchlist by viewModel.watchlistInfo.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(WatchlistTab.ALL) }
    
    var sortOrder by remember { mutableStateOf(WatchlistSortOrder.SYMBOL) }
    //var activeFilter by remember { mutableStateOf(WatchlistFilter.NONE) }

    val processedList = remember(watchlist, selectedTab, sortOrder) {
        var list = when (selectedTab) {
            WatchlistTab.ALL -> watchlist
            WatchlistTab.FOCUS -> watchlist.filter { it.isFocused }
        }

        // Apply Filters
       /* list = when (activeFilter) {
            WatchlistFilter.NONE -> list
            WatchlistFilter.BUY_ONLY -> list.filter { it.signal?.type == IndicatorSignal.BUY }
            WatchlistFilter.POSITIVE_ONLY -> list.filter { it.info.percentChange > 0 }
            WatchlistFilter.HOLDINGS_ONLY -> list.filter { it.portfolio.quantity > 0 }
        } */

        // Apply Sorting
        when (sortOrder) {
            WatchlistSortOrder.SYMBOL -> list.sortedBy { it.info.symbol }
            WatchlistSortOrder.CHANGE -> list.sortedByDescending { it.info.percentChange }
            WatchlistSortOrder.PROFIT -> list.sortedByDescending { it.netProfitPercent }
            WatchlistSortOrder.SIGNAL -> list.sortedByDescending { 
                when (it.signal?.type) {
                    IndicatorSignal.BUY -> 3
                    IndicatorSignal.POTENTIAL -> 2
                    IndicatorSignal.NEUTRAL -> 1
                    IndicatorSignal.SELL -> 0
                    null -> -1
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.title_watchlist), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            actions = {
                IconButton(onClick = { viewModel.refreshWatchlistInfo() }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.desc_refresh), modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = { showImportDialog = true }) {
                    Icon(Icons.Default.CloudDownload, contentDescription = stringResource(R.string.desc_import_set), modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.desc_add_stock), modifier = Modifier.size(24.dp))
                }
            }
        )

        // Tab Control
        GlassSegmentedControl(
            items = WatchlistTab.entries.toList(),
            selectedItem = selectedTab,
            onItemSelect = { selectedTab = it },
            labelExtractor = { it.label },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Sorting, Filtering Row
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(WatchlistSortOrder.entries.toList()) { order ->
                FilterChip(
                    selected = sortOrder == order,
                    onClick = { sortOrder = order },
                    label = { Text(order.label, fontSize = 10.sp) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
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
                text = if (selectedTab == WatchlistTab.FOCUS) {
                    stringResource(R.string.label_focused_count, processedList.size)
                } else {
                    stringResource(R.string.label_monitoring_count, processedList.size)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (processedList.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (selectedTab == WatchlistTab.FOCUS) stringResource(R.string.label_no_focused_stocks) else stringResource(R.string.label_no_matches_found), 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(processedList) { item ->
                    StockItemCard(
                        item = item,
                        onSelect = { onSelectStock(item.info.symbol) },
                        onDelete = { viewModel.removeFromWatchlist(item.info.symbol) }
                    )
                }
            }
            
            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showAddDialog) {
        AddStockDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { symbol ->
                viewModel.addToWatchlist(symbol)
                showAddDialog = false
            },
            viewModel = viewModel
        )
    }

    if (showImportDialog) {
        ImportCollectionDialog(
            onDismiss = { showImportDialog = false },
            onImport = { category ->
                viewModel.importFromCollection(category)
                showImportDialog = false
            }
        )
    }
}

@Composable
fun ImportCollectionDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    GlassDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.title_import_set),
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    ) {
        val collections = listOf("SET50", "SET100", "SETHD", "DIVIDEND", "BLUECHIP")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.label_import_desc), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            collections.forEach { category ->
                Surface(
                    onClick = { onImport(category) },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = category, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
    viewModel: StockViewModel
) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()

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
                    viewModel.searchStocks(it)
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
