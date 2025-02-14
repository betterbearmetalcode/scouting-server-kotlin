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
import org.tahomarobotics.scouting.DatabaseManager

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
//        LazyColumn {
//        }
    }
    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            buttons = {Button(onClick = {showError = false}) {Text("Ok")} },
            text = {Text("Invalid Event Code!")},
        )
    }
}