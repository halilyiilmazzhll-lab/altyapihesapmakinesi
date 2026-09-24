import codecs

path = 'app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = '''        } else {
            // Helper text for connection mode
            Text(
                text = if (sourceRecord == null) "Bağlantının başlayacağı bacaya dokunun" else "Bağlanacak hedef bacaya dokunun (Temizlemek için kendisine tekrar dokunun)",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .background(Color(0xFF007AFF).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}'''
repl = '''        } else {
            // Helper text for connection mode
            Text(
                text = if (sourceRecord == null) "Bağlantının başlayacağı bacaya dokunun" else "Bağlanacak hedef bacaya dokunun (Temizlemek için kendisine tekrar dokunun)",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .background(Color(0xFF007AFF).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
    }
}'''

# Wait, the unicode characters are broken in my output.
# I will just use regex to add a brace before the last }.
import re
content = re.sub(r'\}\n\}$', '    }\n}\n}', content.strip())

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Added brace to StakeoutMapView")