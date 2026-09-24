using System;
using System.IO;
using System.Text;
using System.Linq;

public class Program {
    public static void Main() {
        var str = File.ReadAllText("app/src/main/java/com/example/egimhesabi/ui/screens/TrackingScreen.kt", Encoding.UTF8);
        var lines = str.Split('\n');
        foreach (var line in lines) {
            if (line.Contains("İ") || line.Contains("ı") || line.Contains("ö") || line.Contains("ç") || line.Contains("ş") || line.Contains("ğ")) {
                Console.WriteLine(line.Trim());
                break;
            }
        }
    }
}
