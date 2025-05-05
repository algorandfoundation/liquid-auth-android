package co.algorand.liquid.wallet

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import co.algorand.liquid.wallet.MainActivity
import co.algorand.liquid.wallet.encoding.b64Decode
import co.algorand.liquid.wallet.encoding.b64Encode
import foundation.algorand.auth.connect.AuthMessage
import foundation.algorand.crypto.EncoderType
import foundation.algorand.provider.Message
import foundation.algorand.provider.avm.models.RequestMessage
import foundation.algorand.provider.avm.models.ResponseMessage
import foundation.algorand.provider.avm.models.SignTransactionsParams
import foundation.algorand.provider.avm.models.SignTransactionsResult
import kotlinx.coroutines.launch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await
import java.security.Security
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.io.encoding.Base64

class MainViewModel(notificationViewModel: NotificationViewModel): ViewModel() {
    private val notifications = notificationViewModel
    private val xHDKeyManager = AppDependencies.xHDKeyManager
    private val keysRepository = AppDependencies.keysRepository
    private val attestationApi = AppDependencies.attestationApi
    private val assertionApi = AppDependencies.assertionApi
    private val credentialManager = AppDependencies.credentialManager
    private val provider = AppDependencies.provider

    // Biometrics Prompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // FIDO User Agent for validating the device
    private val userAgent =
        "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} " +
                "(Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; ${Build.BRAND})"

