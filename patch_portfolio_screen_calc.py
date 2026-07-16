file_path = '/Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/PortfolioScreen.kt'
with open(file_path, 'r') as f:
    content = f.read()

if "val cashTransactions by portfolioViewModel.allCashTransactions.collectAsState()" not in content:
    content = content.replace(
        'val cashBalance by portfolioViewModel.cashBalance.collectAsState()',
        'val cashBalance by portfolioViewModel.cashBalance.collectAsState()\n    val cashTransactions by portfolioViewModel.allCashTransactions.collectAsState()'
    )

if "val lifetimeReturn = totalAssetValue - netPrincipal" not in content:
    calc = """
    val netPrincipal = cashTransactions.filter { it.type != "Fee" && it.type != "Dividend" }.sumOf { it.amount }
    val lifetimeReturn = if (netPrincipal != 0.0) totalAssetValue - netPrincipal else 0.0
    val lifetimeReturnPercent = if (netPrincipal > 0) (lifetimeReturn / netPrincipal) * 100 else 0.0
    """
    content = content.replace(
        'val totalAssetValue = totalStockValue + cashBalance',
        'val totalAssetValue = totalStockValue + cashBalance\n' + calc
    )

if "lifetimeReturn = lifetimeReturn," not in content:
    content = content.replace(
        'totalDividendEarned = totalDividendEarned,',
        'totalDividendEarned = totalDividendEarned,\n                    lifetimeReturn = lifetimeReturn,\n                    lifetimeReturnPercent = lifetimeReturnPercent,'
    )

with open(file_path, 'w') as f:
    f.write(content)
print("Patched Screen calculations")
