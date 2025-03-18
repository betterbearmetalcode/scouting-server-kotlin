import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.tahomarobotics.scouting.DatabaseType
import java.io.File

fun importSpreadsheet(file: File, event: String) {
    val workbook = ReadableWorkbook(file)
    val worksheet = workbook.firstSheet

    val rows = worksheet.openStream()

    val indices = rows.findFirst().get()

    val locations = ArrayList<String>()

    indices.forEach {
        locations.add(it.rawValue)
    }

    var firstRow = true
    val fullArray = JsonArray()

    rows.forEach { row ->
        if (firstRow) {
            firstRow = false
            return@forEach
        }

        val jsonObject = JsonObject()
        var i = 0
        row.forEach { cell ->
            val jsonPath = locations[i]
            val value = cell.value
            if (!jsonPath.contains(":")){
                when (value) {
                    is String -> jsonObject.addProperty(jsonPath, value)
                    is Number -> jsonObject.addProperty(jsonPath, value)
                    is Boolean -> jsonObject.addProperty(jsonPath, value)
                    is Char -> jsonObject.addProperty(jsonPath, value)
                }
            } else {
                val fullPath = jsonPath.split(":")
                val finalLocation = fullPath.last()
                fullPath.dropLast(1)
                var currentObj = jsonObject
                fullPath.forEach {
                    val obj = JsonObject()
                    currentObj.add(it.trim(), obj)
                    currentObj
                }
                when (value) {
                    is String -> currentObj.addProperty(finalLocation, value)
                    is Number -> currentObj.addProperty(finalLocation, value)
                    is Boolean -> currentObj.addProperty(finalLocation, value)
                    is Char -> currentObj.addProperty(finalLocation, value)
                }
            }
        }
        jsonObject.addProperty("event_key", event)
        fullArray.add(jsonObject)
    }

    manager.processJSON(DatabaseType.MATCH, fullArray.toString(), event)
}