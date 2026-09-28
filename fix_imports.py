with open('app/src/main/java/apincer/mobile/tradings/ui/PortfolioScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

imports_to_add = """import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
"""

if "import androidx.compose.foundation.layout.ExperimentalLayoutApi" not in content:
    content = content.replace("import androidx.compose.foundation.layout.Box", "import androidx.compose.foundation.layout.Box\n" + imports_to_add)

with open('app/src/main/java/apincer/mobile/tradings/ui/PortfolioScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
