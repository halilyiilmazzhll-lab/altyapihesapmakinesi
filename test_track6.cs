using System;
using System.IO;
using System.Text;
using System.Linq;

public class Program {
    public static void Main() {
        var files = new[] { "HistoryScreen.kt", "SlopeCalculatorScreen.kt", "OrderBookScreen.kt" };
        foreach (var file in files) {
            var str = File.ReadAllText("app/src/main/java/com/example/egimhesabi/ui/screens/" + file, Encoding.UTF8);
            if (str.Contains("ı") || str.Contains("ş") || str.Contains("ğ") || str.Contains("ü") || str.Contains("ç") || str.Contains("ö") || str.Contains("İ") || str.Contains("Ş") || str.Contains("Ğ") || str.Contains("Ü") || str.Contains("Ç") || str.Contains("Ö")) {
                Console.WriteLine(file + " has valid Turkish chars!");
            } else {
                Console.WriteLine(file + " NO valid Turkish chars!");
                // Let's check for any 1254 weird things that didn't have Ã
                // For instance, 'ı' is FD, which is 'ý' in 1254.
                if (str.Contains("ý") || str.Contains("þ") || str.Contains("ð")) {
                    Console.WriteLine(file + " has 1254 corrupted chars!");
                }
                
                // Let's check for U+FFFD
                if (str.Contains("\uFFFD")) {
                    Console.WriteLine(file + " has U+FFFD!");
                }
            }
        }
    }
}
