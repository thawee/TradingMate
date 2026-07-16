import re

# 1. Update StockRepository.kt
repo_path = '/Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/data/StockRepository.kt'
with open(repo_path, 'r') as f:
    repo_content = f.read()

if "val cashTransactionDao: CashTransactionDao" not in repo_content:
    repo_content = repo_content.replace(
        'private val portfolioSnapshotDao: PortfolioSnapshotDao',
        'private val portfolioSnapshotDao: PortfolioSnapshotDao,\n    private val cashTransactionDao: CashTransactionDao'
    )
    
if "allCashTransactions" not in repo_content:
    repo_content = repo_content.replace(
        'val allSnapshots: Flow<List<PortfolioSnapshotEntity>> = portfolioSnapshotDao.getAllSnapshots()',
        'val allSnapshots: Flow<List<PortfolioSnapshotEntity>> = portfolioSnapshotDao.getAllSnapshots()\n    val allCashTransactions: Flow<List<CashTransactionEntity>> = cashTransactionDao.getAllTransactions()'
    )

if "suspend fun updateCash(balance: Double)" in repo_content:
    repo_content = repo_content.replace(
        'suspend fun updateCash(balance: Double) {',
        'suspend fun updateCash(balance: Double, reason: String = "Set Balance") {\n        database.withTransaction {\n            val current = cashDao.getCashSync()?.balance ?: 0.0\n            val diff = balance - current\n            if (diff != 0.0) {\n                cashTransactionDao.insertTransaction(CashTransactionEntity(amount = diff, type = reason))\n            }\n            cashDao.updateCash(CashEntity(balance = balance))\n        }\n    }'
    )
    # Remove the old updateCash body
    repo_content = re.sub(r'suspend fun updateCash\(balance: Double, reason: String = "Set Balance"\) \{.*?\}\n    \}', 
                          'suspend fun updateCash(balance: Double, reason: String = "Set Balance") {\n        database.withTransaction {\n            val current = cashDao.getCashSync()?.balance ?: 0.0\n            val diff = balance - current\n            if (diff != 0.0) {\n                cashTransactionDao.insertTransaction(CashTransactionEntity(amount = diff, type = reason))\n            }\n            cashDao.updateCash(CashEntity(balance = balance))\n        }\n    }', repo_content, flags=re.DOTALL)


if "suspend fun adjustCashBy(amount: Double)" in repo_content:
    repo_content = repo_content.replace(
        'suspend fun adjustCashBy(amount: Double) {',
        'suspend fun adjustCashBy(amount: Double, reason: String = "Adjustment") {\n        database.withTransaction {\n            if (amount < 0) {\n                val current = cashDao.getCashSync()\n                if (current != null && current.balance + amount < 0) {\n                    throw IllegalStateException("Insufficient balance: has ${current.balance}, needs ${-amount}")\n                }\n            }\n            cashTransactionDao.insertTransaction(CashTransactionEntity(amount = amount, type = reason))\n            cashDao.adjustCashBy(amount)\n        }\n    }'
    )
    repo_content = re.sub(r'suspend fun adjustCashBy\(amount: Double, reason: String = "Adjustment"\) \{.*?\}\n    \}', 
                          'suspend fun adjustCashBy(amount: Double, reason: String = "Adjustment") {\n        database.withTransaction {\n            if (amount < 0) {\n                val current = cashDao.getCashSync()\n                if (current != null && current.balance + amount < 0) {\n                    throw IllegalStateException("Insufficient balance: has ${current.balance}, needs ${-amount}")\n                }\n            }\n            cashTransactionDao.insertTransaction(CashTransactionEntity(amount = amount, type = reason))\n            cashDao.adjustCashBy(amount)\n        }\n    }', repo_content, flags=re.DOTALL)

with open(repo_path, 'w') as f:
    f.write(repo_content)


# 2. Update TradingMateApp.kt
app_path = '/Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/TradingMateApp.kt'
with open(app_path, 'r') as f:
    app_content = f.read()

if "cashTransactionDao = database.cashTransactionDao()" not in app_content:
    app_content = app_content.replace(
        'portfolioSnapshotDao = database.portfolioSnapshotDao()',
        'portfolioSnapshotDao = database.portfolioSnapshotDao(),\n            cashTransactionDao = database.cashTransactionDao()'
    )

with open(app_path, 'w') as f:
    f.write(app_content)


# 3. Update StockAlertWorker.kt
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

print("Patched Repos and apps")
