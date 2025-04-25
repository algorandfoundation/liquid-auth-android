package co.algorand.liquid.wallet

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.algorand.liquid.wallet.ui.credentials.CredentialScreen
import co.algorand.liquid.wallet.ui.credentials.CredentialsViewModel
import co.algorand.liquid.wallet.ui.keys.KeyScreen
import co.algorand.liquid.wallet.ui.keys.KeyViewModel
import co.algorand.liquid.wallet.ui.theme.LiquidTheme
import foundation.algorand.auth.connect.SignalService

class MainActivity : ComponentActivity() {
    private var signalService: SignalService? = null

    val scanner = AppDependencies.scanner

    private val keyManager = AppDependencies.mnemonicManager
    private var rootKey: String? = null

    private var credentialViewModel = CredentialsViewModel(
        scanner = scanner,
        keysRepository = AppDependencies.keysRepository,
    )

    private var keyViewModel = KeyViewModel(
        keysRepository = AppDependencies.keysRepository,
        keyManager = keyManager,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootKey = keyManager.fetch()?.joinToString(" ")
        Log.d("MAIN", "${rootKey}")
        enableEdgeToEdge()

        setContent {
            LiquidTheme {
                val navController = rememberNavController()
                MainApp(rootKey, navController, credentialViewModel, keyViewModel)
            }
        }
    }
}

@Composable
fun MainApp(
    rootKey: String?,
    navController: NavHostController,
    credentialsViewModel: CredentialsViewModel,
    keyViewModel: KeyViewModel,
) {
    var startDestination = "keys"
    if (rootKey != null) {
        startDestination = "credentials"
    }
    NavHost(navController = navController, startDestination = startDestination) { // Replace "home" with your starting route
        composable("credentials") {
            CredentialScreen(
                navController = navController,
                onScan = {},
                credentialsViewModel = credentialsViewModel
            )
        }
        composable("keys") { KeyScreen(
            navController = navController,
            keyViewModel = keyViewModel
        )
        }
    }
}