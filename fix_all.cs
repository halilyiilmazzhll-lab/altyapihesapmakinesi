using System;
using System.IO;
using System.Text;
using System.Linq;

public class Program {
    public static void Main() {
        var files = Directory.GetFiles("app/src/main/java/com/example/egimhesabi/ui/screens", "*.kt");
        foreach (var filePath in files) {
            if (filePath.EndsWith("HomeScreen.kt")) continue;
            
            var bytes = File.ReadAllBytes(filePath);
            var str = Encoding.UTF8.GetString(bytes);
            
            if (str.Contains("Ã") || str.Contains("Ä") || str.Contains("Å")) {
                var ansiBytes = Encoding.GetEncoding(1254).GetBytes(str);
                var fixedStr = Encoding.UTF8.GetString(ansiBytes);
                
                File.WriteAllText(filePath, fixedStr, new UTF8Encoding(false));
                Console.WriteLine("Fixed: " + Path.GetFileName(filePath));
            }
        }
    }
}
