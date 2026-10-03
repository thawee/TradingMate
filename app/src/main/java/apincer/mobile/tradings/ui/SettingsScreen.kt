package apincer.mobile.tradings.ui

import android.content.Intent
import androidx.compose.material.icons.filled.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.R
import apincer.mobile.tradings.domain.AvailableGeminiModel
import apincer.mobile.tradings.domain.GeminiClient
import apincer.mobile.tradings.domain.GeminiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: StockViewModel,
    settingsViewModel: SettingsViewModel,
    showSnackbar: (String) -> Unit
) {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let {
                viewModel.exportBackup(
                    contentResolver = context.contentResolver,
                    uri = it,
                    onSuccess = {
                        showSnackbar("Backup exported successfully!")
                    },
                    onError = { err ->
                        showSnackbar("Export failed: ${err.message}")
                    }
                )
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                viewModel.importBackup(
                    contentResolver = context.contentResolver,
                    uri = it,
                    onSuccess = {
                        showSnackbar("Backup restored successfully!")
                    },
                    onError = { err ->
                        showSnackbar("Restore failed: ${err.message}")
                    }
                )
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.title_settings), fontWeight = FontWeight.Black) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // The index core is the app's main plan, so it comes first.
            SectionContent(title = "Core Portfolio", icon = Icons.Default.Savings) {
                val targetCore by settingsViewModel.targetCorePercent.collectAsState()
                val dcaAmount by settingsViewModel.monthlyDcaAmount.collectAsState()
                val dcaDay by settingsViewModel.dcaDayOfMonth.collectAsState()
                var editingCore by remember(targetCore) { mutableStateOf(targetCore.toInt().toString()) }
                var editingDca by remember(dcaAmount) { mutableStateOf(if (dcaAmount > 0) dcaAmount.toLong().toString() else "") }
                var editingDay by remember(dcaDay) { mutableStateOf(dcaDay.toString()) }

                OutlinedTextField(
                    value = editingCore,
                    onValueChange = {
                        editingCore = it
                        it.toDoubleOrNull()?.let { percent -> settingsViewModel.updateTargetCorePercent(percent) }
                    },
                    label = { Text("Target Core Allocation") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("%") },
                    shape = RoundedCornerShape(14.dp)
                )
                Text(
                    text = "Share of invested value held in TDEX (SET50 ETF). The rest is your satellite for individual stocks. Default 80%.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = editingDca,
                    onValueChange = {
                        editingDca = it
                        settingsViewModel.updateMonthlyDcaAmount(it.toDoubleOrNull() ?: 0.0)
                    },
                    label = { Text("Monthly DCA Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("฿") },
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editingDay,
                    onValueChange = {
                        editingDay = it
                        it.toIntOrNull()?.let { day -> settingsViewModel.updateDcaDayOfMonth(day) }
                    },
                    label = { Text("DCA Day of Month") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp)
                )
                Text(
                    text = "On this day (or the next trading day) you get a reminder with the board lots your amount buys for each core fund, fees included. Leave the amount empty to turn the reminder off.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )

                // Optional second core ETF, e.g. 1DIV (SET High Dividend 30): the core becomes a mix with TDEX.
                val coreMix by settingsViewModel.coreMix.collectAsState()
                var editingSecond by remember(coreMix) { mutableStateOf(coreMix.first ?: "") }
                var editingSecondPct by remember(coreMix) { mutableStateOf(coreMix.second.toInt().toString()) }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editingSecond,
                        // Saved with the button below: saving per keystroke re-keyed this field and scrambled typing.
                        onValueChange = { editingSecond = it.uppercase() },
                        label = { Text("Second core ETF (optional)") },
                        placeholder = { Text("e.g. 1DIV") },
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = editingSecondPct,
                        onValueChange = { editingSecondPct = it },
                        label = { Text("Share") },
                        suffix = { Text("%") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
                val pctValue = editingSecondPct.toDoubleOrNull()?.takeIf { it in 0.0..100.0 }
                val changed = editingSecond.ifBlank { null } != coreMix.first || pctValue != coreMix.second
                if (changed) {
                    TextButton(
                        onClick = { settingsViewModel.updateCoreMix(editingSecond.ifBlank { null }, pctValue ?: 50.0) },
                        enabled = pctValue != null
                    ) { Text("Save core mix") }
                }
                Text(
                    text = "TDEX holds the rest of the core. 1DIV (SET High Dividend 30 ETF) returned 3.86% a year vs TDEX 2.55% over 2015-2025, ahead in 2021-2025 but behind in 2015-2020 and 2012-2014. Leave blank for TDEX only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            SectionContent(title = stringResource(R.string.section_dividend_goal), icon = Icons.Default.Savings) {
                val targetDividend by settingsViewModel.targetMonthlyDividend.collectAsState()
                var editingTarget by remember(targetDividend) { mutableStateOf(targetDividend.toInt().toString()) }

                OutlinedTextField(
                    value = editingTarget,
                    onValueChange = { 
                        editingTarget = it
                        it.toDoubleOrNull()?.let { amount ->
                            settingsViewModel.updateTargetMonthlyDividend(amount)
                        }
                    },
                    label = { Text(stringResource(R.string.label_target_monthly_dividend)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("฿ ") },
                    shape = RoundedCornerShape(14.dp)
                )
                Text(
                    text = "Your target monthly passive income from stock dividends. Used to calculate capital requirements and portfolio goal completion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                val citTaxRate by settingsViewModel.citTaxRate.collectAsState()
                var editingCitRate by remember(citTaxRate) { mutableStateOf(citTaxRate.toString()) }

                OutlinedTextField(
                    value = editingCitRate,
                    onValueChange = { 
                        editingCitRate = it
                        it.toDoubleOrNull()?.let { rate ->
                            settingsViewModel.updateCitTaxRate(rate)
                        }
                    },
                    label = { Text(stringResource(R.string.label_cit_tax_rate)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("%") },
                    shape = RoundedCornerShape(14.dp)
                )
                Text(
                    text = stringResource(R.string.desc_cit_tax_rate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                val personalTaxRate by settingsViewModel.personalTaxRate.collectAsState()
                var editingPersonalRate by remember(personalTaxRate) {
                    mutableStateOf(personalTaxRate?.let { String.format(java.util.Locale.ENGLISH, "%.0f", it) } ?: "")
                }
                OutlinedTextField(
                    value = editingPersonalRate,
                    onValueChange = {
                        editingPersonalRate = it
                        if (it.isBlank()) settingsViewModel.updatePersonalTaxRate(null)
                        else it.toDoubleOrNull()?.let { rate -> settingsViewModel.updatePersonalTaxRate(rate) }
                    },
                    label = { Text(stringResource(R.string.label_personal_tax_rate)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("%") },
                    shape = RoundedCornerShape(14.dp)
                )
                Text(
                    text = stringResource(R.string.desc_personal_tax_rate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            SectionContent(title = stringResource(R.string.section_price_alerts), icon = Icons.Default.NotificationsActive) {
                val alertThreshold by settingsViewModel.priceAlertThreshold.collectAsState()
                var editingThreshold by remember(alertThreshold) { mutableStateOf(alertThreshold.toInt().toString()) }

                OutlinedTextField(
                    value = editingThreshold,
                    onValueChange = { 
                        editingThreshold = it
                        it.toDoubleOrNull()?.let { percent ->
                            settingsViewModel.updatePriceAlertThreshold(percent)
                        }
                    },
                    label = { Text(stringResource(R.string.label_alert_threshold_percent)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("%") },
                    shape = RoundedCornerShape(14.dp)
                )
                Text(
                    text = "Proximity threshold percentage for target buy/sell price alerts (e.g. within 10% of target).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                val dividendWindow by settingsViewModel.dividendAlertWindow.collectAsState()
                var editingWindow by remember(dividendWindow) { mutableStateOf(dividendWindow.toString()) }
                val isEndYear by settingsViewModel.isDividendAlertEndYear.collectAsState()

                OutlinedTextField(
                    value = editingWindow,
                    onValueChange = { 
                        editingWindow = it
                        it.toIntOrNull()?.let { days ->
                            settingsViewModel.updateDividendAlertWindow(days)
                        }
                    },
                    label = { Text(stringResource(R.string.label_dividend_alert_window)) },
                    modifier = Modifier.fillMaxWidth().alpha(if (isEndYear) 0.5f else 1f),
                    enabled = !isEndYear,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("Days") },
                    shape = RoundedCornerShape(14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.label_dividend_alert_end_year), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isEndYear, onCheckedChange = { settingsViewModel.toggleDividendAlertEndYear() })
                }

                val isEntryAlertsEnabled by settingsViewModel.isEntryAlertsEnabled.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Entry signal alerts", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Technical entry signals trailed TDEX buy-and-hold in a 2015-2025 backtest. Stop and exit alerts stay on.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isEntryAlertsEnabled, onCheckedChange = { settingsViewModel.toggleEntryAlertsEnabled() })
                }
            }

            SectionContent(title = "App Environment", icon = Icons.Default.Settings) {
                val prefs = context.getSharedPreferences("app_sandbox", android.content.Context.MODE_PRIVATE)
                var isSandbox by remember { mutableStateOf(prefs.getBoolean("is_sandbox_mode", false)) }

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Paper Trading Sandbox", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Simulate trades in an isolated database.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isSandbox,
                        onCheckedChange = { checked ->
                            prefs.edit().putBoolean("is_sandbox_mode", checked).apply()
                            isSandbox = checked
                            showSnackbar("Environment changed. Restarting app...")
                            
                            val packageManager = context.packageManager
                            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                            val componentName = intent?.component
                            val mainIntent = Intent.makeRestartActivityTask(componentName)
                            context.startActivity(mainIntent)
                            Runtime.getRuntime().exit(0)
                        }
                    )
                }

            }

            SectionContent(title = "Risk Management", icon = Icons.Default.Warning) {
                val maxRiskPerTrade by settingsViewModel.maxRiskPerTrade.collectAsState()
                var editingMaxRisk by remember(maxRiskPerTrade) { mutableStateOf(maxRiskPerTrade.toString()) }

                OutlinedTextField(
                    value = editingMaxRisk,
                    onValueChange = { 
                        editingMaxRisk = it
                        it.toDoubleOrNull()?.let { percent -> settingsViewModel.updateMaxRiskPerTrade(percent) }
                    },
                    label = { Text("Max Risk Per Trade") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("%") },
                    shape = RoundedCornerShape(14.dp)
                )
                Text(
                    text = stringResource(R.string.desc_max_risk_per_trade),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Exits: each holding's saved stop (set from daily volatility and rounded to a SET price), then a trailing stop of 2.5x daily volatility (4-10%) once the trade is up 1R. This is the rule the backtest measured.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                var showAdvancedRisk by remember { mutableStateOf(false) }

                TextButton(
                    onClick = { showAdvancedRisk = !showAdvancedRisk },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (showAdvancedRisk) "Hide Advanced Risk Parameters ▲" else "Show Advanced Risk Parameters ▼",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                AnimatedVisibility(visible = showAdvancedRisk) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))

                        val maxOpenExposure by settingsViewModel.maxOpenExposure.collectAsState()
                        var editingMaxOpen by remember(maxOpenExposure) { mutableStateOf(maxOpenExposure.toString()) }

                        OutlinedTextField(
                            value = editingMaxOpen,
                            onValueChange = { 
                                editingMaxOpen = it
                                it.toDoubleOrNull()?.let { percent -> settingsViewModel.updateMaxOpenExposure(percent) }
                            },
                            label = { Text("Max Open Risk Exposure") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            suffix = { Text("%") },
                            shape = RoundedCornerShape(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.desc_max_open_exposure),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val maxAllocation by settingsViewModel.maxPortfolioAllocation.collectAsState()
                        var editingMaxAlloc by remember(maxAllocation) { mutableStateOf(maxAllocation.toString()) }

                        OutlinedTextField(
                            value = editingMaxAlloc,
                            onValueChange = { 
                                editingMaxAlloc = it
                                it.toDoubleOrNull()?.let { percent -> settingsViewModel.updateMaxPortfolioAllocation(percent) }
                            },
                            label = { Text(stringResource(R.string.label_max_portfolio_allocation)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            suffix = { Text("%") },
                            shape = RoundedCornerShape(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.desc_max_portfolio_allocation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val maxSector by settingsViewModel.maxSectorAllocation.collectAsState()
                        var editingMaxSector by remember(maxSector) { mutableStateOf(maxSector.toString()) }

                        OutlinedTextField(
                            value = editingMaxSector,
                            onValueChange = { 
                                editingMaxSector = it
                                it.toDoubleOrNull()?.let { percent -> settingsViewModel.updateMaxSectorAllocation(percent) }
                            },
                            label = { Text(stringResource(R.string.label_max_sector_allocation)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            suffix = { Text("%") },
                            shape = RoundedCornerShape(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.desc_max_sector_allocation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val minRR by settingsViewModel.minRiskRewardRatio.collectAsState()
                        var editingMinRR by remember(minRR) { mutableStateOf(minRR.toString()) }

                        OutlinedTextField(
                            value = editingMinRR,
                            onValueChange = { 
                                editingMinRR = it
                                it.toDoubleOrNull()?.let { ratio -> settingsViewModel.updateMinRiskRewardRatio(ratio) }
                            },
                            label = { Text("Minimum Risk/Reward Ratio") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.desc_min_risk_reward),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }
            }

            SectionContent(title = "Trading Fees", icon = Icons.Default.AccountBalanceWallet) {
                val isAtsEnabled by settingsViewModel.isAtsEnabled.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ATS / E-Statement Registered", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (isAtsEnabled)
                                "Min. commission waived (฿0) — ATS registered ✓"
                            else
                                "Min. commission ฿50/day applied — No ATS",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAtsEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                    Switch(checked = isAtsEnabled, onCheckedChange = { settingsViewModel.toggleAtsEnabled() })
                }
                Text(
                    text = "InnovestX waives the ฿50/day minimum commission if you have registered Automatic Transfer System (ATS) and opted for E-Statements. Enable this if you have completed that setup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                )
            }

            SectionContent(title = "AI Integration", icon = Icons.Default.AccountBalanceWallet) {
                val geminiApiKey by settingsViewModel.geminiApiKey.collectAsState()
                var editingApiKey by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }

                OutlinedTextField(
                    value = editingApiKey,
                    onValueChange = {
                        editingApiKey = it
                        settingsViewModel.updateGeminiApiKey(it)
                    },
                    label = { Text("Gemini API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Text(
                    text = "Required for the in-app \"Analyze with AI\" button in Smart Advisors. Get a free key from Google AI Studio (aistudio.google.com/apikey). Stored only on this device; sent directly to Google, never to our servers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )

                val geminiModelId by settingsViewModel.geminiModel.collectAsState()
                var fetchedModels by remember { mutableStateOf<List<AvailableGeminiModel>?>(null) }
                var isFetchingModels by remember { mutableStateOf(false) }
                var fetchError by remember { mutableStateOf<String?>(null) }
                val coroutineScope = rememberCoroutineScope()

                // Live-fetched models take priority; fall back to the static known-good list
                // until the user refreshes (or if the fetch fails, e.g. offline/bad key).
                data class ModelOption(val id: String, val label: String, val description: String)
                val modelOptions: List<ModelOption> = fetchedModels?.map {
                    ModelOption(it.id, it.displayName, it.description.ifBlank { it.id })
                } ?: GeminiModel.entries.map { ModelOption(it.id, it.label, it.description) }
                val selectedOption = modelOptions.find { it.id == geminiModelId }
                    ?: ModelOption(geminiModelId, geminiModelId, "")
                var modelMenuExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = modelMenuExpanded,
                        onExpandedChange = { modelMenuExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedOption.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Model") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false }
                        ) {
                            modelOptions.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(model.label, fontWeight = FontWeight.Bold)
                                            if (model.description.isNotBlank()) {
                                                Text(model.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    },
                                    onClick = {
                                        settingsViewModel.updateGeminiModel(model.id)
                                        modelMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            fetchError = null
                            isFetchingModels = true
                            coroutineScope.launch {
                                val outcome = withContext(Dispatchers.IO) {
                                    GeminiClient.listModels(geminiApiKey)
                                }
                                isFetchingModels = false
                                outcome.onSuccess { fetchedModels = it }
                                    .onFailure { fetchError = it.message ?: "Failed to fetch models." }
                            }
                        },
                        enabled = !isFetchingModels
                    ) {
                        if (isFetchingModels) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh model list from Gemini")
                        }
                    }
                }
                if (fetchError != null) {
                    Text(
                        text = "Couldn't fetch live models: $fetchError. Showing last known list instead.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
                Text(
                    text = if (fetchedModels != null)
                        "Live list fetched from your Gemini account just now. Tap refresh anytime to update it (e.g. after Google adds/retires a model)."
                    else
                        "Showing a built-in list. Tap refresh to pull the live, up-to-date list of models available to your API key directly from Google.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            SectionContent(title = "Data Backup & Restore", icon = Icons.Default.History) {
                // Auto Backup (allowBackup) already copies the database and settings to the user's Google account.
                Text(
                    "Automatic: when device backup is on (Settings > Google > Backup), Android copies your trades, journal and settings to your Google account about once a day while charging on Wi-Fi, and restores them when you reinstall or move to a new phone. Use Export for a copy you keep yourself.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { exportLauncher.launch("trading_mate_backup.json") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export Backup")
                        Spacer(Modifier.width(8.dp))
                        Text("Export JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import Backup")
                        Spacer(Modifier.width(8.dp))
                        Text("Import JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    
                    Text(apincer.mobile.tradings.util.AppUtils.getAppVersion(context), fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    
                    Text(stringResource(R.string.label_app_tagline), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    
                    Text(stringResource(R.string.label_copyright), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.about_disclaimer_content),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun SettingsItem(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            onClick = onClick,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    
                    Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black)
                    
                    Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}
