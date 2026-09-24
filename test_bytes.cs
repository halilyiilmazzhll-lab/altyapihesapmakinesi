using System;
using System.IO;

public class Program {
    public static void Main() {
        var bytes = File.ReadAllBytes("app/src/main/java/com/example/egimhesabi/ui/screens/MapScreen.kt");
        for (int i=0; i<10; i++) Console.Write(bytes[i].ToString("X2") + " ");
    }
}
