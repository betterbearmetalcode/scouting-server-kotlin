import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController



enum class TargetedValues {
    EVERYTHING,
    MATCHES,
    MATCH,
    TEAMS,
    TEAM,
    NONE
}

val mainTargetedValue = mutableStateOf(TargetedValues.NONE)
val byTargetedValue = mutableStateOf(TargetedValues.NONE)
var targetedTeamNumber = mutableStateOf("")
var targetedMatchNumber = mutableStateOf("")

@Composable
fun DatabaseOptionsScreen(navController: NavController) {
    Column {
        Text("Presets")
        Button(onClick = {
            mainTargetedValue.value = TargetedValues.MATCHES
            byTargetedValue.value = TargetedValues.EVERYTHING
        }) {
            Text("View matches")
        }
        Button(onClick = {
            mainTargetedValue.value = TargetedValues.TEAMS
            byTargetedValue.value = TargetedValues.NONE
        }) {
            Text("View teams")
        }
        Row {
            Button(onClick = {
                mainTargetedValue.value = TargetedValues.TEAM
                byTargetedValue.value = TargetedValues.MATCHES
            }) {
                Text("View $targetedTeamNumber's matches")
            }
            TextField(
                targetedTeamNumber.value,
                onValueChange = {
                    targetedTeamNumber.value = it
                }
            )
        }
        Row {
            Button(onClick = {
                mainTargetedValue.value = TargetedValues.MATCH
                byTargetedValue.value = TargetedValues.EVERYTHING
            }) {
                Text("View match #$targetedMatchNumber")
            }
            TextField(
                targetedMatchNumber.value,
                onValueChange = {
                    targetedMatchNumber.value = it
                }
            )
        }
    }
}