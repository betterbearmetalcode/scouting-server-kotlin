import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.Gson
import org.apache.commons.lang3.StringUtils
import org.bson.Document
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import org.tahomarobotics.scouting.DatabaseType
import org.tahomarobotics.scouting.TBAInterface
import java.io.File
import java.io.FileOutputStream
import java.lang.Integer.parseInt
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

fun convertYesNoToInt(value: String): Int {
    if (value.lowercase() == "yes")
        return 1
    return 0
}

var eventCode = mutableStateOf("")

@Composable
fun DatabaseManagementScreen(navController: NavController) {
    var showError by remember { mutableStateOf(false) }
    var showEmptyEventError by remember { mutableStateOf(false) }
    var invalidEventError by remember { mutableStateOf(false) }
    var matchSelected by remember { mutableStateOf(true) }
    var pitsSelected by remember { mutableStateOf(false) }
    var stratSelected by remember { mutableStateOf(false) }
    var textStyleBold = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)
    var scoutingType by remember { mutableStateOf(DatabaseType.MATCH) }
    Column {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterStart)) {
                Text("Event Code:")
                TextField(value = eventCode.value, onValueChange = { eventCode.value = it })
            }
            Button(
                onClick = { navController.navigate(CorrectionScreen) },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text("Correct Data")
            }
        }
        Row {
            Button(onClick = {
                try {
                    manager.pullFromTBA(DatabaseType.TEAMS, eventCode.value)
                    manager.pullFromTBA(DatabaseType.TBA_MATCHES, eventCode.value)
                } catch (e: Exception) {
                    showError = true
                }
            }) {
                Text("Submit")
            }
            Button(onClick = {
                if (eventCode.value.isEmpty()) {
                    showEmptyEventError = true
                    return@Button
                }
                if (!TBAInterface.isValidEventKey(eventCode.value)) {
                    invalidEventError = true
                    return@Button
                }
                manager.pullFromTBA(DatabaseType.TBA_MATCHES, eventCode.value)
                val tbaData = manager.getDataFromEvent(DatabaseType.TBA_MATCHES, eventCode.value)

                val matches = manager.getDataFromEvent(DatabaseType.MATCH, eventCode.value)
                matches.forEach {
                    val matchNum = (it["match"] as String).toInt()
                    tbaData.forEach tba@{ tbaMatch ->
                        if ((matchNum != tbaMatch["match_number"]))
                            return@tba
                        try {
                            val breakdown = tbaMatch["score_breakdown"]!! as Document
                            val breakdownBlue = breakdown["blue"]!! as Document
                            val breakdownRed = breakdown["red"]!! as Document

                            when (val startPos = it["robotStartPosition"] as Int) {
                                0, 1, 2 -> {
                                    (it["auto"] as Document)["moved"] =
                                        convertYesNoToInt(breakdownRed["autoLineRobot${startPos + 1}"] as String)

                                    val endPos = breakdownRed["endGameRobot${startPos + 1}"] as String
                                    println("end pos: $endPos")

                                    it.put("parked", endPos == "Parked")
                                    it.put("shallow", endPos == "ShallowCage")
                                    it.put("deep", endPos == "DeepCage")
                                    println("poses: $it")
                                }

                                3, 4, 5 -> {
                                    (it["auto"] as Document)["moved"] =
                                        convertYesNoToInt(breakdownBlue["autoLineRobot${startPos - 2}"] as String)

                                    val endPos = breakdownBlue["endGameRobot${startPos - 2}"] as String
                                    it.put("parked", endPos == "Parked")
                                    it.put("shallow", endPos == "ShallowCage")
                                    it.put("deep", endPos == "DeepCage")
                                }
                            }
                        } catch (e: NullPointerException) {

                        }
                    }
                    val string = hashToJSONString(it)
                    manager.processJSON(DatabaseType.MATCH, string, eventCode.value)
                }
            }) {
                Text("Update stop and endgame with TBA")
            }
            Button(
                onClick = {
                    // Retrieve all match documents for the event.
                    val allMatches = manager.getDataFromEvent(DatabaseType.MATCH, eventCode.value)
                    // Group documents by team.
                    val groupedByTeam = allMatches.groupBy { it["team"] as String }
                    // Collection to hold documents that are not duplicates.
                    val filteredCollection = mutableListOf<Map<String, Any>>()

                    groupedByTeam.forEach { (_, docs) ->
                        // Sort each group based on the match number (converted to Int).
                        val sortedDocs = docs.sortedBy { (it["match"] as String).toInt() }
                        var lastKeptMatchNumber: Int? = null
                        // Iterate through the sorted documents.
                        for (doc in sortedDocs) {
                            val currentMatchNumber = (doc["match"] as String).toInt()
                            if (lastKeptMatchNumber == null || currentMatchNumber - lastKeptMatchNumber != 1) {
                                filteredCollection.add(doc)
                                lastKeptMatchNumber = currentMatchNumber
                            }
                        }
                    }
                    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    // Use Gson to convert documents to JSON.
                    val gson = Gson()
                    // Output file in NDJSON format.
                    val outputFile = File("output-${timestamp}-filtered.ndjson")
                    outputFile.printWriter().use { writer ->
                        filteredCollection.forEach { doc ->
                            writer.println(gson.toJson(doc))
                        }
                    }
                }
            ) {
                Text("Export Deduplicated Data")
            }
        }
        Row {
            Button(
                onClick = {
                    if (eventCode.value.isEmpty())
                        showEmptyEventError = true
                    else
                        genExcelFile(eventCode.value, scoutingType)
                },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text("Generate Excel file")
            }
            Column {
                TextButton(onClick = {
                    matchSelected = true
                    pitsSelected = false
                    stratSelected = false
                    scoutingType = DatabaseType.MATCH
                }) {
                    Row {
                        RadioButton(
                            selected = matchSelected,
                            onClick = {
                                matchSelected = true
                                pitsSelected = false
                                stratSelected = false
                                scoutingType = DatabaseType.MATCH
                            }
                        )
                        Text("Match", modifier = Modifier.align(Alignment.CenterVertically), style = textStyleBold)
                    }
                }
                TextButton(onClick = {
                    matchSelected = false
                    pitsSelected = true
                    stratSelected = false
                    scoutingType = DatabaseType.PITS
                }) {
                    Row {
                        RadioButton(
                            selected = pitsSelected,
                            onClick = {
                                matchSelected = false
                                pitsSelected = true
                                stratSelected = false
                                scoutingType = DatabaseType.PITS
                            }
                        )
                        Text("Pits", modifier = Modifier.align(Alignment.CenterVertically), style = textStyleBold)
                    }
                }
                TextButton(onClick = {
                    matchSelected = false
                    pitsSelected = false
                    stratSelected = true
                    scoutingType = DatabaseType.STRATEGY
                }) {
                    Row {
                        RadioButton(
                            selected = stratSelected,
                            onClick = {
                                matchSelected = false
                                pitsSelected = false
                                stratSelected = true
                                scoutingType = DatabaseType.STRATEGY
                            }
                        )
                        Text("Strat", modifier = Modifier.align(Alignment.CenterVertically), style = textStyleBold)
                    }
                }
            }
        }

        Row {
            Button(onClick = { navController.navigateUp() }) {
                Text("Back")
            }
            Button(onClick = { navController.navigate(ScoringScreen) }) {
                Text("Advanced Mode")
            }
            Button(onClick = {
                val file = openFileDialog(ComposeWindow(), "Choose a File", listOf(".xlsx"), false)
                if (!file.isEmpty()) {
                    importSpreadsheet(file.first(), eventCode.value)
                }
            }) {
                Text("Import Excel File")
            }
        }
    }


    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            buttons = { Button(onClick = { showError = false }) { Text("Ok") } },
            text = { Text("Error when generating data from event code") },
        )
    }
    if (showEmptyEventError) {
        AlertDialog(
            onDismissRequest = { showEmptyEventError = false },
            buttons = { Button(onClick = { showEmptyEventError = false }) { Text("Ok") } },
            text = { Text("Empty Event Code!") },
        )
    }
    if (invalidEventError) {
        AlertDialog(
            onDismissRequest = { invalidEventError = false },
            buttons = { Button(onClick = { invalidEventError = false }) { Text("Ok") } },
            text = { Text("Invalid Event Code!") },
        )
    }
}

