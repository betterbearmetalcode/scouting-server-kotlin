import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import composables.PiChart
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun Chart(navController: NavHostController) {
    var showPiDialog by remember { mutableStateOf(false) }
    val piCharts = remember { mutableListOf<Array<Float>>() }
    println(piCharts.size)
    Box(modifier = Modifier.fillMaxSize()) {
        if (showPiDialog) {
            Dialog(onDismissRequest = { showPiDialog = false }) {
                var dropExpanded by remember { mutableStateOf(false) }
                var selectedValue by remember { mutableStateOf("") }
                Card {
                    Column() {
                        Button (onClick = {dropExpanded = true}) {
                            Text(selectedValue)
                            DropdownMenu(expanded = dropExpanded, onDismissRequest = { dropExpanded = false }) {
                                getMeasuredValues().forEach {
                                    DropdownMenuItem(onClick = { selectedValue = it; dropExpanded = false}) {
                                        Text(text = it)
                                    }
                                }
                            }
                        }
                        Button(onClick = { piCharts.add(getValuesFromType(selectedValue)); showPiDialog = false }) {
                            Text("Create Chart")
                        }
                    }
                }
            }
        }

        Button(
            onClick = {navController.navigateUp()},
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text("Back")
        }
        Column (modifier = Modifier.align(Alignment.TopCenter)) {
            Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Button(
                    onClick = { showPiDialog = true },
                ) {
                    Text("Make Pi Chart")
                }
                Button(
                    onClick = {}
                ) {
                    Text("Make Line Chart")
                }
            }

            LazyRow(modifier = Modifier.height(500.dp)) {
                items(items = piCharts) { item ->
                    PiChart(
                        modifier = Modifier.size(500.dp),
                        values = item,
                        colors = arrayOf(Color.Red, Color.Green, Color.Blue, Color.White)
                    )
                    println("Pichart")
                }
            }
        }
    }
}


fun getMeasuredValues() : ArrayList<String> {
    return arrayListOf("L1", "L2", "L3", "L4") // TODO Get measured values from database
}

fun getValuesFromType(type: String) : Array<Float> {
    val values = Array(size = ceil(Math.random()*10).toInt(), init = {0f})
    repeat(values.size) {
        values[it] = (Math.random()*23).toFloat()
    }
    return values // TODO Don't use random numbers
}