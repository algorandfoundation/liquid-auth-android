package co.algorand.liquid.wallet

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import co.algorand.liquid.wallet.fido.Cookies
import co.algorand.liquid.wallet.ui.home.HomeScreen
import co.algorand.liquid.wallet.ui.home.HomeViewModel
import co.algorand.liquid.wallet.ui.theme.LiquidTheme
import foundation.algorand.auth.connect.SignalService
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {
    private var signalService: SignalService? = null

    private var viewModel = HomeViewModel(
        credentialRepository = AppDependencies.credentialsRepository,
        RPIconDataSource = AppDependencies.rpIconDataSource
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startIntent = Intent(this, SignalService::class.java)
        startService(startIntent)
        bindService(startIntent, object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName) {
                signalService = null
            }

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                val mLocalBinder = service as SignalService.LocalBinder
                signalService = mLocalBinder.getServerInstance()
            }
        }, BIND_AUTO_CREATE)

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