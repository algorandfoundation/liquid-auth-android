package co.algorand.liquid.wallet

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import co.algorand.liquid.wallet.encoding.b64Decode
import co.algorand.liquid.wallet.encoding.b64Encode
import com.algorand.algosdk.account.Account
import foundation.algorand.auth.connect.AuthMessage
import foundation.algorand.auth.connect.SignalService
import foundation.algorand.crypto.avm.KeyPairs
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await
import java.security.Security

class MainViewModel(notificationViewModel: NotificationViewModel): ViewModel() {
    private var testAccount: Account? = null
    private val notifications = notificationViewModel
    private val xHDKeyManager = AppDependencies.xHDKeyManager
    private val keysRepository = AppDependencies.keysRepository
    private val attestationApi = AppDependencies.attestationApi
    private val assertionApi = AppDependencies.assertionApi
    private val credentialManager = AppDependencies.credentialManager
    // FIDO User Agent for validating the device
    private val userAgent =
        "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} " +
                "(Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; ${Build.BRAND})"

    init {
        // Override security for BC
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 0)
        testAccount = xHDKeyManager.getTmpAccount()
    }

    suspend fun onScan(context: Context, uri: Uri){
        Log.d(TAG, "Connecting to $uri")
        val address = xHDKeyManager.getAddress()
        if(address === null){
            throw Exception("No account assigned")
        }

        val msg = AuthMessage.fromUri(uri)

        //val passkeysForSite = keysRepository.credentialsForSite(msg.origin)

        //if(passkeysForSite!!.passkeys.isNotEmpty()){
        // authenticate(context, msg, passkeysForSite.passkeys[0].credentialId)
        //} else {
        register(context, msg)
        //}
    }
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
    suspend fun register(context: Context, msg: AuthMessage){
        Log.d(TAG, "Connecting to ${msg.origin}")

        val signalService = AppDependencies.signalService
        signalService.start(
            msg.origin,
            AppDependencies.httpClient,
            notifications.createNotificationBuilder(context),
            NotificationViewModel.SERVICE_NOTIFICATION_ID,
            MainActivity::class.java,
        )

        // TODO: signature validation for account
        //   val address = xHDKeyManager.getAddress()
        val address = testAccount!!.address.toString()
        // Create the Liquid Extension
        val options = getOptions(address)

        val response = attestationApi.postAttestationOptions(msg.origin, userAgent, options).await()
        val requestJson = response.body!!.string()

        val challenge = JSONObject(requestJson).getString("challenge")

        Log.d(TAG, "Received Attestation Options: ${requestJson}")

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
                }
                // Note: This signature may change before the 1.0.0 release
                // val additionalSignature = xHDKeyManager.rawSign(challengeBytes)

                // TODO: Signature issue with HD key
                val keyPair = KeyPairs.getKeyPair(testAccount!!.toMnemonic())
                val additionalSignature = KeyPairs.rawSignBytes(challengeBytes, keyPair.private)

                // Handle Authenticator Response
                val authenticatorJson = result.registrationResponseJson
                Log.d(TAG, "Received Attestation Authenticator Response: $authenticatorJson")

                // Add Liquid Extension
                val liquidExtJSON = JSONObject()
                liquidExtJSON.put("type", "algorand")
                liquidExtJSON.put("address", address.toString())
                liquidExtJSON.put("signature", b64Encode(additionalSignature!!))
                // Optional Arguments
                liquidExtJSON.put("requestId", msg.requestId)
                liquidExtJSON.put("device", Build.MODEL)

                // Submit result to the Liquid Service
                val submit = attestationApi.postAttestationResult(msg.origin, userAgent, result.registrationResponseJson, liquidExtJSON).await()

                Log.d(TAG, "Received Attestation Service Response: ${submit.body!!.string()}")

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

        // TODO: signature validation for account
        //   val address = xHDKeyManager.getAddress()
        val address = testAccount!!.address.toString()

        val response = assertionApi.postAssertionOptions(msg.origin, userAgent, credId).await()
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
            val keyPair = KeyPairs.getKeyPair(testAccount!!.toMnemonic())
            val signature = KeyPairs.rawSignBytes(challengeBytes, keyPair.private)
            val extension = getExtension(msg, address, signature!!)
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