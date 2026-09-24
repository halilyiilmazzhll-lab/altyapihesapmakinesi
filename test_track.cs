using System;
using System.IO;
using System.Text;

public class Program {
    public static void Main() {
        var str = File.ReadAllText("app/src/main/java/com/example/egimhesabi/ui/screens/TrackingScreen.kt", Encoding.UTF8);
        var idx = str.IndexOf('Ã');
        if (idx >= 0) Console.WriteLine("Has Ã at " + idx);
        idx = str.IndexOf('Ä');
        if (idx >= 0) Console.WriteLine("Has Ä at " + idx);
        idx = str.IndexOf('Å');
        if (idx >= 0) Console.WriteLine("Has Å at " + idx);
        
        // Also let's check for any 1254 specific chars like 'ý', 'þ', 'ð'
        // which correspond to 'ı', 'ş', 'ğ' in 1254
        if (str.Contains("ý") || str.Contains("þ") || str.Contains("ð")) {
            Console.WriteLine("Has 1254 chars!");
        }
    }
}
