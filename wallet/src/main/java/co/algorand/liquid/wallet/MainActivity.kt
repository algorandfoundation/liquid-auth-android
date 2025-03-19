package co.algorand.liquid.wallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import co.algorand.liquid.wallet.ui.home.HomeScreen
import co.algorand.liquid.wallet.ui.home.HomeViewModel
import co.algorand.liquid.wallet.ui.theme.LiquidTheme

class MainActivity : ComponentActivity() {
    private var viewModel = HomeViewModel(
        credentialRepository = AppDependencies.credentialsRepository,
        RPIconDataSource = AppDependencies.rpIconDataSource
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiquidTheme {
                    HomeScreen(
                        homeViewModel = viewModel,
                        openDrawer = {},
                    )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LiquidTheme {
        Greeting("Android")
    }
}