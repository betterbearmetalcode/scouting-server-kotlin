import java.lang.Integer.parseInt

const val StartName = "home"
const val ChartName = "charts"
const val DataCollectionName = "data_collection"
const val DataManagementName = "data_management"
const val ScoringScreen = "scoring"

fun String.betterParseDouble(maxLength: Int = 10): Double {
    val stringBuilder = StringBuilder()
    var hadDot = false
    for (c in this) {
        if (c == '.' && !hadDot) {
            hadDot = true
            stringBuilder.append(c)
        }
        try {
            c.toString().toDouble()
            stringBuilder.append(c)
        } catch (e: NumberFormatException) {

        }
    }
    if (stringBuilder.length > maxLength) {
        stringBuilder.deleteCharAt(stringBuilder.lastIndex)
    }
    if (stringBuilder.toString().isNotEmpty()) {
        return stringBuilder.toString().toDouble()
    }
    return 0.0;
}