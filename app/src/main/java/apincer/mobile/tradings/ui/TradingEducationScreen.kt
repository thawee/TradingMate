package apincer.mobile.tradings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingEducationScreen(onBack: () -> Unit) {
    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.title_academy), fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = stringResource(R.string.edu_workflow_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = stringResource(R.string.edu_workflow_subtitle),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                // --- SECTION 1: THE BASICS (Trading Psychology) ---
                SectionHeader("The Basics", Icons.Default.Shield, color = MaterialTheme.colorScheme.error)
                
                GlassCard(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Warning, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                stringResource(R.string.edu_golden_rule_title), 
                                fontWeight = FontWeight.Black, 
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.edu_golden_rule_content),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                EducationGlassCard(
                    title = stringResource(R.string.edu_risk_mgmt_title),
                    content = stringResource(R.string.edu_risk_mgmt_content),
                    color = MaterialTheme.colorScheme.error,
                    icon = Icons.Default.ContentCut
                )

                // --- SECTION 2: THE 5-LAYER FILTER SYSTEM ---
                SectionHeader(stringResource(R.string.title_education_concept), Icons.Default.Layers, color = MaterialTheme.colorScheme.tertiary)
                
                Text(
                    text = stringResource(R.string.edu_5_layers_intro),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                EducationGlassCard(
                    title = stringResource(R.string.edu_layer_1_title),
                    content = stringResource(R.string.edu_layer_1_content),
                    color = Color(0xFF4CAF50),
                    icon = Icons.Default.Verified
                )
                EducationGlassCard(
                    title = stringResource(R.string.edu_layer_2_title),
                    content = stringResource(R.string.edu_layer_2_content),
                    color = Color(0xFF2196F3),
                    icon = Icons.Default.AccountBalanceWallet
                )
                EducationGlassCard(
                    title = stringResource(R.string.edu_layer_3_title),
                    content = stringResource(R.string.edu_layer_3_content),
                    color = Color(0xFFFF9800),
                    icon = Icons.Default.Savings
                )
                EducationGlassCard(
                    title = stringResource(R.string.edu_layer_4_title),
                    content = stringResource(R.string.edu_layer_4_content),
                    color = Color(0xFF9C27B0),
                    icon = Icons.AutoMirrored.Filled.TrendingUp
                )
                EducationGlassCard(
                    title = stringResource(R.string.edu_layer_5_title),
                    content = stringResource(R.string.edu_layer_5_content),
                    color = Color(0xFFF44336),
                    icon = Icons.AutoMirrored.Filled.ShowChart
                )

                // --- SECTION 3: THE SENSORS ---
                SectionHeader(stringResource(R.string.section_the_sensors), Icons.Default.Lightbulb, color = MaterialTheme.colorScheme.secondary)

                EducationGlassCard(
                    title = stringResource(R.string.edu_rsi_title),
                    content = stringResource(R.string.edu_rsi_content),
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.Speed
                )

                EducationGlassCard(
                    title = stringResource(R.string.edu_macd_title),
                    content = stringResource(R.string.edu_macd_content),
                    color = MaterialTheme.colorScheme.tertiary,
                    icon = Icons.AutoMirrored.Filled.CompareArrows
                )

                EducationGlassCard(
                    title = stringResource(R.string.edu_sma_title),
                    content = stringResource(R.string.edu_sma_content),
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.AutoMirrored.Filled.TrendingUp
                )

                EducationGlassCard(
                    title = stringResource(R.string.edu_bollinger_title),
                    content = stringResource(R.string.edu_bollinger_content),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    icon = Icons.Default.BlurOn
                )

                // --- SECTION 4: THE WORKFLOW ---
                SectionHeader(stringResource(R.string.section_success_path), Icons.AutoMirrored.Filled.MenuBook, color = MaterialTheme.colorScheme.primary)

                EducationGlassCard(
                    title = stringResource(R.string.edu_step1_title),
                    content = stringResource(R.string.edu_step1_content),
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.Search
                )

                EducationGlassCard(
                    title = stringResource(R.string.edu_step2_title),
                    content = stringResource(R.string.edu_step2_content),
                    color = MaterialTheme.colorScheme.secondary,
                    icon = Icons.Default.Traffic
                )

                EducationGlassCard(
                    title = stringResource(R.string.edu_step3_title),
                    content = stringResource(R.string.edu_step3_content),
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.AssignmentTurnedIn
                )

                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.action_ready_to_trade), fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun EducationGlassCard(title: String, content: String, color: Color, icon: ImageVector) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.1f), shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title, 
                    fontWeight = FontWeight.Black, 
                    fontSize = 17.sp, 
                    color = color,
                    letterSpacing = (-0.5).sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = content, 
                fontSize = 14.sp, 
                lineHeight = 22.sp, 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
