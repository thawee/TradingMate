package apincer.mobile.tradings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class WatchlistSort {
    SYMBOL, YIELD, QUALITY_PROFIT, POTENTIAL
}

@Composable
fun WatchlistScreen(viewModel: StockViewModel, onSelectStock: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf(WatchlistSort.SYMBOL) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    
    val watchlistInfo by viewModel.watchlistInfo.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    val lastUpdated = watchlistInfo.map { it.info.lastUpdated }
        .filter { it.isNotEmpty() }
        .maxOrNull() ?: "N/A"

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportWatchlist(context.contentResolver, it)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val type = context.contentResolver.getType(it)
            if (type == "text/csv" || it.path?.endsWith(".csv") == true) {
                viewModel.importWatchlistCsv(context.contentResolver, it)
            } else {
                viewModel.importWatchlistJson(context.contentResolver, it)
            }
        }
    }

    val filteredItems = remember(watchlistInfo, searchQuery, sortBy) {
        val base = if (searchQuery.isBlank()) {
            watchlistInfo
        } else {
            watchlistInfo.filter { 
                it.info.symbol.contains(searchQuery, ignoreCase = true) || 
                (it.info.name?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
        
        when (sortBy) {
            WatchlistSort.SYMBOL -> base.sortedBy { it.info.symbol }
            WatchlistSort.YIELD -> base.sortedByDescending { it.info.dividendYield ?: 0.0 }
            WatchlistSort.QUALITY_PROFIT -> base.sortedWith(
                compareByDescending<StockWatchlistInfo> { it.info.isFundamentalGood }
                .thenByDescending { it.netProfitPercent }
            )
            WatchlistSort.POTENTIAL -> base.sortedWith(
                compareByDescending<StockWatchlistInfo> { item ->
                    when (item.signal?.type) {
                        apincer.mobile.tradings.domain.IndicatorSignal.BUY -> 3
                        apincer.mobile.tradings.domain.IndicatorSignal.POTENTIAL -> 2
                        apincer.mobile.tradings.domain.IndicatorSignal.NEUTRAL -> 1
                        apincer.mobile.tradings.domain.IndicatorSignal.SELL -> 0
                        null -> -1
                    }
                }.thenBy { it.portfolio.rsi ?: 100.0 }
            )
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Watchlist", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Monitoring ${watchlistInfo.size} stocks | Updated: $lastUpdated", 
                        fontSize = 12.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reset Button
                    IconButton(
                        onClick = { showResetConfirm = true },
                        enabled = !isRefreshing && watchlistInfo.any { it.portfolio.quantity == 0 },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Reset Watchlist", modifier = Modifier.size(22.dp))
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { viewModel.refreshWatchlistInfo() },
                        enabled = !isRefreshing,
                        modifier = Modifier.size(44.dp)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh, 
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Export Button
                    IconButton(
                        onClick = { exportLauncher.launch("tradingmate_watchlist.json") },
                        enabled = watchlistInfo.isNotEmpty(),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload, 
                            contentDescription = "Export Watchlist", 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Unified Add Button
                    IconButton(
                        onClick = { showAddDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add or Import Stocks", modifier = Modifier.size(22.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Combined Filter & Sort Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter symbols...", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Add, contentDescription = "Clear", modifier = Modifier.size(16.dp).rotate(45f))
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                var showSortMenu by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { showSortMenu = true },
                        label = { 
                            Text(
                                text = when(sortBy) {
                                    WatchlistSort.SYMBOL -> "Symbol"
                                    WatchlistSort.YIELD -> "Yield %"
                                    WatchlistSort.QUALITY_PROFIT -> "Quality & Profit"
                                    WatchlistSort.POTENTIAL -> "Best Entry"
                                },
                                fontSize = 12.sp
                            ) 
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        shape = MaterialTheme.shapes.medium
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        WatchlistSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { 
                                    val label = when(option) {
                                        WatchlistSort.QUALITY_PROFIT -> "Quality & Profit"
                                        WatchlistSort.POTENTIAL -> "Best Entry (Potential)"
                                        else -> option.name.lowercase().replaceFirstChar { it.uppercase() }
                                    }
                                    Text(label) 
                                },
                                onClick = {
                                    sortBy = option
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortBy == option) {
                                        Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Your watchlist is empty." else "No stocks match your search.", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredItems) { item ->
                        StockItemCard(
                            item = item,
                            onSelect = { onSelectStock(item.info.symbol) },
                            onDelete = { viewModel.removeFromWatchlist(item.info.symbol) },
                            onSell = null,
                            onEdit = null
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        UnifiedAddStockDialog(
            viewModel = viewModel,
            onDismiss = { 
                showAddDialog = false 
                viewModel.resetSearchResults()
            },
            onConfirmSingle = { symbol ->
                viewModel.addToWatchlist(symbol, 0.0, 0)
                showAddDialog = false
                viewModel.resetSearchResults()
            },
            onImportCollection = { category ->
                viewModel.importFromCollection(category)
                showAddDialog = false
            },
            onImportFile = {
                importLauncher.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "text/plain"))
                showAddDialog = false
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Watchlist") },
            text = { Text("This will remove all stocks from your watchlist that are NOT currently in your portfolio. Your trade history will be preserved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearWatchlist()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UnifiedAddStockDialog(
    viewModel: StockViewModel,
    onDismiss: () -> Unit, 
    onConfirmSingle: (String) -> Unit,
    onImportCollection: (String) -> Unit,
    onImportFile: () -> Unit
) {
    var symbolInput by remember { mutableStateOf("") }
    val searchResult by viewModel.searchResults.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Stocks") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Section 1: Single Symbol
                Column {
                    Text("Add Single Symbol", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = symbolInput,
                        onValueChange = { 
                            symbolInput = it.uppercase() 
                            viewModel.searchSymbol(it)
                        },
                        label = { Text("Symbol (e.g. PTT)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(
                                onClick = { if(symbolInput.isNotBlank()) onConfirmSingle(symbolInput) },
                                enabled = symbolInput.isNotBlank()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )

                    searchResult?.let { info ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            onClick = { onConfirmSingle(info.symbol) },
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(info.symbol, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(info.name ?: "Unknown Company", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    if (info.sector != null) {
                                        Text("${info.sector} • ${info.industry}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    }
                                }
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // Section 2: Collections
                Column {
                    Text("Import Curated Collections", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Quickly add high-quality stocks to monitor.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CollectionImportButton(
                            label = "Dividend Stars", 
                            description = "Top consistent yield payers",
                            color = MaterialTheme.colorScheme.tertiary,
                            onClick = { onImportCollection("DIVIDEND") }
                        )
                        CollectionImportButton(
                            label = "Growth & Bluechips", 
                            description = "SET50 Market Leaders",
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { onImportCollection("BLUECHIP") }
                        )
                        CollectionImportButton(
                            label = "SET50 Index", 
                            description = "Top 50 market leaders",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { onImportCollection("SET50") }
                        )
                        CollectionImportButton(
                            label = "SET100 Index", 
                            description = "Top 100 market leaders",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { onImportCollection("SET100") }
                        )
                        CollectionImportButton(
                            label = "SETHD Index", 
                            description = "High Dividend yield stocks",
                            color = MaterialTheme.colorScheme.tertiary,
                            onClick = { onImportCollection("SETHD") }
                        )
                        
                        CollectionImportButton(
                            label = "Import from File", 
                            description = "Restore JSON or CSV list",
                            color = MaterialTheme.colorScheme.secondary,
                            icon = Icons.Default.FileUpload,
                            onClick = { onImportFile() }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CollectionImportButton(
    label: String, 
    description: String, 
    color: Color, 
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Collections,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
                Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
    }
}
