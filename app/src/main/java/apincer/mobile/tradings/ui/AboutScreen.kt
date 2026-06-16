package apincer.mobile.tradings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import apincer.mobile.tradings.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.title_about), fontWeight = FontWeight.Black) },
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                GlassCard(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(32.dp),
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = apincer.mobile.tradings.util.AppUtils.getAppVersion(context), 
                        fontSize = 14.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                AboutSectionCard(
                    title = stringResource(R.string.about_idea_title),
                    icon = Icons.Default.EmojiObjects,
                    content = stringResource(R.string.about_idea_content)
                )

                AboutSectionCard(
                    title = stringResource(R.string.about_concept_title),
                    icon = Icons.Default.Info,
                    content = stringResource(R.string.about_concept_content)
                )

                AboutSectionCard(
                    title = stringResource(R.string.about_ai_title),
                    icon = Icons.Default.AutoAwesome,
                    content = stringResource(R.string.about_ai_content)
                )

                AboutSectionCard(
                    title = stringResource(R.string.about_creator_title),
                    icon = Icons.Default.Person,
                    content = stringResource(R.string.about_creator_content)
                )

                AboutSectionCard(
                    title = stringResource(R.string.about_disclaimer_title),
                    icon = Icons.Default.Info,
                    content = stringResource(R.string.about_disclaimer_content)
                )

                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_copyright),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.about_tagline),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AboutSectionCard(title: String, icon: ImageVector, content: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title, 
                    fontWeight = FontWeight.Black, 
                    fontSize = 18.sp,
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
