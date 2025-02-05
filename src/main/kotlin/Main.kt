import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tahomarobotics.scouting.Server
import java.net.InetAddress



@Composable
@Preview
fun App() {
    MaterialTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = StartName,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(route = StartName) {
                StartScreen(navController)
            }
            composable(route = ChartName) {
                Chart(navController)
            }
            composable(route = DataCollectionName) {
                DataCollectionScreen(navController)
            }
        }
    }
}

var server: Server? = null
val chartValues = arrayOf(3,5,2,6,3,75)
val chartColors = arrayOf(Color.Red, Color.Green, Color.Blue, Color.White)


fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
