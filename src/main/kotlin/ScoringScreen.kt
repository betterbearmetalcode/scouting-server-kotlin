import SDScoring.Ranker
import SDScoring.normalizeWeights
import SDScoring.scoring
import SDScoring.teamScorer
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bson.Document
import org.tahomarobotics.scouting.DatabaseType
import org.tahomarobotics.scouting.TBAInterface
import java.awt.FileDialog
import java.io.File
import java.util.*
import kotlin.collections.Map.Entry
import kotlin.math.round
import kotlin.random.Random

@Composable
fun ScoringScreen(navController: NavController) {
    val scope = CoroutineScope(Dispatchers.Default)
    var showEmptyEventError by remember { mutableStateOf(false) }
    var invalidEventError by remember { mutableStateOf(false) }
    val finalMap: HashMap<String, HashMap<Int, Double>> = remember { HashMap() }
    var weightedMap = remember { HashMap<String, HashMap<Int, Double>>() }
    val listOfWeights = remember { mutableMapOf<String, MutableDoubleState>() }
    var debug by remember { mutableStateOf(false) }
    var tabIndex by remember { mutableStateOf(0) }
    var showWeightedMatrix by remember { mutableStateOf(false) }
    var matrixVerticalScrollState = rememberScrollState(0)
    var matrixHorizontalScrollState = rememberScrollState(0)
    val cellWidth = 90.dp
    val cellHeight = 50.dp
    val smallCellHeight = 25.dp
    var teams by remember { mutableStateOf(HashMap<Int, Double>()) }
    val cold = Color(0, 150, 255)
    val warm = Color(255, 165, 0)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Event Code:")
            TextField(value = eventCode.value, onValueChange = { eventCode.value = it })
        }
        Row {
            Button(onClick = {
                debug = false
                if (eventCode.value.isEmpty()) {
                    showEmptyEventError = true
                    return@Button
                }
                if (!TBAInterface.isValidEventKey(eventCode.value)) {
                    invalidEventError = true
                    return@Button
                }
                val play: EnumMap<RankType, HashMap<Int, HashMap<Int, HashMap<Boolean, Int>>>> =
                    EnumMap(RankType::class.java)
                play.putIfAbsent(RankType.STRATEGY, HashMap())
                play.putIfAbsent(RankType.DRIVING_SKILL, HashMap())
                play.putIfAbsent(RankType.MECHANICAL_SOUNDNESS, HashMap())
                val stratInfo = manager.getDataFromEvent(DatabaseType.STRATEGY, eventCode.value)

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
                val teams = manager.getDataFromEvent(DatabaseType.TEAMS, eventCode.value)
                teams.forEach {
                    val num = it["team_number"] as Int
                    if (!(play[RankType.STRATEGY]!!.keys.contains(num))) {
                        return@forEach
                    }
                    hashOfTeamsToRankings.putIfAbsent(num, EnumMap(RankType::class.java))

                    hashOfTeamsToRankings[num]!!.putIfAbsent(
                        RankType.STRATEGY, Ranker(RankType.STRATEGY, num, eventCode.value).getRank()
                    )
                    hashOfTeamsToRankings[num]!!.putIfAbsent(
                        RankType.DRIVING_SKILL, Ranker(RankType.DRIVING_SKILL, num, eventCode.value).getRank()
                    )
                    hashOfTeamsToRankings[num]!!.putIfAbsent(
                        RankType.MECHANICAL_SOUNDNESS,
                        Ranker(RankType.MECHANICAL_SOUNDNESS, num, eventCode.value).getRank()
                    )
                }
                hashOfTeamsToRankings.forEach { (key, value) ->
                    value.forEach {
                        finalMap.putIfAbsent(it.key.toString(), HashMap())
                        finalMap[it.key.toString()]!![key] = it.value
                    }
                }
                val matchData = manager.getDataFromEvent(DatabaseType.MATCH, eventCode.value)
                teams.forEach {
                    val teamKey = it["team_number"] as Int
                    var totalMatch = 0
                    val tempHash = HashMap<String, Double>()
                    matchData.forEach {
                        if ((it["team"] as String).toInt() == teamKey) {
                            it.forEach breakFor@{ (matchKey, value) ->
                                if (matchKey == "_id") return@breakFor
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
                    listOfWeights.putIfAbsent(key, mutableDoubleStateOf(Random.nextDouble(-1.0, 1.0)))
                }
                debug = true
            }) {
                Text("Start")
            }

            Button(onClick = { navController.navigateUp() }) {
                Text("Back")
            }
        }

        if (debug) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { genExcelFile(finalMap) }) {
                    Text("Export compiled strat data")
                }
                Button(
                    onClick = {
                        genCSVFile(
                            HashMap(listOfWeights.mapValues { it.value.value }), "weights"
                        )
                    }) {
                    Text("Export weights")
                }
                Button(
                    onClick = {
                        val files = openFileDialog(
                            window = ComposeWindow(), title = "Import Weights", allowedExtensions = listOf("csv")
                        )
                        if (files.isNotEmpty()) {
                            listOfWeights.clear()
                            val file = files.first()
                            val reader = file.bufferedReader()
                            val lines = reader.readLines()
                            for (line in lines) {
                                val parts = line.split(",")
                                if (parts.size == 2) {
                                    val key = parts[0]
                                    val value = parts[1].toDouble()
                                    listOfWeights[key] = mutableDoubleStateOf(value)
                                }
                            }
                        }
                    }) {
                    Text("Import weights")
                }
                Button(
                    onClick = {
                        genCSVFile(
                            teams.entries.sortedByDescending { it.value }.map { it.key.toString() },
                            "picklist-${eventCode.value}"
                        )
                    }) {
                    Text("Export Picklist")
                }
                Button(
                    onClick = {
                        val files = openFileDialog(
                            window = ComposeWindow(),
                            title = "Import Target Picklist",
                            allowedExtensions = listOf("csv")
                        )
                        if (files.isNotEmpty()) {
                            val file = files.first()
                            val reader = file.bufferedReader()
                            val lines = reader.readLines()
                            val targetPicklist = lines.map { it.toInt() }

                            val unmutableList = HashMap<String, Double>()

                            listOfWeights.forEach {
                                unmutableList[it.key] = it.value.value
                            }

                            scope.launch {
                                val output = train(unmutableList, targetPicklist, finalMap, 1000, 50)
                                output.forEach {
                                    listOfWeights[it.key] = mutableDoubleStateOf(it.value)
                                }
                            }
                        }
                    }) {
                    Text("Start Training")
                }
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(selected = tabIndex == 0, onClick = {
                        tabIndex = 0
                    }) {
                        Text("Weights")
                    }
                    Tab(selected = tabIndex == 1, onClick = {
                        tabIndex = 1
                        for (entry in finalMap) {
                            weightedMap[entry.key] = entry.value.clone() as HashMap<Int, Double>
                        }
                        val weightsAsDouble = HashMap<String, Double>()
                        listOfWeights.forEach { (key, value) ->
                            weightsAsDouble[key] = value.value
                        }
                        teams = calculateList(weightsAsDouble, weightedMap) as HashMap<Int, Double>
                    }) {
                        Text("Picklist")
                    }
                    Tab(selected = tabIndex == 2, onClick = {
                        tabIndex = 2
                        for (entry in finalMap) {
                            weightedMap[entry.key] = entry.value.clone() as HashMap<Int, Double>
                        }
                        val weightsAsDouble = HashMap<String, Double>()
                        listOfWeights.forEach { (key, value) ->
                            weightsAsDouble[key] = value.value
                        }
                        teams = calculateList(weightsAsDouble, weightedMap) as HashMap<Int, Double>
                    }) {
                        Text("Matrix")
                    }
                }
            }
            if (tabIndex == 0) {
                LazyColumn {
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
                                        value = value.value.toString(), onValueChange = {
                                            value.value = it.betterParseDouble()
                                        })
                                }
                                Slider(
                                    value.value.toFloat(),
                                    onValueChange = { value.value = round(it.toDouble() * 100) / 100 },
                                    steps = 99,
                                    valueRange = -1f..1f,
                                )
                            }
                        }
                    }
                }
            }
            else if (tabIndex == 1) {
                LazyColumn {
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
                }
            }
            else if (tabIndex == 2) {
                Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Use Weights")
                    Switch(showWeightedMatrix, onCheckedChange = { showWeightedMatrix = !showWeightedMatrix })
                }
                Column(modifier = Modifier.fillMaxHeight().verticalScroll(matrixVerticalScrollState)) {
                    Row(modifier = Modifier.height(smallCellHeight).horizontalScroll(matrixHorizontalScrollState)) {
                        Text(" ", Modifier.border(1.dp, Color.Black).fillMaxHeight().width(cellWidth * 1.5f))
                        (if (showWeightedMatrix) weightedMap else finalMap).values.first().forEach { (key, value) ->
                            Text(
                                key.toString(),
                                modifier = Modifier.border(1.dp, Color.Black).fillMaxHeight().width(cellWidth)
                            )
                        }
                    }

                    (if (showWeightedMatrix) weightedMap else finalMap).forEach { (key, rowValue) ->
                        Row(modifier = Modifier.height(cellHeight).horizontalScroll(matrixHorizontalScrollState)) {
                            Text(
                                formatKey(key),
                                modifier = Modifier.border(1.dp, Color.Black).fillMaxHeight().width(cellWidth * 1.5f)
                            )

                            rowValue.forEach { (key, value) ->
                                Text(
                                    String.format("%.3f", value * if (showWeightedMatrix) 10 else 1),
                                    modifier = Modifier.border(1.dp, Color.Black).fillMaxHeight().width(cellWidth)
                                        .background(
                                            colorLerp(
                                                cold,
                                                warm,
                                                if (showWeightedMatrix) ((value.toFloat() * 10) - (rowValue.values.min()
                                                    .toFloat() * 10)) / (rowValue.values.max().toFloat() * 10)
                                                else (value.toFloat() - rowValue.values.min()
                                                    .toFloat()) / rowValue.values.max().toFloat()
                                            )
                                        ),
                                    fontSize = 28.sp
                                )
                            }
                        }
                    }
                }
            }
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
}

// From https://www.reddit.com/r/Kotlin/comments/n16u8z/desktop_compose_file_picker/?rdt=55691
fun openFileDialog(
    window: ComposeWindow, title: String, allowedExtensions: List<String>, allowMultiSelection: Boolean = true
): Set<File> {
    return FileDialog(window, title, FileDialog.LOAD).apply {
        isMultipleMode = allowMultiSelection

        // windows
        file = allowedExtensions.joinToString(";") { "*$it" } // e.g. '*.jpg'

        // linux
        setFilenameFilter { _, name ->
            allowedExtensions.any {
                name.endsWith(it)
            }
        }

        isVisible = true
    }.files.toSet()
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

fun colorLerp(cold: Color, warm: Color, v: Float): Color {
    return Color(
        (warm.red - cold.red) * v + cold.red,
        (warm.green - cold.green) * v + cold.green,
        (warm.blue - cold.blue) * v + cold.blue
    )
}