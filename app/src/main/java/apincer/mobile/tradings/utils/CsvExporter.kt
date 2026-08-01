package apincer.mobile.tradings.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import apincer.mobile.tradings.ui.StockWatchlistInfo
import java.io.File

object CsvExporter {

    fun exportHoldingsToCsv(context: Context, items: List<StockWatchlistInfo>) {
        val fileName = "trading_mate_holdings_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        
        val csvHeader = "Symbol,Name,Quantity,AvgCost,LastPrice,MarketValue,NetProfit,NetProfitPct\n"
        val csvBody = items.joinToString("\n") { item ->
            val qty = item.portfolio.quantity
            val cost = item.portfolio.cost
            val price = item.info.lastPrice
            val value = qty * price
            val profit = (price - cost) * qty
            val profitPct = if (cost > 0) ((price - cost) / cost) * 100 else 0.0

            "\"${item.info.symbol}\",\"${item.info.name ?: ""}\",$qty,%.2f,%.2f,%.2f,%.2f,%.2f%%".format(
                cost, price, value, profit, profitPct
            )
        }

        file.writeText(csvHeader + csvBody)
        shareFile(context, file, "text/csv", "Export Portfolio Holdings")
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
