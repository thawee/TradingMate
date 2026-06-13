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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: StockViewModel
) {
    val context = LocalContext.current
    var showConceptDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let {
                viewModel.exportBackup(
                    contentResolver = context.contentResolver,
                    uri = it,
                    onSuccess = {
                        android.widget.Toast.makeText(context, "Backup exported successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        android.widget.Toast.makeText(context, "Export failed: ${err.message}", android.widget.Toast.LENGTH_LONG).show()
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
                        android.widget.Toast.makeText(context, "Backup restored successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        android.widget.Toast.makeText(context, "Restore failed: ${err.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    )

    if (showConceptDialog) {
        ConceptDialog(onDismiss = { showConceptDialog = false })
    }

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
                val targetDividend by viewModel.targetMonthlyDividend.collectAsState()
                var editingTarget by remember(targetDividend) { mutableStateOf(targetDividend.toInt().toString()) }

                OutlinedTextField(
                    value = editingTarget,
                    onValueChange = { 
                        editingTarget = it
                        it.toDoubleOrNull()?.let { amount ->
                            viewModel.updateTargetMonthlyDividend(amount)
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
                val alertThreshold by viewModel.priceAlertThreshold.collectAsState()
                var editingThreshold by remember(alertThreshold) { mutableStateOf(alertThreshold.toInt().toString()) }

                OutlinedTextField(
                    value = editingThreshold,
                    onValueChange = { 
                        editingThreshold = it
                        it.toDoubleOrNull()?.let { percent ->
                            viewModel.updatePriceAlertThreshold(percent)
                        }
                    },
                    label = { Text(stringResource(R.string.label_alert_threshold_percent)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("%") },
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                val dividendWindow by viewModel.dividendAlertWindow.collectAsState()
                var editingWindow by remember(dividendWindow) { mutableStateOf(dividendWindow.toString()) }
                val isEndYear by viewModel.isDividendAlertEndYear.collectAsState()

                OutlinedTextField(
                    value = editingWindow,
                    onValueChange = { 
                        editingWindow = it
                        it.toIntOrNull()?.let { days ->
                            viewModel.updateDividendAlertWindow(days)
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
                    Switch(checked = isEndYear, onCheckedChange = { viewModel.toggleDividendAlertEndYear() })
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showConceptDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.School, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_read_concept), fontWeight = FontWeight.Bold)
            }

            SectionContent(title = "Risk Management", icon = Icons.Default.Warning) {
                val maxRiskPerTrade by viewModel.maxRiskPerTrade.collectAsState()
                var editingMaxRisk by remember(maxRiskPerTrade) { mutableStateOf(maxRiskPerTrade.toString()) }

                OutlinedTextField(
                    value = editingMaxRisk,
                    onValueChange = { 
                        editingMaxRisk = it
                        it.toDoubleOrNull()?.let { percent -> viewModel.updateMaxRiskPerTrade(percent) }
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

                val maxOpenExposure by viewModel.maxOpenExposure.collectAsState()
                var editingMaxOpen by remember(maxOpenExposure) { mutableStateOf(maxOpenExposure.toString()) }

                OutlinedTextField(
                    value = editingMaxOpen,
                    onValueChange = { 
                        editingMaxOpen = it
                        it.toDoubleOrNull()?.let { percent -> viewModel.updateMaxOpenExposure(percent) }
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

                val maxAllocation by viewModel.maxPortfolioAllocation.collectAsState()
                var editingMaxAlloc by remember(maxAllocation) { mutableStateOf(maxAllocation.toString()) }

                OutlinedTextField(
                    value = editingMaxAlloc,
                    onValueChange = { 
                        editingMaxAlloc = it
                        it.toDoubleOrNull()?.let { percent -> viewModel.updateMaxPortfolioAllocation(percent) }
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

                val minRR by viewModel.minRiskRewardRatio.collectAsState()
                var editingMinRR by remember(minRR) { mutableStateOf(minRR.toString()) }

                OutlinedTextField(
                    value = editingMinRR,
                    onValueChange = { 
                        editingMinRR = it
                        it.toDoubleOrNull()?.let { ratio -> viewModel.updateMinRiskRewardRatio(ratio) }
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

            SectionContent(title = "App Preferences", icon = Icons.Default.ColorLens) {
                val isPrivacyMode by viewModel.isPrivacyMode.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Privacy Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Mask sensitive value counts and portfolio totals", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isPrivacyMode, onCheckedChange = { viewModel.togglePrivacyMode() })
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
                    
                    Text(stringResource(R.string.label_app_version), fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    
                    Text(stringResource(R.string.label_app_tagline), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    
                    Text(stringResource(R.string.label_copyright), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConceptDialog(onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.title_education_concept),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    androidx.compose.material3.IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.edu_5_layers_intro),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    ConceptLayerItem(
                        title = stringResource(R.string.edu_layer_1_title),
                        content = stringResource(R.string.edu_layer_1_content),
                        icon = Icons.Default.Verified,
                        color = Color(0xFF4CAF50)
                    )
                    ConceptLayerItem(
                        title = stringResource(R.string.edu_layer_2_title),
                        content = stringResource(R.string.edu_layer_2_content),
                        icon = Icons.Default.AccountBalanceWallet,
                        color = Color(0xFF2196F3)
                    )
                    ConceptLayerItem(
                        title = stringResource(R.string.edu_layer_3_title),
                        content = stringResource(R.string.edu_layer_3_content),
                        icon = Icons.Default.Savings,
                        color = Color(0xFFFF9800)
                    )
                    ConceptLayerItem(
                        title = stringResource(R.string.edu_layer_4_title),
                        content = stringResource(R.string.edu_layer_4_content),
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFF9C27B0)
                    )
                    ConceptLayerItem(
                        title = stringResource(R.string.edu_layer_5_title),
                        content = stringResource(R.string.edu_layer_5_content),
                        icon = Icons.Default.ShowChart,
                        color = Color(0xFFF44336)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun ConceptLayerItem(title: String, content: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.padding(8.dp)
            )
        }
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
