using System;
using System.IO;
using System.Text;

public class Program {
    public static void Main() {
        var str = File.ReadAllText(@"app/src/main/java/com/example/egimhesabi/Navigation.kt", Encoding.UTF8);
        if (str.Contains("ı") || str.Contains("ş") || str.Contains("ğ") || str.Contains("ü") || str.Contains("ç") || str.Contains("ö") || str.Contains("İ") || str.Contains("Ş") || str.Contains("Ğ") || str.Contains("Ü") || str.Contains("Ç") || str.Contains("Ö")) {
            Console.WriteLine("Navigation.kt has valid Turkish chars!");
        } else {
            Console.WriteLine("Navigation.kt has NO valid Turkish chars!");
            if (str.Contains("") || str.Contains("\uFFFD")) {
                Console.WriteLine("BUT IT HAS REPLACEMENT CHARS!");
            }
        }
    }
}
