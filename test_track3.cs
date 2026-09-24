using System;
using System.IO;
using System.Text;
using System.Linq;

public class Program {
    public static void Main() {
        var str = File.ReadAllText("app/src/main/java/com/example/egimhesabi/ui/screens/TrackingScreen.kt", Encoding.UTF8);
        var lines = str.Split('\n');
        foreach (var line in lines) {
            if (line.Contains("Men")) {
                Console.WriteLine("Line: " + line.Trim());
                foreach (char c in line.Trim()) {
                    Console.WriteLine(c + " : " + ((int)c).ToString("X4"));
                }
                break;
            }
        }
    }
}
