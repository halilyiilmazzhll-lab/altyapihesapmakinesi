using System;
using System.IO;
using System.Text;

public class Program {
    public static void Main() {
        var files = new[] { "ImpactCalculationScreen.kt", "InterpolationScreen.kt", "MapScreen.kt", "ProjectListScreen.kt", "ReverseCalculationScreen.kt", "SettingsScreen.kt" };
        foreach (var file in files) {
            var path = "app/src/main/java/com/example/egimhesabi/ui/screens/" + file;
            var str = File.ReadAllText(path, Encoding.UTF8);
            if (str.StartsWith("?")) {
                str = str.Substring(1);
                File.WriteAllText(path, str, new UTF8Encoding(false));
                Console.WriteLine("Removed ? from " + file);
            }
        }
    }
}
