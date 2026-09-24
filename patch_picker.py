import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/components/StakeoutPicker.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

pattern = re.compile(r'    Column\(modifier = modifier\.fillMaxWidth\(\)\) \{.*?\}\n    \}\n', re.DOTALL)

replacement = '''    IconButton(
        onClick = { 
            if (StakeoutSession.lastNeighborhoodId == null) errorDialog = true 
            else showDialog = true 
        },
        modifier = modifier
            .background(Color(0xFFF0F4F9), RoundedCornerShape(8.dp))
            .size(32.dp)
    ) {
        Icon(Icons.Default.List, contentDescription = "Tutanaktan seç", tint = AccentOrange, modifier = Modifier.size(18.dp))
    }
'''

content = pattern.sub(replacement, content, count=1)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Successfully patched StakeoutPicker.kt')