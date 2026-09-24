using System;
using System.Text;

public class Program {
    public static void Main() {
        var enc = Encoding.GetEncoding(1254);
        var bytes = new byte[] { 0xC3, 0xA7 }; // UTF-8 for 'ç'
        var str = enc.GetString(bytes); // Should be "Ã§"
        Console.WriteLine(str);
        
        var bytesBack = enc.GetBytes(str);
        Console.WriteLine(bytesBack[0] == 0xC3 && bytesBack[1] == 0xA7);
    }
}
