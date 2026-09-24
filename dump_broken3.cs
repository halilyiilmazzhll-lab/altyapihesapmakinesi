using System;
using System.IO;
using System.Text;
using System.Collections.Generic;

public class Program {
    public static void Main() {
        var files = new[] { "ImpactCalculationScreen.kt", "InterpolationScreen.kt", "MapScreen.kt", "ProjectListScreen.kt", "SettingsScreen.kt" };
        var brokenWords = new HashSet<string>();
        
        foreach (var file in files) {
            var content = File.ReadAllText("app/src/main/java/com/example/egimhesabi/ui/screens/" + file, Encoding.UTF8);
            var words = content.Split(new[] { ' ', '"', '\n', '\r', '(', ')', '[', ']', '{', '}', '<', '>', ',', '.', ':', ';', '!', '=' }, StringSplitOptions.RemoveEmptyEntries);
            foreach (var word in words) {
                if (word.Contains("\uFFFD")) {
                    brokenWords.Add(word);
                }
            }
        }
        
        foreach (var w in brokenWords) {
            Console.Write("Word: ");
            foreach (var c in w) {
                Console.Write(((int)c).ToString("X4") + " ");
            }
            Console.WriteLine();
        }
    }
}