fun generateLocationIndices(
    key: String,
    value: Any,
    prefix: String,
    hash: HashMap<String, Int>,
    column: Int,
    spreadsheet: Worksheet
): Int {
    var currentColumn = column

    when (value) {
        is Document -> {
            value.forEach { (newKey, value) ->
                currentColumn =
                    generateLocationIndices(newKey, value, "$prefix$key: ", hash, currentColumn, spreadsheet)
            }
        }

        else -> {
            if (key == "match" || key == "_id")
                return currentColumn
            val formattedKey = formatKey("$prefix$key")
            spreadsheet.value(0, currentColumn, formattedKey)
            hash["$prefix$key"] = currentColumn
            currentColumn++
        }
    }
    return currentColumn
}

fun formatKey(key: String): String {
    val temp = key.replace("_", " ")
    val words = temp.split(" ")
    val finalVal = StringBuilder()
    for (word in words) {
        if (word.isNotEmpty()) {
            finalVal.append(word[0].uppercaseChar()).append(word.drop(1)).append(" ")
        }
    }
    return finalVal.toString()
}

fun genExcelFile(eventKey: String, scoutingType: DatabaseType) {
    val matches = manager.getDataFromEvent(scoutingType, eventKey)

    var highestMatchNum = 0
    var lowestMatchNum = 9999

    matches.forEach {
        val matchNum = if (scoutingType == DatabaseType.MATCH) {
            parseInt(it["match"] as String)
        } else if (scoutingType == DatabaseType.STRATEGY) {
            it["match"] as Int
        } else {
            0
        }
        if (highestMatchNum < matchNum)
            highestMatchNum = matchNum
        if (lowestMatchNum > matchNum)
            lowestMatchNum = matchNum
    }

    val file =
        File("${StringUtils.capitalize(scoutingType.collectionName)} export from $eventKey - Matches $lowestMatchNum-$highestMatchNum.xlsx")
    val workbook = Workbook(FileOutputStream(file), "Scouting Data", null)

    val worksheet = workbook.newWorksheet("Data")

    worksheet.value(0, 0, "Match #")

    var i = 1
    val keyLocationsHash = HashMap<String, Int>()
    val tempMatch = matches.firstOrNull()?.toList() ?: emptyList()
    val sortedKeys = tempMatch.sortedBy {
        if (it.first == "team") 0
        else if (it.first == "robotStartPosition") 1
        else if (it.first == "auto") 2
        else if (it.first == "tele") 3
        else if (it.first == "parked") 4
        else if (it.first == "shallow") 5
        else if (it.first == "deep") 6
        else Integer.MAX_VALUE
    }
    sortedKeys.forEach { (key, value) ->
        i = generateLocationIndices(key, value, "", keyLocationsHash, i, worksheet)
    }

    matches.sortWith(Comparator { hash1: HashMap<String, Any>, hash2: HashMap<String, Any> ->
        when (val matchNum = hash1["match"].toString().toInt() - hash2["match"].toString().toInt()) {
            0 -> try {
                hash1["robotStartPosition"].toString().toInt().compareTo(hash2["robotStartPosition"].toString().toInt())
            } catch (_: NumberFormatException) {
                0
            }

            else -> matchNum
        }
    })

    matches.forEach {
        printLn("Current Match: ${it}")
        val index = matches.indexOf(it)
        i = 1
        it.forEach { (key, value) ->
            if (key == "match") {
                worksheet.value(index + 1, 0, value.toString())
                return@forEach
            }
            if (key == "robotStartPosition") {
                worksheet.value(index + 1, keyLocationsHash[key]!!, startPosToString(value as Int))
                return@forEach
            }
            if (scoutingType == DatabaseType.MATCH)
                handleValueForExcel(
                    worksheet,
                    it,
                    matches,
                    value,
                    key,
                    index + 1,
                    keyLocationsHash,
                    "",
                    (it["robotStartPosition"] as Int >= 3),
                    parseInt(it["match"] as String),
                    eventKey
                )
            else if (scoutingType == DatabaseType.STRATEGY)
                handleValueForExcel(
                    worksheet,
                    it,
                    matches,
                    value,
                    key,
                    index + 1,
                    keyLocationsHash,
                    "",
                    (it["is_red_alliance"] as Boolean),
                    it["match"] as Int,
                    eventKey
                )
        }
    }

    workbook.finish()
    workbook.close()
}

