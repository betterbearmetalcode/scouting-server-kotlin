import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.dhatim.fastexcel.reader.Cell
import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.tahomarobotics.scouting.DatabaseType
import java.io.File

fun importSpreadsheet(file: File, event: String) {
    val workbook = ReadableWorkbook(file)
    val worksheet = workbook.firstSheet

    val rows = worksheet.openStream()

    val locations = ArrayList<String>()

    var firstRow = true

    var broke = false

    breakSpot@for(row in rows) {
        println(row)
        if (firstRow) {
            firstRow = false
            row.forEach {
                locations.add(it.rawValue)
            }
            continue
        }

        val jsonObject = JsonObject()
        var i = 0
        var nullCount = 0 // Counts the number of null values there is in a row.
        for(cell in row) {
            val jsonPath = locations[i]
            val value : Any
            if(cell != null) {
                value = cell.value
            } else {
                value = ""
                nullCount++
                if(nullCount == 4) { // If there is 4 nulls in one row, it assumes that the entire row is null and stops iterating.
                    break@breakSpot
                }
            }

            if (!jsonPath.contains(":")){
                when (value) {
                    is String -> jsonObject.addProperty(jsonPath.trim().lowercase().replace(":", "").replace(" ", "_"), value)
                    is Number -> jsonObject.addProperty(jsonPath.trim().lowercase().replace(":", "").replace(" ", "_"), value)
                    is Boolean -> jsonObject.addProperty(jsonPath.trim().lowercase().replace(":", "").replace(" ", "_"), value)
                    is Char -> jsonObject.addProperty(jsonPath.trim().lowercase().replace(":", "").replace(" ", "_"), value)
                }
            } else {

                var fullPath = jsonPath.split(":")
                var finalLocation = fullPath.last()
                fullPath = fullPath.dropLast(1)
                var currentObj = jsonObject
                fullPath.forEach {
                    var obj = currentObj[it.trim()]
                    if (obj == null) {
                        obj = JsonObject()
                        currentObj.add(it.trim(), obj)
                    }
                    currentObj = obj as JsonObject
                }
                finalLocation = finalLocation.trim().lowercase().replace(":", "").replace(" ", "_")

                when (value) {
                    is String -> {
                        if(value.length > 3 && (value.substring(0, 4) == "Red " || value.substring(0, 4) == "Blue")) {
                            when(value) {
                                "Red 1" -> currentObj.addProperty(finalLocation, 0)
                                "Red 2" -> currentObj.addProperty(finalLocation, 1)
                                "Red 3" -> currentObj.addProperty(finalLocation, 2)
                                "Blue 1" -> currentObj.addProperty(finalLocation, 3)
                                "Blue 2" -> currentObj.addProperty(finalLocation, 4)
                                "Blue 3" -> currentObj.addProperty(finalLocation, 5)
                            }
                        } else {
                            currentObj.addProperty(finalLocation, value)
                        }
                    }
                    is Number -> currentObj.addProperty(finalLocation, value)
                    is Boolean -> currentObj.addProperty(finalLocation, value)
                    is Char -> currentObj.addProperty(finalLocation, value)
                }

            }
            i++
        }

        manager.processJSON(DatabaseType.MATCH, jsonObject.toString(), event)
        println("${jsonObject.toString()}")
    }

}