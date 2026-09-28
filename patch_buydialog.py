import sys
import re

with open('app/src/main/java/apincer/mobile/tradings/ui/PortfolioScreen.kt', 'r') as f:
    content = f.read()

# Add imports
imports = """import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.util.Base64
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import apincer.mobile.tradings.domain.GeminiClient
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.CircularProgressIndicator
import apincer.mobile.tradings.data.PreferenceRepository
"""
content = content.replace("import androidx.compose.runtime.setValue", "import androidx.compose.runtime.setValue\n" + imports)

# Find BuyStockDialog signature and body
func_start = content.find("fun BuyStockDialog(")
body_start = content.find("{", func_start) + 1

injections = """
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isExtracting by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            isExtracting = true
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    
                    withContext(Dispatchers.IO) {
                        val db = apincer.mobile.tradings.data.StockDatabase.getDatabase(context)
                        val prefRepo = PreferenceRepository(context, db.checklistDao())
                        val apiKey = kotlinx.coroutines.flow.first(prefRepo.geminiApiKey)
                        val result = GeminiClient.extractTradeScreenshot(base64, apiKey)
                        
                        withContext(Dispatchers.Main) {
                            if (result.isSuccess) {
                                val data = result.getOrNull()
                                if (data != null) {
                                    symbol = data.symbol
                                    if (data.price > 0) entryPrice = data.price.toString()
                                    if (data.quantity > 0) qty = data.quantity.toString()
                                    recordExecutedFill = true
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Vision failed: " + result.exceptionOrNull()?.message, android.widget.Toast.LENGTH_LONG).show()
                            }
                            isExtracting = false
                        }
                    }
                } catch (e: Exception) {
                    isExtracting = false
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Error reading image", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
"""
content = content[:body_start] + injections + content[body_start:]

title_row = """
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (initialStock == null) stringResource(R.string.title_record_purchase) else stringResource(R.string.title_edit_holding),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                if (initialStock == null) {
                    if (isExtracting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.ImageSearch, contentDescription = "Import Screenshot", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
"""

old_title = """
            Text(
                if (initialStock == null) stringResource(R.string.title_record_purchase) else stringResource(R.string.title_edit_holding),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
"""
content = content.replace(old_title, title_row)

with open('app/src/main/java/apincer/mobile/tradings/ui/PortfolioScreen.kt', 'w') as f:
    f.write(content)
