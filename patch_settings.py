with open('app/src/main/java/apincer/mobile/tradings/ui/SettingsScreen.kt', 'r') as f:
    content = f.read()

import_to_add = "import android.content.Intent\nimport androidx.compose.material.icons.filled.Settings\n"
if "import android.content.Intent" not in content:
    content = content.replace("import androidx.activity.compose.rememberLauncherForActivityResult", import_to_add + "import androidx.activity.compose.rememberLauncherForActivityResult")

with open('app/src/main/java/apincer/mobile/tradings/ui/SettingsScreen.kt', 'w') as f:
    f.write(content)
