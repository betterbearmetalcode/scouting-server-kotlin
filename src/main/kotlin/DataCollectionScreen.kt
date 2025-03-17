import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import composables.Accordian
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bson.json.JsonObject
import org.tahomarobotics.scouting.Server
import java.net.InetAddress


@Composable
fun DataCollectionScreen(navController: NavHostController) {
    var showIpDialog by remember { mutableStateOf(false) }
    var ipSubmitted by remember { mutableStateOf(false) }
    var serverStarted = server?.isRunning == true
    var ip by remember { mutableStateOf(server?.inetAddress?.hostAddress ?: "") }
    val dataReceived = remember { mutableStateListOf<String>() }
    var serverRunningText by remember { if (serverStarted) { mutableStateOf("Server Running on $ip") } else { mutableStateOf("Server Disabled") } }
    val textStyleRed = TextStyle(color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    val textStyleGreen = TextStyle(color = Color(0,175,0), fontWeight = FontWeight.Bold, fontSize = 18.sp)
    var serverRunningTextStyle by remember { if (serverStarted) {mutableStateOf(textStyleGreen)} else { mutableStateOf(textStyleRed) } }

    Box {
        val scope = CoroutineScope(Dispatchers.Default)
        Column (modifier = Modifier.align(Alignment.TopStart)) {
            Row {
                Button(onClick = {
                    showIpDialog = true
                }) {
                    Text(text = "Start Server")
                }
                Button(onClick = {
                    scope.launch {
                        server?.stop()
                        serverStarted = false
                        serverRunningText = "Server Disabled"
                        serverRunningTextStyle = textStyleRed
                    }
                }) {
                    Text(text = "Stop Server")
                }
                Text(
                    text = serverRunningText,
                    modifier = Modifier.align(Alignment.CenterVertically),
                    style = serverRunningTextStyle
                )
            }

            LazyColumn {
                var i = 0
                val gson = GsonBuilder().setPrettyPrinting().create()
                dataReceived.forEach {
                    item {
                        var accordian = true
                        var header = ""
                        var data : com.google.gson.JsonObject = com.google.gson.JsonObject()
                        try {
                            val jsonObject = gson.fromJson(it, com.google.gson.JsonObject::class.java)!!;

                            try {
                                val headerElement: JsonElement? = jsonObject["header"]

                                header = headerElement!!.getAsJsonObject().get("h0").asString
                            } catch (_: NullPointerException) {
                            }

                            val temp = jsonObject["data"].toString()
                            data = gson.fromJson(temp, com.google.gson.JsonObject::class.java)
                        } catch (_ : NullPointerException) {
                            accordian = false
                        }
                        if (accordian) {
                            when (header) {
                                "match" -> {
                                    Accordian(labelContents = {
                                        Text("Match #${data["match"].asString.replace("\"", "") } at position ${startPosToString(data["robotStartPosition"].asInt)}. Team #${data["team"].asString.replace("\"", "")}", fontSize = 15.sp)
                                    }, innerContents = {
                                        Text(gson.toJson(data))
                                    })
                                }
                                "strat" -> {
                                    Accordian(labelContents = {
                                        Text("Strategy Scouting for Match #${data["match"]} on the ${if(data["is_red_alliance"].asBoolean) "Blue" else "Red"} alliance")
                                    }) {
                                        Text(gson.toJson(data))
                                    }
                                }
                            }
                        } else {
                            Text("Error displaying data for this match")
                        }
                    }
                }
            }
        }


        if (showIpDialog) {
            Dialog(onDismissRequest = { showIpDialog = false }) {
                Card {
                    Column {
                        TextField(
                            value = ip,
                            onValueChange = { ip = it }
                        )
                        Row {
                            Button(onClick = {
                                if (Regex("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$") matches ip) {
                                    showIpDialog = false
                                    ipSubmitted = true
                                    if (!serverStarted) {
                                        serverStarted = true
                                        serverRunningText = "Server Running on $ip"
                                        serverRunningTextStyle = textStyleGreen
                                        server = Server(2046, true, 2025, InetAddress.getByName(ip))
                                        scope.launch {
                                            server!!.addListener {
                                                dataReceived.add(it)
                                            }
                                            server!!.start()
                                        }
                                    }
                                }
                            }) {
                                Text("Submit")
                            }

                            Button(onClick = { showIpDialog = false }) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }

        Button(onClick = { navController.navigateUp() }, modifier = Modifier.align(Alignment.BottomStart)) {
            Text("Back")
        }
    }
}