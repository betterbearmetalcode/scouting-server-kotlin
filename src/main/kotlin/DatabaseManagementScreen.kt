import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.bson.Document
import org.bson.json.JsonObject
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import org.tahomarobotics.scouting.DatabaseType
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.print.Doc

enum class ScoutingType {
    MATCH,
    PITS,
    STRAT
}

fun convertYesNoToInt(value: String): Int {
    if (value.lowercase() == "yes")
        return 1
    return 0
}

@Composable
fun DatabaseManagementScreen(navController: NavController) {
    var eventCode by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showEmptyEventError by remember { mutableStateOf(false) }
    var matchSelected by remember { mutableStateOf(true) }
    var pitsSelected by remember { mutableStateOf(false) }
    var stratSelected by remember { mutableStateOf(false) }
    var textStyleBold = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)
    var scoutingType by remember { mutableStateOf(ScoutingType.MATCH) }
    Column {
        Row (verticalAlignment = Alignment.CenterVertically) {
            Text("Event Code:")
            TextField(value = eventCode, onValueChange = {eventCode = it})
        }
        Row {
            Button(onClick = {
                try {
                    manager.processTeamsForEvent(eventCode)
                } catch (e: Exception) {
                    showError = true
                }
            }) {
                Text("Submit")
            }
            Button(onClick = {
                if (eventCode.isEmpty()) {
                    showEmptyEventError = true
                    return@Button
                }
                //manager.pullFromTBA(DatabaseType.TBA_MATCHES, eventCode)
                val tbaData = manager.getDataFromEvent(DatabaseType.TBA_MATCHES, eventCode)

                val matches = manager.getDataFromEvent(DatabaseType.MATCH, eventCode)
                matches.forEach {
                    val matchNum = (it["match"] as String).toInt()
                    tbaData.forEach tba@{ tbaMatch ->
                        if ((matchNum != tbaMatch["match_number"]))
                            return@tba

                        val breakdown = tbaMatch["score_breakdown"]!! as Document
                        val breakdownBlue = breakdown["blue"]!! as Document
                        val breakdownRed = breakdown["red"]!! as Document

                        val startPos = it["robotStartPosition"] as Int
                        when (startPos) {
                            0 -> {
                                (it["auto"] as Document)
                                    .putIfAbsent("moved", convertYesNoToInt(breakdownRed["autoLineRobot1"] as String))
                                it.put("endPos", breakdownRed["endGameRobot1"])
                            }
                            1 -> {
                                (it["auto"] as Document)
                                    .putIfAbsent("moved", convertYesNoToInt(breakdownRed["autoLineRobot2"] as String))
                                it.put("endPos", breakdownRed["endGameRobot2"])
                            }
                            2 -> {
                                (it["auto"] as Document)
                                    .putIfAbsent("moved", convertYesNoToInt(breakdownRed["autoLineRobot3"] as String))
                                it.put("endPos", breakdownRed["endGameRobot3"])
                            }
                            3 -> {
                                (it["auto"] as Document)
                                    .putIfAbsent("moved", convertYesNoToInt(breakdownBlue["autoLineRobot1"] as String))
                                it.put("endPos", breakdownBlue["endGameRobot1"])
                            }
                            4 -> {
                                (it["auto"] as Document)
                                    .putIfAbsent("moved", convertYesNoToInt(breakdownBlue["autoLineRobot2"] as String))
                                it.put("endPos", breakdownBlue["endGameRobot2"])
                            }
                            5 -> {
                                (it["auto"] as Document)
                                    .putIfAbsent("moved", convertYesNoToInt(breakdownBlue["autoLineRobot3"] as String))
                                it.put("endPos", breakdownBlue["endGameRobot3"])
                            }
                        }
                    }
                    val string = hashToJSONString(it)
                    manager.processJSON(DatabaseType.MATCH, string, eventCode)
                }
            }) {
                Text("Update stop and endgame with TBA")
            }
        }
        Row {
            Button(
                onClick = {
                    if (eventCode.isEmpty())
                        showEmptyEventError = true
                    else
                        genExcelFile(eventCode, scoutingType)
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
                    scoutingType = ScoutingType.MATCH
                }) {
                    Row {
                        RadioButton(
                            selected = matchSelected,
                            onClick = {
                                matchSelected = true
                                pitsSelected = false
                                stratSelected = false
                                scoutingType = ScoutingType.MATCH
                            }
                        )
                        Text("Match", modifier = Modifier.align(Alignment.CenterVertically), style = textStyleBold)
                    }
                }
                TextButton(onClick = {
                    matchSelected = false
                    pitsSelected = true
                    stratSelected = false
                    scoutingType = ScoutingType.PITS
                }) {
                    Row {
                        RadioButton(
                            selected = pitsSelected,
                            onClick = {
                                matchSelected = false
                                pitsSelected = true
                                stratSelected = false
                                scoutingType = ScoutingType.PITS
                            }
                        )
                        Text("Pits", modifier = Modifier.align(Alignment.CenterVertically), style = textStyleBold)
                    }
                }
                TextButton(onClick = {
                    matchSelected = false
                    pitsSelected = false
                    stratSelected = true
                    scoutingType = ScoutingType.STRAT
                }) {
                    Row {
                        RadioButton(
                            selected = stratSelected,
                            onClick = {
                                matchSelected = false
                                pitsSelected = false
                                stratSelected = true
                                scoutingType = ScoutingType.STRAT
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
            Button(onClick = { navController.navigate(ScoringScreen)}) {
                Text("Advanced Mode")
            }
        }
    }


    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            buttons = {Button(onClick = {showError = false}) {Text("Ok")} },
            text = {Text("Error when generating data from event code")},
        )
    }
    if (showEmptyEventError) {
        AlertDialog(
            onDismissRequest = { showEmptyEventError = false },
            buttons = {Button(onClick = {showEmptyEventError = false}) {Text("Ok")}},
            text = {Text("Empty Event Code!")},
        )
    }
}

fun genExcelFile(eventKey: String, scoutingType: ScoutingType) {
    val file = File("output-${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}.xlsx")
    val workbook = Workbook(FileOutputStream(file), "Scouting Data", "1.0")

    val worksheet = workbook.newWorksheet("Data")

    val matches =
        if (scoutingType == ScoutingType.MATCH) {
            manager.getMatchesFromEvent(eventKey)
        } else if (scoutingType == ScoutingType.PITS) {
            manager.getPitsForEvent(eventKey)
        } else if (scoutingType == ScoutingType.STRAT) {
            manager.getStratForEvent(eventKey)
        } else {
            manager.getMatchesFromEvent(eventKey)
        }

    worksheet.value(0, 0, "Match #")

    var i = 1

    matches.forEach {
        val index = matches.indexOf(it)
        worksheet.value(index+1, 0, index+1)
        i = 1
        it.forEach { (key, value) ->
            if (key == "match") {
                worksheet.value(index+1, 0, value.toString())
                return@forEach
            }
            try {
                value as Document
                i = readDocument(worksheet, value, index + 1, i, "$key:")
            } catch (e: ClassCastException) {
                worksheet.value(0, i, key)
                worksheet.value(index+1, i, value.toString())
                i++
            }
        }
    }

    workbook.finish()
    workbook.close()
}

fun handleValue(value: Any, key : String, json: StringBuilder) {
    when (value) {
        is Document -> {
            json.append("\"$key\":{")
            value.forEach { (newKey, newValue) ->
                handleValue(newValue, newKey, json)
            }
            json.deleteCharAt(json.lastIndex)
            json.append("},")
        }
        is String -> {
            json.append("\"$key\":\"$value\",")
        }
        else -> {
            if (key != "_id")
                json.append("\"$key\":$value,")
        }
    }
}

fun hashToJSONString(hash : HashMap<String, Any>) : String {
    val json = StringBuilder()

    val currentWord = StringBuilder()
    var lastChar = ' '
    var inNet = false
    var inTele = false
    var inNotes = false
    var currentType = ""
    json.append("{")
    hash.forEach { (key, value) ->
        handleValue(value, key, json)
    }
    json.deleteCharAt(json.lastIndex)
    json.append("}")
    return json.toString()
}

fun readDocument(worksheet: Worksheet, document: Document, currentColumn: Int, currentRow: Int, docKey: String) : Int {
    var row = currentRow
    document.forEach { (key, value) ->
        try {
            value as Document
            row = readDocument(worksheet, value, currentColumn, row, "$docKey $key:" )
        } catch (e: ClassCastException) {
            worksheet.value(0, row, "$docKey $key")
            worksheet.value(currentColumn, row, value.toString())
            row++
        }
    }
    return row
}