with open('app/src/main/java/apincer/mobile/tradings/ui/PortfolioScreen.kt', 'r') as f:
    content = f.read()

# Fix PreferenceRepository
content = content.replace("val prefRepo = PreferenceRepository(context, db.checklistDao())", "val prefRepo = PreferenceRepository(context)")

# Move injection
injection = """    val context = LocalContext.current
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
                        val prefRepo = PreferenceRepository(context)
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

vars = """    var symbol by remember { mutableStateOf(initialStock?.info?.symbol ?: "") }
    var entryPrice by remember { mutableStateOf(initialStock?.portfolio?.cost?.toString() ?: "") }
    var qty by remember { mutableStateOf(initialStock?.portfolio?.quantity?.toString() ?: "") }
    
    var targetPrice by remember { mutableStateOf(initialStock?.portfolio?.portfolio?.targetPrice?.let { if (it > 0) it.toString() else "" } ?: "") }
    var stopLossPrice by remember { mutableStateOf(initialStock?.portfolio?.stopLoss?.let { if (it > 0) it.toString() else "" } ?: "") }
    var playbookNote by remember { mutableStateOf(initialStock?.portfolio?.playbookNote ?: "") }
    var tradePurpose by remember { mutableStateOf(initialStock?.portfolio?.tradePurpose ?: "SWING") }
    var recordExecutedFill by remember { mutableStateOf(false) }"""

content = content.replace(injection + "\n" + vars, vars + "\n\n" + injection)

with open('app/src/main/java/apincer/mobile/tradings/ui/PortfolioScreen.kt', 'w') as f:
    f.write(content)
