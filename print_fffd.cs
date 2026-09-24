using System;
using System.IO;
using System.Text;

public class Program {
    public static void Main() {
        var lines = File.ReadAllLines(@"app/src/main/java/com/example/egimhesabi/ui/screens/MapScreen.kt", Encoding.UTF8);
        for (int i=0; i<lines.Length; i++) {
            if (lines[i].Contains("\uFFFD")) {
                Console.WriteLine("Line " + (i+1) + ": " + lines[i].Trim());
            }
        }
    }
}
