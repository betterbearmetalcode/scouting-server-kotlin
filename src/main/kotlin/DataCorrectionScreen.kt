import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import org.tahomarobotics.scouting.DatabaseType

@Composable
fun DataCorrectionScreen(navController: NavController) {
    val matchesWithError = remember { ArrayList<Pair<HashMap<String, Any>, Array<ScoreErrorLevel>>>() }
    Column {
        if (matchesWithError.isEmpty()) {
            val allMatches = manager.getDataFromEvent(DatabaseType.MATCH, eventCode.value)
            allMatches.forEach {
                val blue = it["robotStartPosition"] as Int >= 3
                val matchNum = it["match"].toString().toInt()
                val autoCoralScoutScore = calculateScoutedCoralScoreForMatch(it, allMatches, true, blue)
                val teleCoralScoutScore = calculateScoutedCoralScoreForMatch(it, allMatches, false, blue)
                val algaeScoutScore = calculateScoutedAlgaeScoreForMatch(it, allMatches, blue)
                val autoCoralActual = getActualScoreFromSection("coral", true, blue, eventCode.value, matchNum)
                val teleCoralActual = getActualScoreFromSection("coral", false, blue, eventCode.value, matchNum)
                val algaeActual = getActualScoreFromSection("algae", false, blue, eventCode.value, matchNum)
                val autoCoralErrorLevel = checkScore(autoCoralScoutScore, autoCoralActual, 1, 2)
                val teleCoralErrorLevel = checkScore(teleCoralScoutScore, teleCoralActual, 1, 3)
                val algaeErrorLevel = checkScore(algaeScoutScore, algaeActual, -1, 1)
                if (ScoreErrorLevel.GREEN !in listOf(autoCoralErrorLevel, teleCoralErrorLevel, algaeErrorLevel))
                    matchesWithError.add(Pair(it, arrayOf(autoCoralErrorLevel, teleCoralErrorLevel, algaeErrorLevel)))
            }
        }
        Button(onClick = {navController.navigateUp()}) {
            Text("Back")
        }
        LazyColumn {
            matchesWithError.forEach {
                item {
                    Text("Match # ${it.first["match"]} has a maximum error level of ${if (it.second.contains(ScoreErrorLevel.RED)) "Red" else "Yellow"}")
                }
            }
        }
    }
}