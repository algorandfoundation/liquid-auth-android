package co.algorand.liquid.wallet

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import co.algorand.liquid.wallet.encoding.b64Decode
import co.algorand.liquid.wallet.encoding.b64Encode
import foundation.algorand.auth.connect.AuthMessage
import foundation.algorand.auth.connect.SignalService
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await

class MainViewModel(): ViewModel() {
    private val xHDKeyManager = AppDependencies.xHDKeyManager
    private val keysRepository = AppDependencies.keysRepository
    private val attestationApi = AppDependencies.attestationApi
    private val assertionApi = AppDependencies.assertionApi
    private val credentialManager = AppDependencies.credentialManager
    // FIDO User Agent for validating the device
    private val userAgent =
        "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} " +
                "(Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; ${Build.BRAND})"

    fun onScan(context: Context, uri: Uri){
        Log.d(TAG, "Connecting to $uri")
        val address = xHDKeyManager.getAddress()
        if(address === null){
            throw Exception("No account assigned")
        }
        // TODO: look for existing credentials
        val hasCredential = false
        runBlocking {
           authenticate(context, AuthMessage.fromUri(uri))
        }

    }
    suspend fun authenticate(context: Context, msg: AuthMessage){
        Log.d(TAG, "Connecting to ${msg.origin}")

        val address = xHDKeyManager.getAddress()
        if(address === null){
            throw Exception("No account assigned")
        }
        // Create the Liquid Extension
        val options = JSONObject()
        options.put("username", address.toString())
        options.put("displayName", "Liquid Auth User")
        options.put("authenticatorSelection", JSONObject().put("userVerification", "required"))
        val extensions = JSONObject()
        extensions.put("liquid", true)
        options.put("extensions", extensions)

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
                val additionalSignature = xHDKeyManager.rawSign(b64Decode(challenge))
                val authenticatorJson = result.registrationResponseJson
                Log.d(TAG, "Received Attestation Authenticator Response: ${authenticatorJson}")
                val liquidExtJSON = JSONObject()
                liquidExtJSON.put("type", "algorand")
                liquidExtJSON.put("requestId", msg.requestId)
                liquidExtJSON.put("address", address.toString())
                liquidExtJSON.put("signature", b64Encode(additionalSignature!!))
                liquidExtJSON.put("device", Build.MODEL)
                val submit = attestationApi.postAttestationResult(msg.origin, userAgent, result.registrationResponseJson, liquidExtJSON).await()
                Log.d(TAG, "Received Attestation Service Response: ${submit.body!!.string()}")
            }

        } catch (e: NoCredentialException) {
            Log.e("CredentialManager", "No credential available", e)
        }
    }
    fun register(msg: AuthMessage, signalService: SignalService){
        Log.d(TAG, "Connecting to ${msg.origin}")
    }

    fun signal(origin: String) {
        Log.d(TAG, "Connecting to $origin")
    }

    companion object {
        const val TAG = "MainViewModel"
    }
}