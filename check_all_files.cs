using System;
using System.IO;
using System.Text;

public class Program {
    public static void Main() {
        var dir = @"app/src/main/java/com/example/egimhesabi";
        var files = Directory.GetFiles(dir, "*.kt", SearchOption.AllDirectories);
        bool foundAny = false;
        
        foreach (var file in files) {
            var content = File.ReadAllText(file, Encoding.UTF8);
            if (content.Contains("Ã") || content.Contains("Ä") || content.Contains("Å")) {
                Console.WriteLine("CORRUPTED: " + file);
                foundAny = true;
            }
        }
        
        if (!foundAny) {
            Console.WriteLine("ALL FILES ARE CLEAN!");
        }
    }
}
