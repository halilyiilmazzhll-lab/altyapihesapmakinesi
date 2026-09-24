using System;
using System.IO;
using System.Text;

public class Program {
    public static void Main() {
        var bytes = File.ReadAllBytes("app/src/main/java/com/example/egimhesabi/ui/screens/MapScreen.kt");
        var str = Encoding.UTF8.GetString(bytes);
        
        // str is what's currently in the file.
        // It was originally read using default ANSI encoding (1254 in Turkey)
        // and written using UTF8 without BOM.
        
        // Can we get the ANSI bytes back?
        var ansiBytes = Encoding.GetEncoding(1254).GetBytes(str);
        
        // Then parse those ANSI bytes as UTF8
        var originalStr = Encoding.UTF8.GetString(ansiBytes);
        
        Console.WriteLine(originalStr.Substring(originalStr.IndexOf("ManholeStatusSe"), 200));
    }
}
