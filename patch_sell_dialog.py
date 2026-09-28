import sys

with open('app/src/main/java/apincer/mobile/tradings/ui/PortfolioScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

injections_top = """import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
"""

if "import androidx.compose.material3.FilterChip" not in content:
    content = content.replace("import androidx.compose.runtime.setValue", "import androidx.compose.runtime.setValue\n" + injections_top)


old_dialog = """    var price by remember(stock.info.symbol) { mutableStateOf(String.format(Locale.ENGLISH, "%.2f", stock.info.lastPrice)) }
    var qty by remember(stock.info.symbol) { mutableStateOf(stock.portfolio.quantity.toString()) }
    var note by remember(stock.info.symbol) { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(
                stringResource(R.string.title_sell_stock, stock.info.symbol),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.label_current_holdings_count, stock.portfolio.quantity), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                TextButton(
                    onClick = { qty = stock.portfolio.quantity.toString() },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Sell All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text(stringResource(R.string.label_sell_price)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("฿ ") },
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = qty,
                onValueChange = { qty = it },
                label = { Text(stringResource(R.string.label_quantity_to_sell)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.label_sell_note)) },
                placeholder = { Text(stringResource(R.string.hint_sell_note)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        }
        
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Spacer(Modifier.width(8.dp))
                val sellQty = qty.toIntOrNull() ?: 0
                val sellPrice = price.toDoubleOrNull() ?: 0.0
                val isValid = sellQty > 0 && sellQty <= stock.portfolio.quantity && sellPrice.isFinite() && sellPrice > 0.0
                Button(
                    enabled = isValid && !isSaving,
                    onClick = { 
                        if (isValid) {
                            onConfirm(stock.info.symbol, sellPrice, sellQty, note)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_confirm_sell), color = Color.White)
                }
            }
        }
    }"""

new_dialog = """    var price by remember(stock.info.symbol) { mutableStateOf(String.format(Locale.ENGLISH, "%.2f", stock.info.lastPrice)) }
    var qty by remember(stock.info.symbol) { mutableStateOf(stock.portfolio.quantity.toString()) }
    var lesson by remember(stock.info.symbol) { mutableStateOf("") }
    var selectedReason by remember(stock.info.symbol) { mutableStateOf("") }

    val exitReasons = listOf("Target Hit 🎯", "Stop Hit 🛡️", "Trailing Stop 📈", "Boredom 😴", "Mistake 🤦", "Fundamental 📜")

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(
                "Trade Autopsy: ${stock.info.symbol}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.label_current_holdings_count, stock.portfolio.quantity), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    TextButton(
                        onClick = { qty = stock.portfolio.quantity.toString() },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Sell All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text(stringResource(R.string.label_sell_price)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        prefix = { Text("฿ ") },
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text(stringResource(R.string.label_quantity_to_sell)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Text("Exit Reason", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(-4.dp)
                ) {
                    exitReasons.forEach { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            label = { Text(reason, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = lesson,
                    onValueChange = { lesson = it },
                    label = { Text("Lesson Learned (Psychological Journal)") },
                    placeholder = { Text("What did you do well? What went wrong?") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 4
                )
            }
        
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Spacer(Modifier.width(8.dp))
                val sellQty = qty.toIntOrNull() ?: 0
                val sellPrice = price.toDoubleOrNull() ?: 0.0
                
                val isNegativeExit = selectedReason.contains("Stop Hit") || selectedReason.contains("Mistake") || selectedReason.contains("Boredom") || sellPrice < stock.portfolio.cost
                val isLessonSufficient = !isNegativeExit || lesson.trim().length >= 10
                
                val isValid = sellQty > 0 && sellQty <= stock.portfolio.quantity && sellPrice.isFinite() && sellPrice > 0.0 && selectedReason.isNotBlank() && isLessonSufficient
                
                Button(
                    enabled = isValid && !isSaving,
                    onClick = { 
                        if (isValid) {
                            val finalNote = "[$selectedReason] $lesson"
                            onConfirm(stock.info.symbol, sellPrice, sellQty, finalNote)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isLessonSufficient) stringResource(R.string.action_confirm_sell) else "Write Lesson First", color = Color.White)
                }
            }
        }
    }"""

content = content.replace(old_dialog, new_dialog)

with open('app/src/main/java/apincer/mobile/tradings/ui/PortfolioScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)

