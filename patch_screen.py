import re

file_path = '/Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/PortfolioScreen.kt'
with open(file_path, 'r') as f:
    content = f.read()

content = content.replace(
    'portfolioViewModel.updateCashBalance(amount)',
    'portfolioViewModel.updateCashBalance(amount, reason)'
)
content = content.replace(
    'portfolioViewModel.adjustCash(amount)',
    'portfolioViewModel.adjustCash(amount, reason)'
)

with open(file_path, 'w') as f:
    f.write(content)

# Update PortfolioViewModel
vm_path = '/Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/PortfolioViewModel.kt'
with open(vm_path, 'r') as f:
    vm_content = f.read()

if "fun updateCashBalance(amount: Double, reason: String = \"Set Balance\")" not in vm_content:
    vm_content = vm_content.replace(
        'fun updateCashBalance(amount: Double)',
        'fun updateCashBalance(amount: Double, reason: String = "Set Balance")'
    )
    vm_content = vm_content.replace(
        'repository.updateCash(amount)',
        'repository.updateCash(amount, reason)'
    )
    vm_content = vm_content.replace(
        'fun adjustCash(amount: Double)',
        'fun adjustCash(amount: Double, reason: String = "Adjustment")'
    )
    vm_content = vm_content.replace(
        'repository.adjustCashBy(amount)',
        'repository.adjustCashBy(amount, reason)'
    )

if "allCashTransactions" not in vm_content:
    transactions_val = """
    val allCashTransactions: StateFlow<List<apincer.mobile.tradings.data.CashTransactionEntity>> =
        repository.allCashTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    """
    vm_content = vm_content.replace(
        'initialValue = emptyList()\n        )',
        'initialValue = emptyList()\n        )' + transactions_val,
        1
    )

with open(vm_path, 'w') as f:
    f.write(vm_content)

print("Patched Screen and ViewModel")
