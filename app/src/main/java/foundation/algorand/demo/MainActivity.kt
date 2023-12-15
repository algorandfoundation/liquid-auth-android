package foundation.algorand.demo

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.Fido2ApiClient
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import foundation.algorand.auth.fido2.*
import foundation.algorand.auth.verify.ConnectApi
import foundation.algorand.auth.verify.Message
import foundation.algorand.auth.verify.crypto.KeyPairs
import foundation.algorand.demo.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONArray
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await
import java.security.Security

class MainActivity : AppCompatActivity() {
    val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    // Third Party APIs
    private var httpClient = OkHttpClient()
    private lateinit var scanner: GmsBarcodeScanner
    private var fido2Client: Fido2ApiClient? = null

    // FIDO/Auth interfaces
    val connectApi = ConnectApi(httpClient)
    val attestationApi = AttestationApi(httpClient)
    val assertionApi = AssertionApi(httpClient)

    val userAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} " +
            "(Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; ${Build.BRAND})"

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
        fido2Client = Fido2ApiClient(this@MainActivity)
        scanner = GmsBarcodeScanning.getClient(this@MainActivity)

        // View Bindings
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        setContentView(binding.root)
        binding.viewModel = viewModel

        binding.connectButton.setOnClickListener {
            connect()
        }
        binding.registerButton.setOnClickListener {
            lifecycleScope.launch {
                register(viewModel.message.value!!, viewModel.session.value!!)
            }
        }
        binding.authenticateButton.setOnClickListener {
            lifecycleScope.launch {
                authenticate()
            }
        }
        binding.disconnectButton.setOnClickListener {
            setSession(null)
        }

    }
    /**
     * Connect/Proof of Knowledge API
     *
     * Connects the Wallet/Android Application to a dApp/website using a Barcode.
     * The barcode includes a Message which includes the origin, requestId and challenge to be signed.
     * The wallet must sign the challenge and attach both the wallet address and
     */
    private fun connect() {
        val account = viewModel.account.value!!
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                Log.d("Scan Barcode", "Doing stuff")
                // Decode Barcode Message
                val msg = Message.fromBarcode(barcode)
                // Add wallet to Message
                msg.wallet = account.address.toString()

                // Optionally sign the message directly with a PrivateKey
                // val signer = Signature.getInstance("EdDSA")
                // signer.initSign(key)
                // signer.update(bytes)
                // msg.signature = signer.sign()


                // Connect to Service
                lifecycleScope.launch {
                    // Connect to Service and pass in optional KeyPair for signing,
                    // the message must be signed if no KeyPair is provided
                    val response = connectApi.submit(msg, KeyPairs.getKeyPair(account.toMnemonic())).await()
                    // Save the current session for future API requests
                    val session = Cookie.fromResponse(response)

                    // Update Render
                    viewModel.setMessage(msg)
                    setSession(session!!)
                }
            }
            .addOnCanceledListener {
                Toast.makeText(this@MainActivity, "Canceled", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Registration of a new Credential (Step 1 of 2)
     *
     * Receives PublicKeyCredentialCreationOptions from the FIDO2 Server and launches
     * the authenticator Intent using the handleAuthenticatorAttestationResult Handler
     */
    private suspend fun register(msg: Message, sessionCookie: String, options: JSONObject = JSONObject()) {
        // FIDO2 Server API Response for PublicKeyCredentialCreationOptions
        val response = attestationApi.postAttestationOptions(msg.origin, userAgent, sessionCookie, options).await()
        // Convert ResponseBody to FIDO2 PublicKeyCredentialCreationOptions
        val pubKeyCredentialCreationOptions = response.body!!.toPublicKeyCredentialCreationOptions()
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
                Toast.makeText(this@MainActivity, "Canceled", Toast.LENGTH_LONG).show()

            bytes == null ->
                Toast.makeText(this@MainActivity, "Error", Toast.LENGTH_LONG)
                    .show()

            else -> {
                // Handle PublicKeyCredential Response from Authenticator
                val credential = PublicKeyCredential.deserializeFromBytes(bytes)
                val response = credential.response
                if (response is AuthenticatorErrorResponse) {
                    Toast.makeText(this@MainActivity, response.errorMessage, Toast.LENGTH_LONG)
                        .show()
                } else {
                    //
                    lifecycleScope.launch {
                        // POST Authenticator Results to FIDO2 API
                        attestationApi.postAttestationResult(
                            viewModel.message.value!!.origin,
                            userAgent,
                            viewModel.session.value!!,
                            credential
                        ).await()

                        // Update Render/State
                        viewModel.setCredential(credential)
                        Toast.makeText(this@MainActivity, "Registered Credentials!", Toast.LENGTH_LONG).show()
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
    private suspend fun authenticate() {
        val response = assertionApi.postAssertionOptions(
            viewModel.message.value!!.origin,
            userAgent,
            viewModel.session.value,
            viewModel.credential.value!!.id
        ).await()
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
                Toast.makeText(this@MainActivity, "Canceled", Toast.LENGTH_LONG).show()

            bytes == null ->
                Toast.makeText(this@MainActivity, "Authenticate Error", Toast.LENGTH_LONG).show()

            else -> {
                // Handle PublicKeyCredential Response from Authenticator
                val credential = PublicKeyCredential.deserializeFromBytes(bytes)
                val pubKeyCredentialResponse = credential.response
                if (pubKeyCredentialResponse is AuthenticatorErrorResponse) {
                    Toast.makeText(this@MainActivity, pubKeyCredentialResponse.errorMessage, Toast.LENGTH_LONG)
                        .show()
                } else {
                    lifecycleScope.launch {
                        // POST Authenticator Results to FIDO2 API
                        val response = assertionApi.postAssertionResult(
                            viewModel.message.value!!.origin,
                            userAgent,
                            viewModel.session.value!!,
                            credential
                        ).await()

                        // Update Render/State
                        val data = response.body!!.string()
                        val json = JSONObject(data)
                        val creds = json.get("credentials") as JSONArray
                        if(creds.length() > 0) {
                            for (i in 0 until creds.length()) {
                                val cred: JSONObject = creds.getJSONObject(i)
                                if(cred.get("credId") == credential.id ){
                                    viewModel.setCount(cred.get("prevCounter") as Int)
                                }
                            }
                        } else {
                            viewModel.setCount(0)
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
            binding.registerButton.visibility = View.INVISIBLE
            binding.authenticateButton.visibility = View.INVISIBLE
            binding.disconnectButton.visibility = View.INVISIBLE
            binding.connectButton.visibility = View.VISIBLE
        } else {
            viewModel.setSession(s)
            binding.registerButton.visibility = View.VISIBLE
            binding.authenticateButton.visibility = View.VISIBLE
            binding.disconnectButton.visibility = View.VISIBLE
            binding.connectButton.visibility = View.INVISIBLE
        }
    }
}
