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
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class ScoutingType {
    MATCH,
    PITS,
    STRAT
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
        Button(onClick = {
            try {
                manager.processTeamsForEvent(eventCode)
            } catch (e: Exception) {
                showError = true
            }
        }) {
            Text("Submit")
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

        LazyColumn {

        }

        Button(onClick = {navController.navigateUp()}) {
            Text("Back")
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