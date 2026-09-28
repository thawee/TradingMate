import sys

with open('app/src/main/java/apincer/mobile/tradings/ui/StockViewModel.kt', 'r') as f:
    content = f.read()

new_func = """
    fun acceptAiPlan(rec: apincer.mobile.tradings.domain.AiRecommendation, showSnackbar: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val symbol = rec.symbol.uppercase()
                val targetPrice = rec.targetProfit.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                val stopLoss = rec.stopLoss.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                
                if (targetPrice <= 0.0 || stopLoss <= 0.0) {
                    withContext(Dispatchers.Main) {
                        showSnackbar("AI plan missing valid numerical Target or Stop prices.")
                    }
                    return@launch
                }
                
                val existing = repository.allStocks.first().find { it.info.symbol == symbol }?.portfolio?.portfolio
                val entity = existing?.copy(
                    targetPrice = targetPrice,
                    stopLoss = stopLoss,
                    playbookNote = "[AI Plan] ${rec.playbookType}: ${rec.reasoning.take(150)}...",
                    planSource = "GEMINI",
                    planId = java.util.UUID.randomUUID().toString(),
                    planVersion = 1,
                    planCreatedAtMillis = System.currentTimeMillis(),
                    exitPolicy = "FIXED_TARGET",
                    tradePurpose = if (rec.playbookType.contains("Dividend", ignoreCase = true)) "DIVIDEND" else "SWING"
                ) ?: apincer.mobile.tradings.data.PortfolioEntity(
                    symbol = symbol,
                    targetPrice = targetPrice,
                    stopLoss = stopLoss,
                    playbookNote = "[AI Plan] ${rec.playbookType}: ${rec.reasoning.take(150)}...",
                    planSource = "GEMINI",
                    planId = java.util.UUID.randomUUID().toString(),
                    planVersion = 1,
                    planCreatedAtMillis = System.currentTimeMillis(),
                    exitPolicy = "FIXED_TARGET",
                    tradePurpose = if (rec.playbookType.contains("Dividend", ignoreCase = true)) "DIVIDEND" else "SWING"
                )
                
                repository.updatePortfolio(entity)
                
                repository.recordAdviceEvent(apincer.mobile.tradings.data.AdviceEventEntity(
                    symbol = symbol, planId = entity.planId, planVersion = 1,
                    kind = "AI_ACCEPTED", timeMillis = System.currentTimeMillis(),
                    entryPrice = 0.0, stopPrice = stopLoss,
                    targetPrice = targetPrice, quantity = 0,
                    source = "GEMINI", note = "Accepted AI Plan: ${rec.playbookType}"
                ))
                
                withContext(Dispatchers.Main) {
                    showSnackbar("✅ AI Plan Saved for $symbol!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showSnackbar("Failed to accept AI plan: ${e.message}")
                }
            }
        }
    }
"""
content = content.replace("fun fetchStockData(symbol: String) {", new_func + "\n    fun fetchStockData(symbol: String) {")

with open('app/src/main/java/apincer/mobile/tradings/ui/StockViewModel.kt', 'w') as f:
    f.write(content)
