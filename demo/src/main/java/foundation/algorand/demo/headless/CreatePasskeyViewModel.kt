package foundation.algorand.demo.headless

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.webauthn.AuthenticatorAttestationResponse
import androidx.credentials.webauthn.FidoPublicKeyCredential
import androidx.credentials.webauthn.PublicKeyCredentialCreationOptions
import androidx.lifecycle.*
import foundation.algorand.demo.credential.CredentialRepository
import foundation.algorand.demo.credential.db.Credential
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigInteger
import java.security.KeyPair
import java.security.interfaces.ECPublicKey
import java.security.spec.ECPoint
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


class CreatePasskeyViewModel(): ViewModel() {
    private val credentialRepository = CredentialRepository()
    companion object {
        const val TAG = "CreatePasskeyViewModel"
    }

    private val _origin = MutableLiveData<String>().apply {
        value = ""
    }
    val origin: LiveData<String> = _origin

    private fun setOrigin(o: String){
        _origin.value = o
    }
    private val _requestJson = MutableLiveData<String>().apply {
        value = ""
    }
    val requestJson: LiveData<String> = _requestJson

    private fun setRequestJson(jsonStr: String){
        _requestJson.value = JSONObject(jsonStr).toString(2)
    }

    private val _callingApp = MutableLiveData<String>().apply {
        value = ""
    }

    val callingApp: LiveData<String> = _callingApp
    private fun setCallingAppPackage(name: String){
        _callingApp.value = name
    }

    /**
     * Handle state and dispatch handlers
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun processCreatePasskey(context: Context, request: ProviderCreateCredentialRequest): Intent{
        val publicKeyRequest: CreatePublicKeyCredentialRequest =
            request.callingRequest as CreatePublicKeyCredentialRequest

        setOrigin(request.callingAppInfo.origin!!)
        setCallingAppPackage(request.callingAppInfo.packageName)
        setRequestJson(publicKeyRequest.requestJson)

        return handleCreatePasskey(context, request)
    }

    /**
     * Thank you https://developers.kddi.com/blog/2esxXGTcSBSaGLTJO0dC67
     */
    private fun getPublicKeyFromKeyPair(keyPair: KeyPair?): ByteArray {
        // credentialPublicKey CBOR
        if (keyPair==null) return ByteArray(0)
        if (keyPair.public !is ECPublicKey) return ByteArray(0)

        val ecPubKey = keyPair.public as ECPublicKey
        val ecPoint: ECPoint = ecPubKey.w

        // for now, only covers ES256
        if (ecPoint.affineX.bitLength() > 256 || ecPoint.affineY.bitLength() > 256) return ByteArray(0)

        val byteX = bigIntToByteArray32(ecPoint.affineX)
        val byteY = bigIntToByteArray32(ecPoint.affineY)

        // refer to RFC9052 Section 7 for details
        return "A5010203262001215820".chunked(2).map { it.toInt(16).toByte() }.toByteArray() + byteX+ "225820".chunked(2).map { it.toInt(16).toByte() }.toByteArray() + byteY

    }
    private fun bigIntToByteArray32(bigInteger: BigInteger):ByteArray{
        var ba = bigInteger.toByteArray()

        if(ba.size < 32) {
            // append zeros in front
            ba = ByteArray(32) + ba
        }
        // get the last 32 bytes as bigint conversion sometimes put extra zeros at front
        return ba.copyOfRange(ba.size - 32, ba.size)

    }
    @SuppressLint("RestrictedApi")
    @OptIn(ExperimentalEncodingApi::class)
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun handleCreatePasskey(context: Context, request: ProviderCreateCredentialRequest): Intent {
        Log.d(TAG, "handleCreatePasskey($context, $request)")
        val publicKeyRequest: CreatePublicKeyCredentialRequest =
            request.callingRequest as CreatePublicKeyCredentialRequest
        val requestOptions = PublicKeyCredentialCreationOptions(publicKeyRequest.requestJson)

        // Generate a credentialId
        val credentialId = credentialRepository.generateCredentialId()
        // Generate a credential key pair
        val keyPair = credentialRepository.getKeyPair(context, credentialId)

        val requestJson = JSONObject(publicKeyRequest.requestJson)
        val userJson = requestJson.getJSONObject("user")
        val name = userJson.get("name").toString()
        val userId = userJson.get("id").toString()

        // Save passkey in your database as per your own implementation
        viewModelScope.launch {
            credentialRepository.saveCredential(
                context,
                Credential(
                    credentialId = Base64.encode(credentialId),
                    userHandle = name,
                    userId= userId,
                    origin = request.callingAppInfo.origin!!,
                    publicKey = Base64.encode(keyPair.public.encoded),
                    privateKey = Base64.encode(keyPair.private.encoded),
                    count = 0,
                )
            )
        }

        // Create AuthenticatorAttestationResponse object to pass to
        // FidoPublicKeyCredential

        val response = AuthenticatorAttestationResponse(
            requestOptions = requestOptions,
            credentialId = credentialId,
            credentialPublicKey = getPublicKeyFromKeyPair(keyPair),
            origin = credentialRepository.appInfoToOrigin(request.callingAppInfo),
            up = true,
            uv = true,
            be = true,
            bs = true,
            packageName = request.callingAppInfo.packageName
        )

        val credential = FidoPublicKeyCredential(
            rawId = credentialId, response = response, authenticatorAttachment = "cross-platform"
        )

        val result = Intent()

        val createPublicKeyCredResponse =
            CreatePublicKeyCredentialResponse(credential.json())

               // Set the CreateCredentialResponse as the result of the Activity
        PendingIntentHandler.setCreateCredentialResponse(
            result, createPublicKeyCredResponse
        )
        return result
    }
}
