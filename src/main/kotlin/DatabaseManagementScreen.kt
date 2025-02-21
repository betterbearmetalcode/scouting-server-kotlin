import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import org.bson.Document
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import org.tahomarobotics.scouting.DatabaseManager
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date

@Composable
fun DatabaseManagementScreen(navController: NavController) {
    var eventCode by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
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
        Button(onClick = {genExcelFile(eventCode)}) {
            Text("Generate Excel file")
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
            text = {Text("Error generating data from event code")},
        )
    }
}

fun genExcelFile(eventKey: String) {
    val file = File("output-${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}.xlsx")
    val workbook = Workbook(FileOutputStream(file), "Scouting Data", "1.0")

    val worksheet = workbook.newWorksheet("Data")

    val matches = manager.getMatchesFromEvent(eventKey)

    val temp = matches.first()

    worksheet.value(0, 0, "Match #")

    var i = 1

    matches.forEach {
        val index = matches.indexOf(it)
        worksheet.value(index+1, 0, index+1)
        i = 1
        it.forEach { (key, value) ->
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