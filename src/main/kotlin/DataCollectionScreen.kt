import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.*
import androidx.compose.runtime.*
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
    Column {
        val scope = CoroutineScope(Dispatchers.Default)
        Button(onClick = {
            showIpDialog = true
        }) {
            Text(text = "Start Server")
        }
        if(ipSubmitted && !serverStarted) {
            scope.launch {
                val server = Server(2046, true, 2025, InetAddress.getByName(ip))
                serverStarted = true
                server.start()

            }
        }


        if (showIpDialog) {
            Dialog(onDismissRequest = { showIpDialog = false }) {
                Card {
                    Column() {
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

        Button(onClick = { navController.navigateUp() }) {
            Text("Back")
        }
    }
}