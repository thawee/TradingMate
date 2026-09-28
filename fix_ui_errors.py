import sys

with open('app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt', 'r') as f:
    content = f.read()

# 1. Update AiCopilotCard signature
old_sig = """    cashBalance: Double = 0.0,
    marketRegime: apincer.mobile.tradings.domain.TechnicalAnalysis.MarketRegime = apincer.mobile.tradings.domain.TechnicalAnalysis.MarketRegime.NEUTRAL,
    showSnackbar: (String) -> Unit
) {"""
new_sig = """    cashBalance: Double = 0.0,
    marketRegime: apincer.mobile.tradings.domain.TechnicalAnalysis.MarketRegime = apincer.mobile.tradings.domain.TechnicalAnalysis.MarketRegime.NEUTRAL,
    showSnackbar: (String) -> Unit,
    onAcceptAiPlan: (apincer.mobile.tradings.domain.AiRecommendation) -> Unit = {}
) {"""
content = content.replace(old_sig, new_sig)

# 2. Fix the usage of viewModel in AiCopilotCard body
usage_broken1 = """onAcceptAiPlan = { rec -> viewModel.acceptAiPlan(rec, showSnackbar) }"""
usage_fixed1 = """onAcceptAiPlan = onAcceptAiPlan"""
content = content.replace(usage_broken1, usage_fixed1)

# 3. In DividendAdvisorScreen, when calling AiCopilotCard, pass onAcceptAiPlan
old_call = """        AiCopilotCard(
            playbookMode = playbookMode,
            checklist = checklist,
            onToggleAiDone = {
                viewModel.updateChecklistState {
                    if (playbookMode == PlaybookMode.SWING) it.copy(swingAiDone = !it.swingAiDone)
                    else it.copy(dividendAiDone = !it.dividendAiDone)
                }
            },
            onMarkAiDone = {
                viewModel.updateChecklistState {
                    if (playbookMode == PlaybookMode.SWING) it.copy(swingAiDone = true)
                    else it.copy(dividendAiDone = true)
                }
            },
            watchlist = watchlist,
            portfolioItems = portfolio,
            speculativePlays = speculativePlays,
            isQual = StockDna::isQualityCompany,
            isVal = StockDna::isDeepValue,
            isDiv = StockDna::isDividendCandidate,
            isMom = { s -> StockDna.isSwingCandidate(s, marketRegime == TechnicalAnalysis.MarketRegime.BEAR) },
            isSup = StockDna::isNearSupport,
            isGapUp = StockDna::isGapUpReversal,
            isLiquid = StockDna::isLiquid,
            maxRiskPerTrade = maxRisk,
            minRiskRewardRatio = minRR,
            maxOpenExposure = maxExposure,
            maxPortfolioAllocation = maxStockAlloc,
            maxSectorAllocation = maxSectorAlloc,
            atsEnabled = atsEnabled,
            onValidatedAiResult = { res, plans -> viewModel.recordAiRecommendations(res, plans) },
            apiKey = geminiApiKey,
            cashBalance = cashBalance,
            marketRegime = marketRegime,
            showSnackbar = showSnackbar
        )"""

new_call = old_call.replace(
    "showSnackbar = showSnackbar\n        )",
    "showSnackbar = showSnackbar,\n            onAcceptAiPlan = { rec -> viewModel.acceptAiPlan(rec, showSnackbar) }\n        )"
)

content = content.replace(old_call, new_call)

with open('app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt', 'w') as f:
    f.write(content)

