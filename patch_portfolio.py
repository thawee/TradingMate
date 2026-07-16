import re

with open('/Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/PortfolioScreen.kt', 'r') as f:
    lines = f.readlines()

# Find the start of LazyColumn items block
start_idx = 180  # This is where `item {` starts
end_idx = 417    # This is just before `if (portfolioItems.isEmpty()) {`

# Verify we're on the right track
if "item {" in lines[start_idx] and "if (portfolioItems.isEmpty()) {" in lines[end_idx]:
    new_content = """            item {
                PortfolioSummaryCard(
                    totalAssetValue = totalAssetValue,
                    stockValue = stockValue,
                    cashBalance = cashBalance,
                    grossProfit = grossProfit,
                    totalFees = totalFees,
                    netProfit = netProfitValue,
                    netPercent = totalNetProfitPercent,
                    yieldOnCost = avgYieldOnCost,
                    totalDividendEarned = totalDividendEarned,
                    isPrivacyMode = isPrivacyMode,
                    profitScopeLabel = if (selectedPlaybook == "ALL") null else selectedPlaybook,
                    onEditCash = { showCashDialog = true },
                    onLogDividend = { showDividendDialog = true }
                )
            }

            if (portfolioItems.isNotEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Portfolio Snapshot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            val chartColors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.error,
                                androidx.compose.ui.graphics.Color(0xFFFFA000),
                                androidx.compose.ui.graphics.Color(0xFF00B0FF),
                                androidx.compose.ui.graphics.Color(0xFFE040FB),
                                androidx.compose.ui.graphics.Color(0xFF1DE9B6)
                            )
                            val isMacro = selectedPlaybook == "ALL"
                            val chartValues = if (isMacro) {
                                val swingVal = allPortfolioItems.filter { it.portfolio.tradePurpose == "SWING" }.sumOf { it.info.lastPrice * it.portfolio.quantity }.toFloat()
                                val divVal = allPortfolioItems.filter { it.portfolio.tradePurpose == "DIVIDEND" }.sumOf { it.info.lastPrice * it.portfolio.quantity }.toFloat()
                                listOf(swingVal, divVal, cashBalance.toFloat())
                            } else {
                                portfolioItems.map { (it.info.lastPrice * it.portfolio.quantity).toFloat() }
                            }
                            val centerAmount = if (isMacro) totalAssetValue else stockValue
                            val centerTextStr = if (isPrivacyMode) "฿••••" else "฿${String.format(java.util.Locale.ENGLISH, "%,.0f", centerAmount)}"

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                // Donut chart
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    DonutChart(
                                        values = chartValues,
                                        colors = chartColors,
                                        centerText = centerTextStr,
                                        centerSubText = if (isMacro) "Total Assets" else "Portfolio"
                                    )
                                }

                                Spacer(Modifier.width(16.dp))

                                // Legend + key metrics
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (isMacro) {
                                        val total = chartValues.sum()
                                        listOf("Swing", "Dividend", "Cash").forEachIndexed { i, label ->
                                            val pct = if (total > 0f) (chartValues[i] / total) * 100.0 else 0.0
                                            if (pct > 0.1) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(chartColors[i], CircleShape)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                    Text(
                                                        text = String.format(java.util.Locale.ENGLISH, "%.1f%%", pct),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        portfolioItems.forEachIndexed { i, item ->
                                            val pct = if (stockValue > 0) (item.info.lastPrice * item.portfolio.quantity / stockValue) * 100 else 0.0
                                            if (pct > 0.1) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(chartColors[i % chartColors.size], CircleShape)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(item.info.symbol, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                    Text(
                                                        text = String.format(java.util.Locale.ENGLISH, "%.1f%%", pct),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (targetYearlyDividend > 0 && selectedPlaybook != "SWING") {
                                Spacer(Modifier.height(24.dp))
                                HorizontalDivider(modifier = Modifier.alpha(0.1f))
                                Spacer(Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Dividend Snowball Goal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "฿${String.format(java.util.Locale.ENGLISH, "%,.0f", totalYearlyDividend)} / ฿${String.format(java.util.Locale.ENGLISH, "%,.0f", targetYearlyDividend)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiary,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            "${String.format(java.util.Locale.ENGLISH, "%.1f", dividendProgress * 100)}%",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiary
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                LinearProgressIndicator(
                                    progress = { dividendProgress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                        }
                    }
                }

                item {
                    HoldingsSummaryTable(
                        items = portfolioItems,
                        isPrivacyMode = isPrivacyMode
                    )
                }
            }
"""
    lines[start_idx:end_idx] = [new_content + "\n"]
    with open('/Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/PortfolioScreen.kt', 'w') as f:
        f.writelines(lines)
    print("Successfully patched PortfolioScreen.kt")
else:
    print(f"Failed to find boundaries. Found start: {repr(lines[start_idx])}, Found end: {repr(lines[end_idx])}")
