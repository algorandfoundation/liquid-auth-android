package co.algorand.liquid.wallet

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class MainActivity : ComponentActivity() {
    // Barcode Scanner
    val scanner = AppDependencies.scanner

    // WebRTC Service Binding
    lateinit var mConnection: ServiceConnection
    private lateinit var startServiceIntent: Intent

    // Data
    private var rootKey: String? = null
    private val keyManager = AppDependencies.mnemonicManager

    // View Models
    private var notifications = NotificationViewModel()
    private var mainViewModel = MainViewModel(notifications)
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

        mConnection = AppDependencies.mConnection

        // Check if the request was a URI DeepLink
        val isDeepLink = intent?.data != null && intent.data is Uri

        // Override security for BC
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 0)

        // Create background signal service
        startServiceIntent = Intent(this, SignalService::class.java)
        startForegroundService(startServiceIntent)
        bindService(startServiceIntent, mConnection, Context.BIND_AUTO_CREATE)

        // Retrieve the mnemonic
        rootKey = keyManager.fetch()?.joinToString(" ")
        Log.d("MAIN", "${rootKey}")
        enableEdgeToEdge()

        setContent {
            LiquidTheme {
                val navController = rememberNavController()
                MainAppView(rootKey, navController, credentialViewModel, keyViewModel) {
                    Log.d(TAG, "Handle scanner")
                    lifecycleScope.launch {
                        mainViewModel.onScan(this@MainActivity, it)

                        // Handle messages from Signal Service
                        AppDependencies.signalService.handleMessages(this@MainActivity, {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                            }
                        }, {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                            }
                        },
                            notifications.createNotificationBuilder(this@MainActivity),
                            NotificationViewModel.SERVICE_NOTIFICATION_ID,
                            MainActivity::class.java
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}

@Composable
fun MainAppView(
    rootKey: String?,
    navController: NavHostController,
    credentialsViewModel: CredentialsViewModel,
    keyViewModel: KeyViewModel,
    onScan: (uri: Uri) -> Unit = {},
) {
    var startDestination = "keys"
    if (rootKey != null) {
        startDestination = "credentials"
    }
    NavHost(navController = navController, startDestination = startDestination) { // Replace "home" with your starting route
        composable("credentials") {
            CredentialScreen(
                navController = navController,
                onScan = onScan,
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