    init {
        // Override security for BC
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 0)
    }

    /**
     * Handles the scanning process for a given URI. This method attempts to connect to the provided URI
     * and processes authentication or registration based on the state of the credentials.
     *
     * @param context The context from which the scan is being initiated.
     * @param uri The URI to be scanned and processed.
     * @throws Exception If no account is assigned or an error occurs during the scan handling process.
     */
    suspend fun onScan(context: Context, uri: Uri){
        Log.d(TAG, "Connecting to $uri")
        val address = xHDKeyManager.getAddress()
        if(address === null){
            throw Exception("No account assigned")
        }

        val msg = AuthMessage.fromUri(uri)

        val passkeysForSite = keysRepository.credentialsForSite(msg.origin.replace("https://", ""))

        if(passkeysForSite !== null && passkeysForSite.passkeys.isNotEmpty()){
            authenticate(context, msg, passkeysForSite.passkeys[0].credentialId)
        } else {
         register(context, msg)
        }


    }

    /**
     * Constructs a Liquid Extension JSON object representing an extension for authentication or messaging purposes.
     * Includes details such as type, address, and a signature, along with optional metadata.
     *
     * @param msg The authentication message containing request details.
     * @param address The address to be included in the generated JSON object.
     * @param signature The byte array representing the signature to be Base64-encoded and included in the JSON object.
     * @return A JSONObject containing the constructed extension with the provided details and metadata.
     */
    fun getExtension(msg: AuthMessage, address: String, signature: ByteArray): JSONObject{
        // Add Liquid Extension
        val liquidExtJSON = JSONObject()
        liquidExtJSON.put("type", "algorand")
        liquidExtJSON.put("address", address.toString())
        liquidExtJSON.put("signature", b64Encode(signature))
        // Optional Arguments
        liquidExtJSON.put("requestId", msg.requestId)
        liquidExtJSON.put("device", Build.MODEL)
        return liquidExtJSON
    }

    /**
     * Generates a JSON object containing options for Liquid authentication. The method sets
     * up the username, display name, authenticator selection preferences, and extension details.
     *
     * @param address The address to be associated with the generated options.
     * @return A JSONObject containing the constructed options for Liquid authentication.
     */
    fun getOptions(address: String): JSONObject{
        // Create the Liquid Extension
        val options = JSONObject()
        options.put("username", address)
        options.put("displayName", "Liquid Auth User")
        options.put("authenticatorSelection", JSONObject().put("userVerification", "required"))
        val extensions = JSONObject()
        extensions.put("liquid", true)
        options.put("extensions", extensions)
        return options
    }

    /** Transaction Biometric Prompt */
    suspend fun biometrics(
        activity: FragmentActivity,
        message: SignTransactionsParams
    ): BiometricPrompt.AuthenticationResult? {
        return suspendCoroutine { continuation ->
            var biometricPrompt =
                BiometricPrompt(
                    activity,
                    ContextCompat.getMainExecutor(activity),
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
                    }
                )
            val promptInfo =
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Transaction(s) ${message.txns.size}")
                    .setSubtitle("Provider: ${message.providerId}")
                    .setNegativeButtonText("Cancel")
                    .build()
            biometricPrompt.authenticate(promptInfo)
        }
    }
    suspend fun handleMessage(activity: FragmentActivity, msgStr: String, onError: (String)-> Unit){
        val signalService = AppDependencies.signalService

        try {
            val message = Message(b64Decode(msgStr), EncoderType.CBOR)
            val request = provider.encoder.decode<RequestMessage>(message.data, message.encoding)
            if (request.reference == "arc0027:sign_transactions:request") {

                    val params =
                        provider.encoder.decode<SignTransactionsParams>(
                            provider.encoder.encode(request.params, EncoderType.NONE),
                            EncoderType.NONE
                        )
                    biometrics(activity, params)
                    val resultMessage = provider.handleMessage(message) as ResponseMessage
                    when (resultMessage.result) {
                        is SignTransactionsResult -> {
                            signalService.send(
                                b64Encode(
                                    resultMessage.toByteArray(EncoderType.CBOR)
                                )
                            )
                        }
                        else -> {
                            TODO("Not Implemented")
                        }
                    }
                }
        } catch (e: Exception) {
            val errMsg = e.message ?: "Couldn't decode message"
            Log.e(TAG, errMsg)
            onError(errMsg)
        }
    }

    suspend fun register(context: Context, msg: AuthMessage){
        Log.d(TAG, "Connecting to ${msg.origin}")

        // Start the signal service
        val signalService = AppDependencies.signalService
        signalService.start(
            msg.origin,
            AppDependencies.httpClient,
            notifications.createNotificationBuilder(context),
            NotificationViewModel.SERVICE_NOTIFICATION_ID,
            MainActivity::class.java,
        )

        // Account to use for registration
        val address = xHDKeyManager.getAddress()

        // Request that the service use the Liquid Extension
        val options = getOptions(address!!)
        val response = attestationApi.postAttestationOptions(msg.origin, userAgent, options).await()
        val requestJson = response.body!!.string()

        Log.d(TAG, "Received Attestation Options: $requestJson")
        val challenge = JSONObject(requestJson).getString("challenge")


        // Create the Request from the Authenticator
        val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(
            requestJson = requestJson
        )

        try {
            val result = credentialManager.createCredential(
                context = context,
                request = createPublicKeyCredentialRequest
            )
            if(result is CreatePublicKeyCredentialResponse){
                val challengeBytes = b64Decode(challenge)
                if(hasAlgorandTags(challengeBytes)){
                    throw Exception("Attempted to sign a message with an Algorand prefix")
                } else {
                    Log.d(TAG, "Signing $challenge with $address")
                }
                // Note: This signature may change before the 1.0.0 release
                 val additionalSignature = xHDKeyManager.rawSign(challengeBytes)

                // Handle Authenticator Response
                val authenticatorJson = result.registrationResponseJson
                Log.d(TAG, "Received Attestation Authenticator Response: $authenticatorJson")

                // Add Liquid Extension
                val liquidExtJSON = getExtension(msg, address, additionalSignature!!)

                // Submit result to the Liquid Service
                val submit = attestationApi.postAttestationResult(msg.origin, userAgent, result.registrationResponseJson, liquidExtJSON).await()
                val submitBodyString = submit.body!!.string()
                Log.d(TAG, "Received Attestation Service Response: $submitBodyString")

                if(submit.code != 201){
                    throw Exception(submitBodyString)
                }

                // Optionally, connect to a peer
                if(AppDependencies.mBounded){
                    val iceServers = AppDependencies.iceServers
                    signalService.peer(msg.requestId, "answer", iceServers)
                } else {
                    throw Exception("Invalid peer client")
                }

            }

        } catch (e: NoCredentialException) {
            Log.e("CredentialManager", "No credential available", e)
        }
    }

    suspend fun authenticate(context: Context, msg: AuthMessage, credId: String){
        Log.d(TAG, "Connecting to ${msg.origin}")

        val signalService = AppDependencies.signalService
        signalService.start(
            msg.origin,
            AppDependencies.httpClient,
            notifications.createNotificationBuilder(context),
            NotificationViewModel.SERVICE_NOTIFICATION_ID,
            MainActivity::class.java,
        )

        // Account to use for registration
        val address = xHDKeyManager.getAddress()
        if(address === null){
            throw Exception("Address not found")
        }

        val response = assertionApi.postAssertionOptions(msg.origin, userAgent, credId).await()
        if(response.code != 201){
            throw Exception("Couldn't fetch the options")
        }
        val requestJson = response.body!!.string()

        val challenge = JSONObject(requestJson).getString("challenge")

        val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
            requestJson = requestJson
        )
        val getCredRequest = GetCredentialRequest(
            listOf(getPublicKeyCredentialOption)
        )
        try {
            val result = credentialManager.getCredential(
                // Use an activity-based context to avoid undefined system UI
                // launching behavior.
                context = context,
                request = getCredRequest
            )

            val challengeBytes = b64Decode(challenge)
            if(hasAlgorandTags(challengeBytes)){
                throw Exception("Attempted to sign a message with an Algorand prefix")
            }
            // Note: This signature may change before the 1.0.0 release
            val signature = xHDKeyManager.rawSign(challengeBytes)!!
            val extension = getExtension(msg, address, signature)
            val credential = result.credential
            if(credential is PublicKeyCredential){
                val submit = assertionApi.postAssertionResult(msg.origin, userAgent, credential.authenticationResponseJson, extension).await()
                Log.d(TAG, submit.body!!.string())

                // Optionally, connect to a peer
                if(AppDependencies.mBounded){
                    val iceServers = AppDependencies.iceServers
                    signalService.peer(msg.requestId, "answer", iceServers)
                } else {
                    throw Exception("Invalid peer client")
                }

            } else {
                throw Exception("Unsupported Credential")
            }


        } catch (e: GetCredentialException){
            Log.e(TAG, e.message ?: "Something went wrong")
        }

    }

    // TODO: Remove in favor of HD validation
    fun hasAlgorandTags(message: ByteArray): Boolean {
        val prefixes =
            listOf(
                "appID",
                "arc",
                "aB",
                "aD",
                "aO",
                "aP",
                "aS",
                "AS",
                "BH",
                "B256",
                "BR",
                "CR",
                "GE",
                "KP",
                "MA",
                "MB",
                "MX",
                "NIC",
                "NIR",
                "NIV",
                "NPR",
                "OT1",
                "OT2",
                "PF",
                "PL",
                "Program",
                "ProgData",
                "PS",
                "PK",
                "SD",
                "SpecialAddr",
                "STIB",
                "spc",
                "spm",
                "spp",
                "sps",
                "spv",
                "TE",
                "TG",
                "TL",
                "TX",
                "VO"
            )
        // Prefixes taken from go-algorand node software code
        // https://github.com/algorand/go-algorand/blob/master/protocol/hash.go

        val messageString = String(message)
        return prefixes.any { messageString.startsWith(it) }
    }

    companion object {
        const val TAG = "MainViewModel"
    }
}
