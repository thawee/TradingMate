import re

# 1. Fix TradingMateApp (remove adviceEventDao)
with open('app/src/main/java/apincer/mobile/tradings/TradingMateApp.kt', 'r') as f:
    app_content = f.read()

app_content = app_content.replace(",\n                    adviceEventDao = db.adviceEventDao()", "")
with open('app/src/main/java/apincer/mobile/tradings/TradingMateApp.kt', 'w') as f:
    f.write(app_content)

# 2. Fix ViewModels
import glob
for file in glob.glob('app/src/main/java/apincer/mobile/tradings/ui/*ViewModel.kt'):
    with open(file, 'r') as f:
        content = f.read()
    
    # Change get() = application.appRepository to get() = getApplication<apincer.mobile.tradings.TradingMateApp>().repository
    content = content.replace("get() = application.appRepository", "get() = getApplication<apincer.mobile.tradings.TradingMateApp>().repository")
    with open(file, 'w') as f:
        f.write(content)

# 3. Fix SettingsScreen Icons and Intent
with open('app/src/main/java/apincer/mobile/tradings/ui/SettingsScreen.kt', 'r') as f:
    settings_content = f.read()

settings_content = settings_content.replace("Icons.Default.Build", "Icons.Default.Settings")
with open('app/src/main/java/apincer/mobile/tradings/ui/SettingsScreen.kt', 'w') as f:
    f.write(settings_content)

