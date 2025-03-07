import SDScoring.Ranker
import SDScoring.normalizeWeights
import SDScoring.scoring
import SDScoring.teamScorer
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.bson.Document
import org.tahomarobotics.scouting.DatabaseType
import org.tahomarobotics.scouting.TBAInterface
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.Map.Entry
import kotlin.collections.set
import kotlin.math.round

@Composable
fun ScoringScreen(navController: NavController) {
    var eventKey by remember { mutableStateOf("") }
    var showEmptyEventError by remember { mutableStateOf(false) }
    var invalidEventError by remember { mutableStateOf(false) }
    val finalMap : HashMap<String, HashMap<Int, Double>> = remember { HashMap() }
    val listOfWeights = remember { mutableMapOf<String, MutableDoubleState>() }
    var debug by remember { mutableStateOf(false) }
    var showTeamsRanked by remember { mutableStateOf(false) }
    var showMatrix by remember { mutableStateOf(false) }
    var matrixVerticalScrollState = rememberScrollState(0)
    var matrixHorizontalScrollState = rememberScrollState(0)
    val cellWidth = 90.dp
    val cellHeight = 50.dp
    val smallCellHeight = 25.dp
    var teams = HashMap<Int, Double>()
    Column {
        Row (verticalAlignment = Alignment.CenterVertically) {
            Text("Event Code:")
            TextField(value = eventKey, onValueChange = {eventKey = it})
        }
        Row {
            Button(onClick = {
                debug = false
                if (eventKey.isEmpty()) {
                    showEmptyEventError = true
                    return@Button
                }
                if (!TBAInterface.isValidEventKey(eventKey)) {
                    invalidEventError = true
                    return@Button
                }
                val play: EnumMap<RankType, HashMap<Int, HashMap<Int, HashMap<Boolean, Int>>>> = EnumMap(RankType::class.java)
                play.putIfAbsent(RankType.STRATEGY, HashMap())
                play.putIfAbsent(RankType.DRIVING_SKILL, HashMap())
                play.putIfAbsent(RankType.MECHANICAL_SOUNDNESS, HashMap())
                val stratInfo = manager.getDataFromEvent(DatabaseType.STRATEGY, eventKey)

                stratInfo.forEach {
                    val strat = it["strategy"] as Document
                    val driving = it["driving_skill"] as Document
                    val mech = it["mechanical_soundness"] as Document

                    strat.forEach { (key, value) ->
                        val stratHash = play[RankType.STRATEGY]!!
                        stratHash.putIfAbsent(value as Int, HashMap())
                        stratHash[value]!!.putIfAbsent(it["match"] as Int, HashMap())
                        val teamArray = stratHash[value]!![it["match"] as Int]!!

                        teamArray[it["is_red_alliance"] as Boolean] = 4 - key.toInt()
                    }

                    driving.forEach { (key, value) ->
                        val driveHash = play[RankType.DRIVING_SKILL]!!
                        driveHash.putIfAbsent(value as Int, HashMap())
                        driveHash[value]!!.putIfAbsent(it["match"] as Int, HashMap())
                        val teamArray = driveHash[value]!![it["match"] as Int]!!

                        teamArray[it["is_red_alliance"] as Boolean] = 4 - key.toInt()
                    }

                    mech.forEach { (key, value) ->
                        val mechHash = play[RankType.MECHANICAL_SOUNDNESS]!!
                        mechHash.putIfAbsent(value as Int, HashMap())
                        mechHash[value]!!.putIfAbsent(it["match"] as Int, HashMap())
                        val teamArray = mechHash[value]!![it["match"] as Int]!!

                        teamArray[it["is_red_alliance"] as Boolean] = 4 - key.toInt()
                    }
                }

                Ranker.setPlay(play)

                val hashOfTeamsToRankings: HashMap<Int, EnumMap<RankType, Double>> = HashMap()
                val teams = manager.getDataFromEvent(DatabaseType.TEAMS, eventKey)
                teams.forEach {
                    val num = it["team_number"] as Int
                    if (!(play[RankType.STRATEGY]!!.keys.contains(num))) {
                        return@forEach
                    }
                    hashOfTeamsToRankings.putIfAbsent(num, EnumMap(RankType::class.java))

                    hashOfTeamsToRankings[num]!!.putIfAbsent(
                        RankType.STRATEGY,
                        Ranker(RankType.STRATEGY, num, eventKey).getRank()
                    )
                    hashOfTeamsToRankings[num]!!.putIfAbsent(
                        RankType.DRIVING_SKILL,
                        Ranker(RankType.DRIVING_SKILL, num, eventKey).getRank()
                    )
                    hashOfTeamsToRankings[num]!!.putIfAbsent(
                        RankType.MECHANICAL_SOUNDNESS,
                        Ranker(RankType.MECHANICAL_SOUNDNESS, num, eventKey).getRank()
                    )
                }

                val matchData = manager.getDataFromEvent(DatabaseType.MATCH, eventKey)
                teams.forEach {
                    val teamKey = it["team_number"] as Int
                    var totalMatch = 0
                    val tempHash = HashMap<String, Double>()
                    matchData.forEach {
                        if ((it["team"] as String).toInt() == teamKey) {
                            it.forEach breakFor@{ (matchKey, value) ->
                                when (value) {
                                    is String -> return@breakFor
                                    is Document -> processDocument(value, tempHash, matchKey)
                                    is Double -> tempHash[matchKey] = value + (tempHash[matchKey] ?: 0.0)
                                    is Int -> tempHash[matchKey] = value + (tempHash[matchKey] ?: 0.0)
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
                }
                finalMap.forEach { (key, value) ->
                    finalMap[key] = sdScorer(value)
                    listOfWeights.putIfAbsent(key, mutableDoubleStateOf(1.0))
                }
                debug = true
            }) {
                Text("Start")
            }

            Button(onClick = {navController.navigateUp()}) {
                Text("Back")
            }
        }

        if (debug) {
            Row {
                Button(onClick = {
                    if (showTeamsRanked) {
                        showTeamsRanked = false
                    } else {
                        val weightsAsDouble = HashMap<String, Double>()
                        listOfWeights.forEach { (key, value) ->
                            weightsAsDouble[key] = value.value
                        }

                        teams = calculateList(weightsAsDouble, finalMap) as HashMap<Int, Double>

                        showTeamsRanked = true
                    }
                }) {
                    if (showTeamsRanked) {
                        Text("Edit Weights")
                    } else {
                        Text("Show Results")
                    }
                }
                Button(onClick = {
                    showMatrix = !showMatrix
                }) {
                    Text("Show Matrix")
                }
            }

            if(showMatrix) {
                Column (modifier = Modifier.fillMaxHeight().verticalScroll(matrixVerticalScrollState)) {
                    Row (modifier = Modifier.height(smallCellHeight).horizontalScroll(matrixHorizontalScrollState)) {
                        Text(" ", Modifier.border(1.dp, Color.Black).fillMaxHeight().width(cellWidth * 1.5f))
                        finalMap.values.first().forEach { (key, value) ->
                            Text(key.toString(), modifier = Modifier.border(1.dp, Color.Black).fillMaxHeight().width(cellWidth))
                        }
                    }

                    finalMap.forEach { (key, value) ->
                        Row(modifier = Modifier.height(cellHeight).horizontalScroll(matrixHorizontalScrollState)) {
                            Text(formatKey(key), modifier = Modifier.border(1.dp, Color.Black).fillMaxHeight().width(cellWidth * 1.5f))

                            value.forEach { (key, value) ->
                                Text(String.format("%.3f", value), modifier = Modifier.border(1.dp, Color.Black).fillMaxHeight().width(cellWidth), fontSize = 28.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn {
                    if (showTeamsRanked) {
                        val sortedMap = ArrayList<Entry<Int, Double>>()

                        teams.forEach {
                            sortedMap.add(it)
                        }

                        sortedMap.sortByDescending { it.value }

                        sortedMap.forEach {
                            item {
                                Text("${it.key}, ${it.value}")
                            }
                        }
                    } else {
                        val listOfWeightsAsArray = ArrayList<Entry<String, MutableDoubleState>>()

                        listOfWeights.forEach {
                            listOfWeightsAsArray.add(it)
                        }

                        val sortedListOfWeights = listOfWeightsAsArray.sortedBy { it.key }

                        sortedListOfWeights.forEach { (key, value) ->
                            item {
                                Column {
                                    Row {
                                        Text("${formatKey(key)}: ")
                                        TextField(
                                            value = value.value.toString(),
                                            onValueChange = {
                                                value.value = it.betterParseDouble()
                                            }
                                        )
                                    }
                                    Slider(
                                        value.value.toFloat(),
                                        onValueChange = { value.value = round(it.toDouble() * 100) / 100 },
                                        steps = 99,
                                        valueRange = -1f..1f
                                    )
                                }
                            }
                        }
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

        if (invalidEventError) {
            AlertDialog(
                onDismissRequest = { invalidEventError = false },
                buttons = {Button(onClick = {invalidEventError = false}) {Text("Ok")}},
                text = {Text("Invalid Event Code!")},
            )
        }
    }
}

fun calculateList(weights: Map<String, Double>, sdScores: Map<String, HashMap<Int, Double>>): Map<Int, Double> {
    val normalizedWeights = normalizeWeights(weights)

    scoring(normalizedWeights, sdScores)

    return teamScorer(sdScores)
}

fun processDocument(document: Document, hash: HashMap<String, Double>, prefix: String) {
    document.forEach { (key, value) ->
        when (value) {
            is String -> return@forEach
            is Document -> processDocument(value, hash, "$prefix $key")
            is Double -> hash["$prefix $key"] = value + (hash["$prefix $key"] ?: 0.0)
            is Int -> hash["$prefix $key"] = value + (hash["$prefix $key"] ?: 0.0)
        }
    }
}