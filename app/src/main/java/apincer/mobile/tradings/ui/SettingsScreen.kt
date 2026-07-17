package apincer.mobile.tradings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

                val trailingStopPercent by settingsViewModel.trailingStopPercent.collectAsState()
                var editingTrailingStop by remember(trailingStopPercent) { mutableStateOf(trailingStopPercent.toString()) }

                OutlinedTextField(
                    value = editingTrailingStop,
                    onValueChange = { 
                        editingTrailingStop = it
                        it.toDoubleOrNull()?.let { percent -> settingsViewModel.updateTrailingStopPercent(percent) }
                    },
                    label = { Text(stringResource(R.string.label_trailing_stop_percent)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("%") },
                    shape = RoundedCornerShape(14.dp)
                )
                Text(
                    text = stringResource(R.string.desc_trailing_stop_percent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                    label = { Text("Max Portfolio Allocation per Asset") },
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

            SectionContent(title = "App Preferences", icon = Icons.Default.ColorLens) {
                val isPrivacyMode by settingsViewModel.isPrivacyMode.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Privacy Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Mask sensitive value counts and portfolio totals", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isPrivacyMode, onCheckedChange = { settingsViewModel.updatePrivacyMode(it) })
                }
            }

            SectionContent(title = "Data Backup & Restore", icon = Icons.Default.History) {
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
