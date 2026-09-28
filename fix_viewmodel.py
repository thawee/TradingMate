with open('app/src/main/java/apincer/mobile/tradings/ui/StockViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "val existing = repository.allStocks.first().find { it.info.symbol == symbol }?.portfolio?.portfolio",
    "val existing = repository.allStocks.first().find { it.portfolio.symbol == symbol }?.portfolio"
)

with open('app/src/main/java/apincer/mobile/tradings/ui/StockViewModel.kt', 'w') as f:
    f.write(content)
