import re

file_path = '/Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/StockComponents.kt'
with open(file_path, 'r') as f:
    content = f.read()

if "lifetimeReturn: Double = 0.0" not in content:
    content = content.replace(
        'totalDividendEarned: Double = 0.0,',
        'totalDividendEarned: Double = 0.0,\n    lifetimeReturn: Double = 0.0,\n    lifetimeReturnPercent: Double = 0.0,'
    )

if "Lifetime P/L" not in content:
    lifetime_ui = """
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Lifetime Return", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (isPrivacyMode) "฿••••" else "฿${String.format(java.util.Locale.ENGLISH, "%,.2f", lifetimeReturn)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lifetimeReturn >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isPrivacyMode) "(••••%)" else "${String.format(java.util.Locale.ENGLISH, "%+.2f", lifetimeReturnPercent)}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lifetimeReturn >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
"""
    content = content.replace(
        'Spacer(Modifier.height(12.dp))\n            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {\n                Text("Gross',
        lifetime_ui.strip() + '\n            Spacer(Modifier.height(12.dp))\n            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {\n                Text("Gross'
    )

with open(file_path, 'w') as f:
    f.write(content)
print("Patched StockComponents.kt")