fun genExcelFile(eventKey: String, stratData: Map<String, Map<Int, Double>>) {

    var highestMatchNum = 0
    var lowestMatchNum = 9999

    val file = File(
        "Compiled Strategy Export from $eventKey - ${
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"))
        }.xlsx"
    )
    val workbook = Workbook(FileOutputStream(file), "Scouting Data", "1.0")

    val worksheet = workbook.newWorksheet("Data")

    worksheet.value(0, 1, "Strategy")
    worksheet.value(0, 2, "Mechanical Soundness")
    worksheet.value(0, 3, "Driving Skill")

    var row = 1
    val strategy = stratData["STRATEGY"]
    val mech = stratData["MECHANICAL_SOUNDNESS"]
    val driving = stratData["DRIVING_SKILL"]

    strategy!!.forEach { (key, value) ->
        worksheet.value(row, 0, key)
        worksheet.value(row, 1, value.toString())
        row++
    }
    row = 1
    mech!!.forEach { (key, value) ->
        worksheet.value(row, 2, value.toString())
        row++
    }
    row = 1
    driving!!.forEach { (key, value) ->
        worksheet.value(row, 3, value.toString())
        row++
    }

    workbook.finish()
    workbook.close()
}

enum class ScoreErrorLevel(val color: String) {
    GREEN("00FF00"),
    YELLOW("FFFF00"),
    ORANGE("FF9900"),
    RED("FF0000"),
    ERROR("B7B7B7");

