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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: StockViewModel
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackup(context.contentResolver, it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(context.contentResolver, it) }
    }

    if (showDeleteConfirm) {
        GlassDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = stringResource(R.string.title_clear_all_data),
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.clearWatchlist()
                        viewModel.clearTradeHistory()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_clear_everything), color = Color.White)
                }
            },
            dismissButton = {
                
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            
            Text(stringResource(R.string.confirm_clear_all_data))
        }
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
            SectionContent(title = stringResource(R.string.section_data_management), icon = Icons.Default.Info) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsItem(
                        title = stringResource(R.string.action_backup_data),
                        description = stringResource(R.string.label_backup_desc),
                        icon = Icons.Default.FileDownload,
                        onClick = { exportLauncher.launch("trading_mate_backup_${System.currentTimeMillis()}.json") }
                    )
                    
                    SettingsItem(
                        title = stringResource(R.string.action_restore_data),
                        description = stringResource(R.string.label_restore_desc),
                        icon = Icons.Default.FileUpload,
                        onClick = { importLauncher.launch(arrayOf("application/json")) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.title_clear_all_data), fontWeight = FontWeight.Black)
                    }
                }
            }

            SectionContent(title = stringResource(R.string.section_appearance), icon = Icons.Default.ColorLens) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                ) {
                    
                    Text(
                        stringResource(R.string.label_dynamic_theme), 
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
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
