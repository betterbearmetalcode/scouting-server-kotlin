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
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tahomarobotics.scouting.Server
import java.net.InetAddress

@Composable
fun DataCollectionScreen(navController: NavHostController) {
    var showIpDialog by remember { mutableStateOf(false) }
    var ipSubmitted by remember { mutableStateOf(false) }
    var serverStarted by remember { mutableStateOf(false) }
    var ip by remember { mutableStateOf("") }
    val dataReceived = remember { mutableStateListOf<String>() }
    Box {
        val scope = CoroutineScope(Dispatchers.Default)
        Column (modifier = Modifier.align(Alignment.TopStart)) {
            Row {
                Button(onClick = {
                    showIpDialog = true
                }) {
                    Text(text = "Start Server")
                }
                Button(onClick = { server?.stop() }) {
                    Text(text = "Stop Server")
                    serverStarted = false
                }
            }

            if (ipSubmitted && !serverStarted) {
                serverStarted = true
                scope.launch {
                    server = Server(2046, true, 2025, InetAddress.getByName(ip))
                    server!!.addListener {
                        dataReceived.add(it)
                    }
                    server!!.start()
                }
            }

            LazyColumn {
                dataReceived.forEach {
                    item {
                        Text(it)
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
                                if (Regex("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}\$") matches ip) {
                                    showIpDialog = false
                                    ipSubmitted = true
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