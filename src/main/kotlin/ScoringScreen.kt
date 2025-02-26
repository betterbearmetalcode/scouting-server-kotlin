import SDScoring.Ranker
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import org.bson.Document
import java.util.*
import kotlin.collections.HashMap

@Composable
fun ScoringScreen(navController: NavController) {
    var eventKey by remember { mutableStateOf("") }
    var showEmptyEventError by remember { mutableStateOf(false) }
    val finalMap : HashMap<String, HashMap<Int, Double>> = HashMap()
    val listOfWeights = mutableMapOf<String, Double>()
    Column {
        Row (verticalAlignment = Alignment.CenterVertically) {
            Text("Event Code:")
            TextField(value = eventKey, onValueChange = {eventKey = it})
        }
        Button(onClick = {
            if (eventKey.isEmpty())
                showEmptyEventError = true
            val play: EnumMap<RankType, HashMap<Int, HashMap<Int, Int>>> = EnumMap(RankType::class.java)
            play.putIfAbsent(RankType.STRATEGY, HashMap())
            play.putIfAbsent(RankType.DRIVING_SKILL, HashMap())
            play.putIfAbsent(RankType.MECHANICAL_SOUNDNESS, HashMap())
            val stratInfo = manager.getStratForEvent(eventKey)

            stratInfo.forEach {
                val strat = it["strategy_order"] as Document
                val driving = it["driving_skill"] as Document
                val mech = it["mechanical_soundness"] as Document

                strat.forEach { (key, value) ->
                    val stratHash = play[RankType.STRATEGY]!!
                    stratHash.putIfAbsent(value as Int, HashMap())
                    val teamArray = stratHash[value]!!

                    teamArray[it["match"] as Int] = 4 - key.toInt()
                }

                driving.forEach { (key, value) ->
                    val driveHash = play[RankType.DRIVING_SKILL]!!
                    driveHash.putIfAbsent(value as Int, HashMap())
                    val teamArray = driveHash[value]!!

                    teamArray[it["match"] as Int] = 4 - key.toInt()
                }

                mech.forEach { (key, value) ->
                    val mechHash = play[RankType.STRATEGY]!!
                    mechHash.putIfAbsent(value as Int, HashMap())
                    val teamArray = mechHash[value]!!

                    teamArray[it["match"] as Int] = 4 - key.toInt()
                }
            }

            Ranker.setPlay(play)



            val hashOfTeamsToRankings : HashMap<Int, EnumMap<RankType, Double>> = HashMap()
            val teams = manager.getTeamsFromEvent(eventKey)
            teams.forEach { (key, _) ->
                hashOfTeamsToRankings.putIfAbsent(key, EnumMap(RankType::class.java))

                hashOfTeamsToRankings[key]!!.putIfAbsent(RankType.STRATEGY, Ranker(RankType.STRATEGY, key, eventKey).getRank())
                hashOfTeamsToRankings[key]!!.putIfAbsent(RankType.DRIVING_SKILL, Ranker(RankType.DRIVING_SKILL, key, eventKey).getRank())
                hashOfTeamsToRankings[key]!!.putIfAbsent(RankType.MECHANICAL_SOUNDNESS, Ranker(RankType.MECHANICAL_SOUNDNESS, key, eventKey).getRank())
            }

            val matchData = manager.getMatchesFromEvent(eventKey)

            teams.forEach { (teamKey, _) ->
                teamKey as Int
                var totalMatch = 0
                val tempHash = HashMap<String, Double>()
                matchData.forEach {
                    if ((it["team"] as String).toInt() == teamKey) {
                        it.forEach breakFor@{ (matchKey, value) ->
                            when (value) {
                                is String -> return@breakFor
                                is Document -> processDocument(value, tempHash)
                                is Double -> tempHash[matchKey] = value + (tempHash[matchKey] ?: 0.0)
                            }
                        }
                        totalMatch++
                    }
                }
                tempHash.forEach { (key, value) ->
                    tempHash[key] = value / totalMatch
                    finalMap.putIfAbsent(key, HashMap())
                    finalMap[key]!![teamKey] = tempHash[key]!!
                }

                finalMap.forEach { (key, value) ->
                    finalMap[key] = sdScorer(value)
                    listOfWeights.putIfAbsent(key, 1.0)
                }

            }
        }) {
            Text("Start")
        }

        LazyColumn {
            listOfWeights.forEach { (key, value) ->
                item {
                    Row {
                        Text("$key: $value")
                        Slider(value.toFloat(), onValueChange = {listOfWeights[key] = it.toDouble()}, steps = 100)
                    }
                }
            }
        }

        if (showEmptyEventError) {
            AlertDialog(
                onDismissRequest = { showEmptyEventError = false },
                buttons = {Button(onClick = {showEmptyEventError = false}) {Text("Ok")}},
                text = {Text("Empty Event Code!")},
            )
        }
    }
}

fun processDocument(document: Document, hash: HashMap<String, Double>) {
    document.forEach { (key, value) ->
        when (value) {
            is String -> return
            is Document -> processDocument(value, hash)
            is Double -> hash[key] = value + (hash[key] ?: 0.0)
        }
    }
}