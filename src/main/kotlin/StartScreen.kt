import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

@Composable
fun StartScreen(navController: NavHostController) {
    Box(contentAlignment = Center) {
        Column(horizontalAlignment = CenterHorizontally, modifier = Modifier.fillMaxHeight()) {

            Row(modifier = Modifier.weight(1f)) {
                Button(onClick = {}) {
                    Text("Help")
                }
            }
            Row(modifier = Modifier.weight(1f)) {
                Button(onClick = {navController.navigate(DataCollectionName)}) {
                    Text("Data Collection")
                }
            }
            Row(modifier = Modifier.weight(1f)) {
                Button(onClick = {navController.navigate(DataManagementName)}) {
                    Text("Database Management")
                }
            }
            Row(modifier = Modifier.weight(1f)) {
                Button(onClick = {navController.navigate(ChartName)}) {
                    Text("Charts")
                }
            }
            Row(modifier = Modifier.weight(1f)) {
                Button(onClick = {}) {
                    Text("Misc")
                }
            }
        }
    }
}