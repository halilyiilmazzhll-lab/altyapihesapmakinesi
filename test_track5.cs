using System;
using System.IO;
using System.Text;
using System.Linq;

public class Program {
    public static void Main() {
        var str = File.ReadAllText("app/src/main/java/com/example/egimhesabi/ui/screens/TrackingScreen.kt", Encoding.UTF8);
        if (str.Contains("Ã")) Console.WriteLine("YES A!");
        else Console.WriteLine("NO A!");
    }
}
