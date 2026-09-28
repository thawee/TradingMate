import sys

with open('app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt', 'r') as f:
    content = f.read()

# 1. Update AiAnalysisButton signature
sig_old = """fun AiAnalysisButton(
    label: String,
    apiKey: String,
    geminiModelId: String = "gemini-3.6-flash",
    buildPrompt: () -> String,
    emptyMessage: String = "No locally validated setup available. AI explanations are educational only.",
    allowedPlans: Map<String, apincer.mobile.tradings.domain.AiCandidatePlan> = emptyMap(),
    onValidatedResult: (apincer.mobile.tradings.domain.AiAnalysisResult) -> Unit = {},
    onDone: () -> Unit,
    showSnackbar: (String) -> Unit
) {"""
sig_new = """fun AiAnalysisButton(
    label: String,
    apiKey: String,
    geminiModelId: String = "gemini-3.6-flash",
    buildPrompt: () -> String,
    emptyMessage: String = "No locally validated setup available. AI explanations are educational only.",
    allowedPlans: Map<String, apincer.mobile.tradings.domain.AiCandidatePlan> = emptyMap(),
    onValidatedResult: (apincer.mobile.tradings.domain.AiAnalysisResult) -> Unit = {},
    onDone: () -> Unit,
    showSnackbar: (String) -> Unit,
    onAcceptAiPlan: (apincer.mobile.tradings.domain.AiRecommendation) -> Unit = {}
) {"""
content = content.replace(sig_old, sig_new)

# 2. Update AiRecommendationCard call inside AiAnalysisButton
call_old = "AiRecommendationCard(rec, onAccept = { viewModel.acceptAiPlan(rec, showSnackbar) })"
call_new = "AiRecommendationCard(rec, onAccept = { onAcceptAiPlan(rec) })"
content = content.replace(call_old, call_new)

# 3. Add onAcceptAiPlan to the AiAnalysisButton usages
usage1_old = """                AiAnalysisButton(
                    label = "Ask! Google Gemini",
                    apiKey = apiKey,
                    geminiModelId = geminiModelId,
                    buildPrompt = buildSwingPrompt,
                    allowedPlans = allowedPlans,
                    onValidatedResult = { result ->
                        viewModel.recordAiRecommendations(result, allowedPlans)
                    },
                    onDone = onMarkAiDone,
                    showSnackbar = showSnackbar
                )"""
usage1_new = """                AiAnalysisButton(
                    label = "Ask! Google Gemini",
                    apiKey = apiKey,
                    geminiModelId = geminiModelId,
                    buildPrompt = buildSwingPrompt,
                    allowedPlans = allowedPlans,
                    onValidatedResult = { result ->
                        viewModel.recordAiRecommendations(result, allowedPlans)
                    },
                    onDone = onMarkAiDone,
                    showSnackbar = showSnackbar,
                    onAcceptAiPlan = { rec -> viewModel.acceptAiPlan(rec, showSnackbar) }
                )"""
content = content.replace(usage1_old, usage1_new)

usage2_old = """                AiAnalysisButton(
                    label = "Explain dividend candidates",
                    apiKey = apiKey,
                    geminiModelId = geminiModelId,
                    buildPrompt = buildDividendPrompt,
                    emptyMessage = "Dividend analysis is qualitative until a locally validated trade plan is available.",
                    onDone = onMarkAiDone,
                    showSnackbar = showSnackbar
                )"""
usage2_new = """                AiAnalysisButton(
                    label = "Explain dividend candidates",
                    apiKey = apiKey,
                    geminiModelId = geminiModelId,
                    buildPrompt = buildDividendPrompt,
                    emptyMessage = "Dividend analysis is qualitative until a locally validated trade plan is available.",
                    onDone = onMarkAiDone,
                    showSnackbar = showSnackbar,
                    onAcceptAiPlan = { rec -> viewModel.acceptAiPlan(rec, showSnackbar) }
                )"""
content = content.replace(usage2_old, usage2_new)

with open('app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt', 'w') as f:
    f.write(content)