    override fun toString(): String {
        return this.color
    }
}

fun genCSVFile(values: List<String>, fileName: String) {
    val file = File("$fileName.csv")
    file.printWriter().use { writer ->
        values.forEach { value ->
            writer.println(value)
        }
    }
}

fun genCSVFile(values: HashMap<String, Double>, fileName: String) {
    val file = File("$fileName.csv")
    file.printWriter().use { writer ->
        values.forEach { (key, value) ->
            writer.println("$key,$value")
        }
    }
}

fun readCSV(csv: String): List<String> {
    return csv.split(",")
}

fun readCSV(csv: String, hashMap: Boolean): HashMap<String, Double> {
    val hash = HashMap<String, Double>()
    val lines = csv.split("\n")
    lines.forEach {
        val line = it.split(",")
        if (line.size == 2) {
            hash[line[0]] = line[1].toDouble()
        }
    }
    return hash
}

enum class ReefLevel(val tbaKey: String, val key: String) {
    TOP("tba_topRowCount", "reef_level4"),
    MID("tba_midRowCount", "reef_level3"),
    LOW("tba_botRowCount", "reef_level2"),
    TROUGH("trough", "reef_level1")
}

fun checkScore(sampleScore: Int, realScore: Int, yellowPoint: Int, orangePoint: Int, redPoint: Int): ScoreErrorLevel {
    if (realScore == -1) return ScoreErrorLevel.ERROR

    val offBy = (realScore - sampleScore).absoluteValue
    
    if (offBy >= redPoint) {
        return ScoreErrorLevel.RED
    }
    
    if (offBy in orangePoint..<redPoint && orangePoint > 0) return ScoreErrorLevel.ORANGE

    if (offBy in yellowPoint..<orangePoint && yellowPoint > 0) return ScoreErrorLevel.YELLOW
    
    return ScoreErrorLevel.GREEN
}

