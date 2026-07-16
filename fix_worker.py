import re

worker_path = '/Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/util/StockAlertWorker.kt'
with open(worker_path, 'r') as f:
    worker_content = f.read()

if "cashTransactionDao = database.cashTransactionDao()" not in worker_content:
    worker_content = worker_content.replace(
        'portfolioSnapshotDao = database.portfolioSnapshotDao()',
        'portfolioSnapshotDao = database.portfolioSnapshotDao(),\n            cashTransactionDao = database.cashTransactionDao()'
    )

with open(worker_path, 'w') as f:
    f.write(worker_content)
