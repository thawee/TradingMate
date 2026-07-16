file_path = '/Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/PortfolioViewModel.kt'
with open(file_path, 'r') as f:
    content = f.read()

content = content.replace(
    'repository.adjustCashBy(totalReceived)',
    'repository.adjustCashBy(totalReceived, "Dividend")'
)

with open(file_path, 'w') as f:
    f.write(content)