fun handleValueForJSON(value: Any, key: String, json: StringBuilder) {
    when (value) {
        is Document -> {
            json.append("\"$key\":{")
            value.forEach { (newKey, newValue) ->
                handleValueForJSON(newValue, newKey, json)
            }
            json.deleteCharAt(json.lastIndex)
            json.append("},")
        }

        is String -> {
            json.append("\"$key\":\"${value.replace("\"", "\\\"")}\",")
        }

        else -> {
            if (key != "_id")
                json.append("\"$key\":$value,")
        }
    }
}

fun generateMatchingMatches(
    match: HashMap<String, Any>,
    matches: List<HashMap<String, Any>>,
    blue: Boolean
): ArrayList<HashMap<String, Any>> {
    val matchNum = match["match"].toString()
    val matchingMatches = ArrayList<HashMap<String, Any>>()
    matches.forEach {
        if (matchNum == it["match"].toString() && blue == (it["robotStartPosition"] as Int >= 3)) {
            matchingMatches.add(it)
        }
    }
    return matchingMatches
}

fun calculateScoutedCoralScoreForMatch(
    match: HashMap<String, Any>,
    matches: List<HashMap<String, Any>>,
    auto: Boolean,
    blue: Boolean,
    level: ReefLevel
): Int {
    val matchingMatches = generateMatchingMatches(match, matches, blue)
    var num = 0
    matchingMatches.forEach {
        num += ((it[if (auto) "auto" else "tele"] as Document)["coral"] as Document)[level.key] as Int
    }
    return num
}

fun calculateScoutedAlgaeScoreForMatch(
    match: HashMap<String, Any>,
    matches: List<HashMap<String, Any>>,
    blue: Boolean,
    net: Boolean
): Int {
    val matchingMatches = generateMatchingMatches(match, matches, blue)
    var num = 0
    matchingMatches.forEach {
        if (net) {
            num += ((it["auto"] as Document)["net"] as Document)["scored"] as Int
            num += ((it["tele"] as Document)["net"] as Document)["scored"] as Int
        } else {
            num += ((it["auto"] as Document)["algae"] as Document)["processed"] as Int
            num += ((it["tele"] as Document)["algae"] as Document)["processed"] as Int
        }
    }
    return num
}

fun hashToJSONString(hash: HashMap<String, Any>): String {
    val json = StringBuilder()
    json.append("{")
    hash.forEach { (key, value) ->
        handleValueForJSON(value, key, json)
    }
    json.deleteCharAt(json.lastIndex)
    json.append("}")
    return json.toString()
}

