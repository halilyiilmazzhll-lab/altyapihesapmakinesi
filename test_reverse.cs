using System;
using System.IO;
using System.Text;

public class Program {
    public static void Main() {
        var filePath = "app/src/main/java/com/example/egimhesabi/ui/screens/InterpolationScreen.kt";
        var bytes = File.ReadAllBytes(filePath);
        var str = Encoding.UTF8.GetString(bytes);
        
        // Let's try to reverse it
        var ansiBytes = Encoding.GetEncoding(1254).GetBytes(str);
        var fixedStr = Encoding.UTF8.GetString(ansiBytes);
        
        // Check if there are still any weird chars in fixedStr
        if (fixedStr.Contains("Ã") || fixedStr.Contains("Ä")) {
            Console.WriteLine("Still contains weird chars!");
        } else {
            Console.WriteLine("Reversal looks clean!");
            File.WriteAllText(filePath + ".fixed", fixedStr, new UTF8Encoding(false));
        }
    }
}
