import sys
with open('app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "AiRecommendationCard(rec)",
    "AiRecommendationCard(rec, onAccept = { viewModel.acceptAiPlan(rec, showSnackbar) })"
)

old_sig = "fun AiRecommendationCard(rec: apincer.mobile.tradings.domain.AiRecommendation) {"
new_sig = "fun AiRecommendationCard(rec: apincer.mobile.tradings.domain.AiRecommendation, onAccept: () -> Unit) {"
content = content.replace(old_sig, new_sig)

old_reasoning = """Text("AI reasoning · unverified; use only the local levels above",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(rec.reasoning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)"""

new_reasoning = """Text("AI reasoning · unverified; use only the local levels above",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(rec.reasoning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Accept AI Plan", fontWeight = FontWeight.Bold)
            }"""

content = content.replace(old_reasoning, new_reasoning)

with open('app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt', 'w') as f:
    f.write(content)