fun handleValueForExcel(
    worksheet: Worksheet,
    matchDocument: HashMap<String, Any>,
    allMatches: List<HashMap<String, Any>>,
    value: Any,
    key: String,
    currentColumn: Int,
    locationsHash: HashMap<String, Int>,
    prefix: String,
    blue: Boolean,
    match: Int,
    event: String
) {
    when (value) {
        is Document -> {
            value.forEach { (docKey, docValue) ->
                handleValueForExcel(
                    worksheet,
                    matchDocument,
                    allMatches,
                    docValue,
                    docKey,
                    currentColumn,
                    locationsHash,
                    "$prefix$key: ",
                    blue,
                    match,
                    event
                )
            }
            if (key == "coral") {
                val inAuto = prefix.contains("auto")

                value.forEach { (docKey, docValue) ->
                    var level: ReefLevel? = null
                    when (docKey) {
                        "reef_level1" -> level = ReefLevel.TROUGH
                        "reef_level2" -> level = ReefLevel.LOW
                        "reef_level3" -> level = ReefLevel.MID
                        "reef_level4" -> level = ReefLevel.TOP
                    }

                    if (level != null) {
                        val score = calculateScoutedCoralScoreForMatch(matchDocument, allMatches, inAuto, blue, level)
                        val realScore = getActualCoral(level, inAuto, blue, event, match)
                        val color = checkScore(
                            score, realScore, 1, 2, if (inAuto) {
                                2
                            } else {
                                3
                            }
                        )
                        worksheet.style(currentColumn, locationsHash["$prefix$key: $docKey"]!!)
                            .fillColor(color.toString()).set()
                    }
                }
            } else if (key == "algae") {
                val score = calculateScoutedAlgaeScoreForMatch(matchDocument, allMatches, blue, false)
                val realScore = getActualAlgae(false, blue, event, match)
                val color = checkScore(score, realScore, -1, 1, 2)
                value.forEach { (docKey, docValue) ->
                    if (docKey.contains("processed"))
                        worksheet.style(currentColumn, locationsHash["$prefix$key: $docKey"]!!)
                            .fillColor(color.toString()).set()
                }
            } else if (key == "net") {
                val score = calculateScoutedAlgaeScoreForMatch(matchDocument, allMatches, blue, true)
                val realScore = getActualAlgae(true, blue, event, match)
                val color = checkScore(score, realScore, -1, 1, 2)
                value.forEach { (docKey, docValue) ->
                    if (docKey.contains("scored"))
                        worksheet.style(currentColumn, locationsHash["$prefix$key: $docKey"]!!)
                            .fillColor(color.toString()).set()
                }
            }
        }

        is Boolean -> {
            worksheet.value(currentColumn, locationsHash["$prefix$key"]!!, if (value) 1 else 0)
        }

        else -> {
            if (key != "_id") {
                worksheet.value(currentColumn, locationsHash["$prefix$key"]!!, value.toString())
            }
        }
    }
}

fun getActualCoral(level: ReefLevel, auto: Boolean, blue: Boolean, event: String, matchNum: Int): Int {
    val matches = manager.getDataFromEvent(DatabaseType.TBA_MATCHES, event)

    var actualMatch = Document()

    matches.forEach {
        if (it["match_number"] as Int == matchNum) {
            try {
                actualMatch =
                    ((it["score_breakdown"] as Document)[if (blue) "blue" else "red"] as Document)[if (auto) "autoReef" else "teleopReef"] as Document
            } catch (_: NullPointerException) {
                return -1
            }
        }
    }

    return if (auto) actualMatch[level.tbaKey] as Int
    else {
        val autoCoral = getActualCoral(level, true, blue, event, matchNum)
        if (autoCoral != -1) {
            actualMatch[level.tbaKey] as Int - autoCoral
        } else -1
    }
}

fun getActualAlgae(net: Boolean, blue: Boolean, event: String, matchNum: Int): Int {
    val matches = manager.getDataFromEvent(DatabaseType.TBA_MATCHES, event)

    var actualMatch = Document()

    matches.forEach {
        if (it["match_number"] as Int == matchNum) {
            try {
                actualMatch = (it["score_breakdown"] as Document)[if (blue) "blue" else "red"] as Document
            } catch (_: NullPointerException) {
                return -1
            }
        }
    }

    return actualMatch[if (net) "netAlgaeCount" else "wallAlgaeCount"] as Int
}

fun startPosToString(pos: Int): String {
    return when (pos) {
        0 -> "Red 1"
        1 -> "Red 2"
        2 -> "Red 3"
        3 -> "Blue 1"
        4 -> "Blue 2"
        5 -> "Blue 3"
        else -> "Red 1"
    }
}