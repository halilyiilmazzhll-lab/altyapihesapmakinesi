using System;
using System.IO;
using System.Text;

public class Program {
    public static void Main() {
        var str = File.ReadAllText("app/src/main/java/com/example/egimhesabi/ui/screens/InterpolationScreen.kt", Encoding.UTF8);
        if (str.Contains("E?im")) Console.WriteLine("YES, it contains E?im");
        if (str.Contains("Δ")) Console.WriteLine("Contains delta!");
        if (str.Contains("?h")) Console.WriteLine("Contains ?h");
    }
}
