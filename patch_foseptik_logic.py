import sys

path = "c:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/ui/screens/FoseptikScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Replace the calculation logic in FoseptikScreen.kt
calc_start = content.find("    var inletElev: Double? = null")
calc_end = content.find("    val glassFieldColors = OutlinedTextFieldDefaults.colors(")

new_calc = """    import com.example.egimhesabi.domain.FoseptikCalculator
    
    val result = FoseptikCalculator.calculate(
        manholeInvert = manholeInvert,
        distance = distance,
        slopeType = slopeType,
        slopeVal = slopeVal,
        groundElev = groundElev,
        tankHeight = tankHeight,
        coverToInlet = coverToInlet
    )
    
    val inletElev = result.inletElev
    val bottomElev = result.bottomElev
    val coverElev = result.coverElev
    val excavationDepth = result.excavationDepth
    val slopeDecimal = result.slopeDecimal
    val errorMessage = result.errorMessage

"""

# We can't import inside a function. 
# It's better to just write a script that updates imports and the calc block.

content = content.replace("import com.example.egimhesabi.theme.*", "import com.example.egimhesabi.theme.*\nimport com.example.egimhesabi.domain.FoseptikCalculator")

calc_replacement = """    val result = FoseptikCalculator.calculate(
        manholeInvert = manholeInvert,
        distance = distance,
        slopeType = slopeType,
        slopeVal = slopeVal,
        groundElev = groundElev,
        tankHeight = tankHeight,
        coverToInlet = coverToInlet
    )
    
    val inletElev = result.inletElev
    val bottomElev = result.bottomElev
    val coverElev = result.coverElev
    val excavationDepth = result.excavationDepth
    val slopeDecimal = result.slopeDecimal
    val errorMessage = result.errorMessage
"""

content = content[:calc_start] + calc_replacement + "\n" + content[calc_end:]

# Add Error message display to RESULTS section
results_start = content.find("Text(\"Sonuçlar\", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AccentOrange)")

error_ui = """Text("Sonuçlar", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AccentOrange)
                            
                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
"""

content = content.replace("Text(\"Sonuçlar\", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AccentOrange)", error_ui)

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
