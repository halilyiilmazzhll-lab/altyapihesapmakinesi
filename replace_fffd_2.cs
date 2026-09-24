using System;
using System.IO;
using System.Text;
using System.Collections.Generic;

public class Program {
    public static void Main() {
        var files = new[] { "ImpactCalculationScreen.kt", "InterpolationScreen.kt", "MapScreen.kt", "ProjectListScreen.kt", "SettingsScreen.kt" };
        
        var replacements = new Dictionary<string, string> {
            { "de\uFFFDi\uFFFDmedi", "değişmedi" },
            { "DEĞERLER\uFFFD", "DEĞERLERİ" },
            { "do\uFFFDu-bat\uFFFD", "doğu-batı" },
            { "hakedi\uFFFD?", "hakediş?" },
            { "oledu\uFFFDu", "olduğu" },
            { "hakedi\uFFFD", "hakediş" },
            { "HAKED\uFFFDŞ", "HAKEDİŞ" },
            { "E\uFFFDi\uFFFDi", "Eşiği" },
            { "e\uFFFDi\uFFFDi", "eşiği" }
        };
        
        foreach (var file in files) {
            var path = "app/src/main/java/com/example/egimhesabi/ui/screens/" + file;
            var content = File.ReadAllText(path, Encoding.UTF8);
            
            var keys = new List<string>(replacements.Keys);
            keys.Sort((a, b) => b.Length.CompareTo(a.Length));
            
            foreach (var key in keys) {
                content = content.Replace(key, replacements[key]);
            }
            
            File.WriteAllText(path, content, new UTF8Encoding(false));
            Console.WriteLine("Fixed replacements in " + file);
        }
    }
}
