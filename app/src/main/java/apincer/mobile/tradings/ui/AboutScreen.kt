package apincer.mobile.tradings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("About TradingMate", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "TradingMate",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = "Version 1.0.0", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(32.dp))

            AboutSection(
                title = "The Idea",
                icon = Icons.Default.EmojiObjects,
                content = "TradingMate was born from the need for a disciplined, emotionless approach to stock trading. It aims to replace guesswork with data-driven signals."
            )

            Spacer(modifier = Modifier.height(24.dp))

            AboutSection(
                title = "The Concept",
                icon = Icons.Default.Info,
                content = "Our philosophy is 'Quality First'. We focus on dividend stocks with solid fundamentals, monitored through indicators like RSI and MACD to find high-probability zones."
            )

            Spacer(modifier = Modifier.height(24.dp))

            AboutSection(
                title = "Built with AI",
                icon = Icons.Default.AutoAwesome,
                content = "This application is built through human-AI collaboration. It was architected and implemented with the assistance of Google's Gemini AI."
            )

            Spacer(modifier = Modifier.height(24.dp))

            AboutSection(
                title = "The Creator",
                icon = Icons.Default.Person,
                content = "Developed by Thawee P., a software engineer and trading enthusiast passionate about building tools for financial empowerement."
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "© 2024 TradingMate Project",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Happy Trading! 🚀",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AboutSection(title: String, icon: ImageVector, content: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = content, fontSize = 14.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
