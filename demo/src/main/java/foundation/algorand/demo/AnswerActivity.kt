package foundation.algorand.demo

import android.app.*
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.StrictMode
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.algorand.algosdk.account.Account
import com.algorand.algosdk.transaction.Transaction
import com.algorand.algosdk.util.Encoder
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.Fido2ApiClient
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import foundation.algorand.auth.Cookie
import foundation.algorand.auth.connect.AuthMessage
import foundation.algorand.auth.connect.SignalClient
import foundation.algorand.auth.crypto.decodeBase64
import foundation.algorand.auth.fido2.AssertionApi
import foundation.algorand.auth.fido2.AttestationApi
import foundation.algorand.auth.fido2.toPublicKeyCredentialCreationOptions
import foundation.algorand.auth.fido2.toPublicKeyCredentialRequestOptions
import foundation.algorand.demo.credential.CredentialRepository
import foundation.algorand.demo.credential.db.Credential
import foundation.algorand.demo.credential.db.CredentialDatabase
import foundation.algorand.demo.crypto.KeyPairs
import foundation.algorand.demo.databinding.ActivityAnswerBinding
import foundation.algorand.demo.services.LiquidWebRTCService
import foundation.algorand.demo.settings.AccountDialogFragment
import foundation.algorand.demo.settings.NotificationsDialogFragment
import foundation.algorand.demo.settings.SettingsDialogFragment
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import org.apache.commons.codec.binary.Base64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.DataChannel
import ru.gildor.coroutines.okhttp.await
import java.security.Security
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AnswerActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "AnswerActivity"
        private const val SHARED_PREFERENCE_SEED_FILE = "ACCOUNT_SEEDS"
    }

    // Liquid Auth Service
    private var mBounded = false
    private var liquidWebRTCService: LiquidWebRTCService? = null
    private var mConnection: ServiceConnection? = null

    // Data Models
    private lateinit var db: CredentialDatabase
    private val credentialRepository = CredentialRepository() // Handle Credential Operations
    private val viewModel: AnswerViewModel by viewModels()    // Handle View State
    private val wallet: WalletViewModel by viewModels()       // Handle Wallet Operations

    // Fragments/Bindings
    private lateinit var accountDialogFragment: AccountDialogFragment
    private lateinit var binding: ActivityAnswerBinding

    // Third Party APIs
    private var httpClient = OkHttpClient.Builder()
        .cookieJar(Cookies())
        .build()
    private lateinit var scanner: GmsBarcodeScanner
    private lateinit var promptInfo: BiometricPrompt.PromptInfo


    // FIDO/Auth interfaces
    private var fido2Client: Fido2ApiClient? = null
    private var signalClient: SignalClient? = null
    private val attestationApi = AttestationApi(httpClient)
    private val assertionApi = AssertionApi(httpClient)
    private val userAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} " +
            "(Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; ${Build.BRAND})"
    private var signature: ByteArray? = null

    // Register/Attestation Intent Launcher
    private val attestationIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ::handleAuthenticatorAttestationResult
    )

    // Authenticate/Assertion Intent Channel
    private val assertionIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ::handleAuthenticatorAssertionResult
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set Security
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 0)

        // Create FIDO Client, TODO: refactor to Credential Manager
        fido2Client = Fido2ApiClient(this@AnswerActivity)
        scanner = GmsBarcodeScanning.getClient(this@AnswerActivity)

        // Load the Shared Preferences
        hydrateSharedPreferences()

        // Create Fragments
        accountDialogFragment =
            AccountDialogFragment(wallet.account.value!!, wallet.rekey.value!!, wallet.selected.value!!)

        // Ensure the device has notifications enabled
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.areNotificationsEnabled()) {
            val notificationsDialogFragment = NotificationsDialogFragment(packageName)
            if (!notificationsDialogFragment.isVisible) {
                notificationsDialogFragment.show(supportFragmentManager, "NOTIFICATIONS")
            }
        }
        // Ensure the device is secure to access FIDO/Passkeys
        val keyguardManager = this@AnswerActivity.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguardManager.isDeviceSecure) {
            val settingsDialogFragment = SettingsDialogFragment()
            if (!settingsDialogFragment.isVisible) {
                settingsDialogFragment.show(supportFragmentManager, "CREATED")
            }
        }

        // Load the existing credentials
        lifecycleScope.launch {
            db = CredentialDatabase.getInstance(this@AnswerActivity)
            val credentials = db.credentialDao().getAll()
            credentials.collect() { credentialList ->
                Log.d(TAG, "db: $credentialList")
                val credArray = credentialList.map {
                    val user = it.userHandle
                    val origin = it.origin
                    "$user@$origin"
                } as MutableList<String>
                if (credArray.isEmpty()) {
                    credArray.add("No Credentials Found, scan a QR code to register a new credential.")
                }
                val listView = findViewById<ListView>(R.id.listView)
                val adapter: ArrayAdapter<*> = ArrayAdapter<String>(
                    this@AnswerActivity,
                    android.R.layout.simple_list_item_1,
                    android.R.id.text1,
                    credArray
                )
                listView.adapter = adapter
            }
        }
        // View Bindings
        binding = ActivityAnswerBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.connectButton.setOnClickListener {
            connect()
        }
        setContentView(binding.root)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        initWebRTCService {
            hydrateIntents()
        }
    }

    /**
     * Reload the application state from an Intent
     */
    private fun hydrateIntents() {
        val isConnected = liquidWebRTCService?.dataChannel is DataChannel && liquidWebRTCService?.dataChannel?.state() === DataChannel.State.OPEN
        val isIntent = intent != null
        val isDeepLink = intent?.data != null && intent.data is Uri
        val isDataChannelMessage = intent?.getStringExtra("msg") != null
        if (isDeepLink) {
            val intentUri = intent.data as Uri
            Log.d(TAG, "Intent Detected: $intentUri")

            // Find the Application ID in the Intent Extras
            if (intent.extras is Bundle) {
                val bundle = intent.extras as Bundle
                val keySet = bundle.keySet().toTypedArray()
                for (k in keySet) {
                    if (k.contains("application_id")) {
                        bundle.getString(k)?.let { appId ->
                            liquidWebRTCService!!.updateLastKnownReferer(appId)
                        }
                    }

                }
            }
            // Find the Referrer in the Activity
            this@AnswerActivity.referrer?.let {
                Log.d(TAG, "Referrer: $it")
                liquidWebRTCService!!.updateLastKnownReferer(it.toString())
            }

            // Set the Message and Start the Service
            val msg = AuthMessage.fromUri(intentUri)
            viewModel.setMessage(msg)
            liquidWebRTCService?.start(msg.origin, httpClient)

            // Launch the authentication process
            lifecycleScope.launch {
                val savedCredential = credentialRepository.getCredentialByOrigin(this@AnswerActivity, msg.origin)
                if (savedCredential === null) {
                    register(msg)
                } else {
                    authenticate(msg, savedCredential)
                }
            }
        }
        // Handle the app relaunching
        if ( isConnected && !isIntent) {
            liquidWebRTCService?.handleMessages(this@AnswerActivity, {
                this@AnswerActivity.handleMessages(it)
            })
        }

        // Handle a datachannel message
        if(isDataChannelMessage) {
            val msg = intent.getStringExtra("msg")
            if (msg !== null) {
                handleMessages(msg)
            }
        }
    }

    /**
     * Initialize the WebRTC Service
     *
     * This checks for a bound service and starts the service if it is not already running.
     */
    private fun initWebRTCService(onServiceConnection: () -> Unit) {
        // Check if the service is already bound
        if (mBounded) {
            return
        }
        // Handle the Service Connection
        mConnection = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName) {
                mBounded = false
                liquidWebRTCService = null
            }

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                mBounded = true
                val mLocalBinder = service as LiquidWebRTCService.LocalBinder
                liquidWebRTCService = mLocalBinder.getServerInstance()
                onServiceConnection()
            }
        }
        val startIntent = Intent(this, LiquidWebRTCService::class.java)
        startService(startIntent)
        bindService(startIntent, mConnection as ServiceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Load seed phrases from SharedPreferences
     * This is not recommended in production applications, it is just for demonstration purposes.
     */
    private fun hydrateSharedPreferences() {
        val sharedPref = getSharedPreferences(SHARED_PREFERENCE_SEED_FILE, Context.MODE_PRIVATE)
        // Load the stored seed phrases
        sharedPref.getString("MAIN_ACCOUNT", null)?.let {
            wallet.setAccount(Account(it))
        } ?: run {
            val account = Account()
            sharedPref.edit().putString("MAIN_ACCOUNT", account.toMnemonic()).apply()
            wallet.setAccount(account)
        }
        sharedPref.getString("REKEY_ACCOUNT", null)?.let {
            wallet.setRekey(Account(it))
        } ?: run {
            val account = Account()
            sharedPref.edit().putString("REKEY_ACCOUNT", account.toMnemonic()).apply()
            wallet.setRekey(account)
        }
        sharedPref.getString("SELECTED_ACCOUNT", null)?.let {
            if (wallet.rekey.value!!.address.toString() == it) {
                wallet.setSelected(wallet.rekey.value!!)
            } else {
                wallet.setSelected(wallet.account.value!!)
            }
        } ?: run {
            sharedPref.edit().putString("SELECTED_ACCOUNT", wallet.account.value!!.address.toString()).apply()
            wallet.setSelected(wallet.account.value!!)
        }
    }

    /**
     * Show the Account Settings Fragment
     */
    private fun toggleAccountDialogFragment() {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        transaction
            .add(android.R.id.content, accountDialogFragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * Switch the Activity type
     */
    private fun handleSwitchActivity() {
        val switchIntent = Intent(this, OfferActivity::class.java)
        switchIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(switchIntent)
    }

    /**
     * Algorand Specific Rekey
     */
    private fun handleRekey() {
        val result = wallet.algod.AccountInformation(wallet.account.value!!.address).execute()
        if (!result.isSuccessful) {
            Toast.makeText(this@AnswerActivity, "Error getting account information", Toast.LENGTH_LONG).show()
            return
        }
        val accountInfo = result.body()
        if (accountInfo.amount < 1000) {
            Toast.makeText(this@AnswerActivity, "Insufficient Funds", Toast.LENGTH_LONG).show()
            return
        }
        // Rekey Main Account to the Rekey Account
        if (wallet.account.value!!.address === wallet.selected.value!!.address) {
            wallet.rekey(wallet.account.value!!, wallet.rekey.value!!)
            Toast.makeText(this@AnswerActivity, "Rekeyed to ${wallet.rekey.value!!.address}", Toast.LENGTH_LONG).show()
            // Rekey back to the Main Account from the Rekey Account
        } else {
            wallet.rekey(wallet.account.value!!, wallet.account.value!!, wallet.rekey.value!!)
            Toast.makeText(this@AnswerActivity, "Removed Rekey", Toast.LENGTH_LONG).show()
        }
        getSharedPreferences(SHARED_PREFERENCE_SEED_FILE, Context.MODE_PRIVATE).edit().putString("SELECTED_ACCOUNT", wallet.selected.value!!.address.toString()).apply()
    }

    /**
     * Navigate to the Algorand Dispenser
     */
    private fun handleOpenDispenser() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Address", wallet.account.value!!.address.toString()))
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bank.testnet.algorand.network"))
        startActivity(browserIntent)
    }

    /**
     * Navigate to the Account Explorer
     */
    private fun handleAccountExplorer() {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://testnet.explorer.perawallet.app/address/${wallet.account.value!!.address}")
        )
        startActivity(browserIntent)
    }

    /**
     * Handle Menu Options
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection.
        return when (item.itemId) {
            R.id.switchButton -> {
                handleSwitchActivity()
                true
            }

            R.id.rekeyButton -> {
                handleRekey()
                true
            }

            R.id.accountButton -> {
                toggleAccountDialogFragment()
                true
            }

            R.id.accountExplorerButton -> {
                handleAccountExplorer()
                true
            }

            R.id.dispenserButton -> {
                handleOpenDispenser()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Transaction Biometric Prompt
     */
    private suspend fun biometrics(txn: Transaction): BiometricPrompt.AuthenticationResult? {
        return suspendCoroutine { continuation ->
            var biometricPrompt = BiometricPrompt(this@AnswerActivity, ContextCompat.getMainExecutor(this@AnswerActivity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        super.onAuthenticationSucceeded(result)
                        continuation.resume(result)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        continuation.resume(null)
                    }
                })
            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("${txn.type} Transaction ${txn.assetIndex}")
                .setSubtitle(
                    "From: ${txn.sender.toString().substring(0, 4)} To: ${
                        txn.receiver.toString().substring(0, 4)
                    } Amount: ${txn.amount}"
                )
                .setNegativeButtonText("Cancel")
                .build()
            biometricPrompt.authenticate(promptInfo)
        }
    }

    /**
     * Decode Unsigned Transaction
     */
    private fun decodeUnsignedTransaction(unsignedTxn: String): Transaction? {
        return Encoder.decodeFromMsgPack(unsignedTxn.decodeBase64(), Transaction::class.java)
    }

    /**
     * Handle Messages
     *
     * Callback for datachannel messages
     */
    private fun handleMessages(msgStr: String) {
        val keyPair = KeyPairs.getKeyPair(wallet.selected.value!!.toMnemonic())
        try {
            val message = JSONObject(msgStr)
            if (message.get("type") == "transaction") {
                lifecycleScope.launch {
                    // Decode the Transaction
                    val txn = decodeUnsignedTransaction(message.get("txn").toString())
                    // Display a biometric prompt with some transaction details
                    val biometricResult = biometrics(txn!!)
                    if (biometricResult !== null) {
                        val bytes = txn.bytesToSign()
                        val signatureBytes = KeyPairs.rawSignBytes(bytes, keyPair.private)
                        val sig = Base64.encodeBase64URLSafeString(signatureBytes)
                        val responseObj = JSONObject()
                        responseObj.put("sig", sig)
                        responseObj.put("txId", txn.txID())
                        responseObj.put("type", "transaction-signature")
                        liquidWebRTCService!!.send(responseObj.toString())
                        // Is Notification Intent(not Deep Link)
                        if (intent?.data == null && liquidWebRTCService!!.isDeepLink) {
                            intent?.let { deepLinkIntent ->
                                if (deepLinkIntent.getStringExtra("msg") !== null) {
                                    liquidWebRTCService!!.lastKnownReferer?.let { referer ->
                                        this@AnswerActivity.finish()
                                        val browserIntent = packageManager.getLaunchIntentForPackage(
                                            referer.replace(
                                                "android-app://",
                                                ""
                                            )
                                        )
                                        browserIntent?.let { openBrowser ->
                                            openBrowser.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                            startActivity(browserIntent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@AnswerActivity, msgStr, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: $e")
        }
    }

    /**
     * Connect/Proof of Knowledge API
     *
     * Connects the Wallet/Android Application to a dApp/website using a Barcode.
     * The barcode must use the liquid uri scheme and contain a request id.
     *
     * liquid://<ORIGIN>/?requestId=<REQUEST_ID>
     *
     * In Android 14, the application can handle the FIDO:/ URI scheme directly.
     * This is useful when a user is registering the phone as an Authenticator for the first time.
     */
    private fun connect() {
        GmsBarcodeScanning.getClient(this@AnswerActivity).startScan()
            .addOnSuccessListener { barcode ->
                // Handle any scanned FIDO URI directly
                if (barcode.displayValue!!.startsWith("FIDO:/")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(barcode.displayValue)))
                    } else {
                        Toast.makeText(this@AnswerActivity, "Android 14 Required", Toast.LENGTH_LONG).show()
                    }
                // Handle Liquid Auth URI
                } else {
                    // Decode Barcode Message
                    val msg = AuthMessage.fromBarcode(barcode)
                    viewModel.setMessage(msg)
                    liquidWebRTCService!!.updateDeepLinkFlag(false)
                    liquidWebRTCService?.start(msg.origin, httpClient)
                    // Connect to Service
                    lifecycleScope.launch {
                        val savedCredential =
                            credentialRepository.getCredentialByOrigin(this@AnswerActivity, msg.origin)
                        signalClient = SignalClient(msg.origin, this@AnswerActivity, httpClient)
                        if (savedCredential === null) {
                            register(msg)
                        } else {
                            authenticate(msg, savedCredential)
                        }
                    }
                }
            }
            .addOnCanceledListener {
                Toast.makeText(this@AnswerActivity, "Canceled", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@AnswerActivity, e.message, Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Registration of a new Credential (Step 1 of 2)
     *
     * Receives PublicKeyCredentialCreationOptions from the FIDO2 Server and launches
     * the authenticator Intent using the handleAuthenticatorAttestationResult Handler
     */
    private suspend fun register(msg: AuthMessage, options: JSONObject = JSONObject()) {
        val account = wallet.account.value!!
        val selected = wallet.selected.value!!
        Log.d(TAG, "Registering new Credential with ${account.address} at ${msg.origin}")

        // Create Options for FIDO2 Server
        options.put("username", account.address.toString())
        options.put("displayName", "Liquid Auth User")
        options.put("authenticatorSelection", JSONObject().put("userVerification", "required"))
        val extensions = JSONObject()
        extensions.put("liquid", true)
        options.put("extensions", extensions)

        // FIDO2 Server API Response for PublicKeyCredentialCreationOptions
        val response = attestationApi.postAttestationOptions(msg.origin, userAgent, options).await()
        val session = Cookie.fromResponse(response)
        session?.let {
            setSession(Cookie.getID(it))
        }
        // Convert ResponseBody to FIDO2 PublicKeyCredentialCreationOptions
        val pubKeyCredentialCreationOptions = response.body!!.toPublicKeyCredentialCreationOptions()
        // Sign the challenge with the algorand account, this is used in the liquid FIDO2 extension
        signature = KeyPairs.rawSignBytes(
            pubKeyCredentialCreationOptions.challenge,
            KeyPairs.getKeyPair(selected.toMnemonic()).private
        )
        // Kick off FIDO2 Client Intent
        val pendingIntent = fido2Client!!.getRegisterPendingIntent(pubKeyCredentialCreationOptions).await()
        attestationIntentLauncher.launch(
            IntentSenderRequest.Builder(pendingIntent)
                .build()
        )
    }

    /**
     * Registration of a New Credential (Step 2 of 2)
     *
     * Handles the ActivityResult from a FIDO2 Intent and submits
     * the Authenticator's PublicKeyCredential to the FIDO2 Server
     */
    private fun handleAuthenticatorAttestationResult(activityResult: ActivityResult) {
        val bytes = activityResult.data?.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)

        when {
            activityResult.resultCode != Activity.RESULT_OK ->
                Toast.makeText(this@AnswerActivity, "Canceled", Toast.LENGTH_LONG).show()

            bytes == null ->
                Toast.makeText(this@AnswerActivity, "Error", Toast.LENGTH_LONG)
                    .show()

            else -> {
                // Handle PublicKeyCredential Response from Authenticator
                val credential = PublicKeyCredential.deserializeFromBytes(bytes)
                val response = credential.response
                if (response is AuthenticatorErrorResponse) {
                    if (response.errorCode === ErrorCode.UNKNOWN_ERR) {
                        Toast.makeText(this@AnswerActivity, "Something Went Wrong", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@AnswerActivity, response.errorMessage, Toast.LENGTH_LONG).show()
                    }
                } else {
                    if (signature === null) {
                        Toast.makeText(this@AnswerActivity, "Signature is null", Toast.LENGTH_LONG).show()
                        return
                    }
                    val msg = viewModel.message.value!!
                    val account = wallet.account.value!!
                    // Create the Liquid Extension JSON
                    val liquidExtJSON = JSONObject()
                    liquidExtJSON.put("type", "algorand")
                    liquidExtJSON.put("requestId", msg.requestId)
                    liquidExtJSON.put("address", account.address.toString())
                    liquidExtJSON.put("signature", Base64.encodeBase64URLSafeString(signature!!))
                    liquidExtJSON.put("device", Build.MODEL)

                    lifecycleScope.launch {
                        // POST Authenticator Results to FIDO2 API
                        attestationApi.postAttestationResult(
                            msg.origin,
                            userAgent,
                            credential,
                            liquidExtJSON
                        ).await()
                        viewModel.saveCredential(this@AnswerActivity, wallet.account.value!!, credential)
                        if (mBounded) {
                            liquidWebRTCService?.peer(msg.requestId, "answer")
                            runOnUiThread {
                                if(liquidWebRTCService!!.isDeepLink) this@AnswerActivity.onBackPressed()
                            }
                            liquidWebRTCService?.handleMessages(this@AnswerActivity, { peerMsg ->
                                handleMessages(peerMsg)
                            }) {
                                Log.d(TAG, "onStateChange($it)")
                                if (it === "OPEN") {
                                    Log.d(TAG, "Sending Credential")
                                    liquidWebRTCService!!.send(
                                        viewModel.getCredentialMessage(
                                            wallet.account.value!!,
                                            credential
                                        ).toString()
                                    )
                                }
                            }

                            Toast.makeText(this@AnswerActivity, "Registered Credentials!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@AnswerActivity, "Couldn't find service", Toast.LENGTH_LONG).show()
                        }
                    }

                }
            }
        }
    }

    /**
     * Authentication using a PublicKeyCredential (Step 1 of 2)
     *
     * Receives PublicKeyCredentialRequestOptions from the FIDO2 Server and launches
     * the authenticator Intent using the handleAuthenticatorAssertionResult Handler
     */
    private suspend fun authenticate(msg: AuthMessage, credential: Credential) {
        val response = assertionApi.postAssertionOptions(
            msg.origin,
            userAgent,
            credential.credentialId
        ).await()
        val session = Cookie.fromResponse(response)
        session?.let {
            setSession(Cookie.getID(it))
        }
        val publicKeyCredentialRequestOptions = response.body!!.toPublicKeyCredentialRequestOptions()
        val pendingIntent = fido2Client!!.getSignPendingIntent(publicKeyCredentialRequestOptions).await()
        assertionIntentLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
    }

    /**
     * Authentication using a PublicKeyCredential (Step 2 of 2)
     *
     * Handles the ActivityResult from a FIDO2 Intent and submits
     * the Authenticator's PublicKeyCredential to the FIDO2 Server
     */
    private fun handleAuthenticatorAssertionResult(activityResult: ActivityResult) {
        val bytes = activityResult.data?.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)
        when {
            activityResult.resultCode != Activity.RESULT_OK ->
                Toast.makeText(this@AnswerActivity, "Canceled", Toast.LENGTH_LONG).show()

            bytes == null ->
                Toast.makeText(this@AnswerActivity, "Authenticate Error", Toast.LENGTH_LONG).show()

            else -> {
                // Handle PublicKeyCredential Response from Authenticator
                val credential = PublicKeyCredential.deserializeFromBytes(bytes)
                val pubKeyCredentialResponse = credential.response
                if (pubKeyCredentialResponse is AuthenticatorErrorResponse) {
                    Toast.makeText(this@AnswerActivity, pubKeyCredentialResponse.errorMessage, Toast.LENGTH_LONG)
                        .show()
                } else {
                    lifecycleScope.launch {
                        val liquidExtJSON = JSONObject()
                        liquidExtJSON.put("requestId", viewModel.message.value!!.requestId)
                        // POST Authenticator Results to FIDO2 API
                        val response = assertionApi.postAssertionResult(
                            viewModel.message.value!!.origin,
                            userAgent,
                            credential,
                            liquidExtJSON
                        ).await()

                        // Update Render/State
                        val data = response.body!!.string()
                        val json = JSONObject(data)
                        val creds = json.get("credentials") as JSONArray

                        if (creds.length() > 0) {
                            for (i in 0 until creds.length()) {
                                val cred: JSONObject = creds.getJSONObject(i)
                                if (cred.get("credId") == credential.id) {
                                    viewModel.setCount(cred.get("prevCounter") as Int)
                                }
                            }
                        } else {
                            viewModel.setCount(0)
                        }
                        val msg = viewModel.message.value!!
                        if (mBounded) {
                            liquidWebRTCService?.peer(msg.requestId, "answer")
                            runOnUiThread {
                                if(liquidWebRTCService!!.isDeepLink) this@AnswerActivity.onBackPressed()
                            }
                            liquidWebRTCService?.handleMessages(this@AnswerActivity, { peerMsg ->
                                handleMessages(peerMsg)
                            }) {
                                Log.d(TAG, "onStateChange($it)")
                                if (it === "OPEN") {
                                    Log.d(TAG, "Sending Credential")
                                    liquidWebRTCService?.send(
                                        viewModel.getCredentialMessage(
                                            wallet.account.value!!,
                                            credential
                                        ).toString()
                                    )
                                }
                            }
                        } else {
                            Toast.makeText(this@AnswerActivity, "Couldn't find service", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    /**
     * Update Render for demonstration purposes only
     */
    private fun setSession(s: String?) {
        if (s === null) {
            viewModel.setSession("Logged Out")
            viewModel.setMessage(null)
        } else {
            viewModel.setSession(s)
        }
    }
}
