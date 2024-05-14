package foundation.algorand.demo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.webauthn.AuthenticatorAssertionResponse
import androidx.credentials.webauthn.FidoPublicKeyCredential
import androidx.credentials.webauthn.PublicKeyCredentialRequestOptions
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import foundation.algorand.demo.credential.CredentialRepository
import org.json.JSONObject
import java.security.*
import java.security.interfaces.ECPrivateKey
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class GetPasskeyViewModel: ViewModel() {
    private val credentialRepository = CredentialRepository()
    companion object {
        const val TAG = "GetPasskeyViewModel"
    }

    private val _origin = MutableLiveData<String>().apply {
        value = ""
    }
    val origin: LiveData<String> = _origin

    fun setOrigin(o: String){
        _origin.value = o
    }
    private val _requestJson = MutableLiveData<String>().apply {
        value = ""
    }
    val requestJson: LiveData<String> = _requestJson

    fun setRequestJson(jsonStr: String){
        _requestJson.value = JSONObject(jsonStr).toString(2)
    }

    private val _callingApp = MutableLiveData<String>().apply {
        value = ""
    }

    val callingApp: LiveData<String> = _callingApp
    fun setCallingAppPackage(name: String){
        _callingApp.value = name
    }
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun processGetPasskey(context: Context, request: ProviderGetCredentialRequest, requestInfo: Bundle?): Intent{
        Log.d(TAG, "processGetPasskey($request)")

        // Set Origin
        request.callingAppInfo.origin?.let { setOrigin(it) }

        // Set Calling App Package
        val packageName = request.callingAppInfo.packageName
        setCallingAppPackage(packageName)

        // Set Request JSON, TODO: Handle multiple options
        val option = request.credentialOptions[0] as GetPublicKeyCredentialOption
        setRequestJson(option.requestJson)
        return handleGetPasskey(context, request, requestInfo)
    }
    @SuppressLint("RestrictedApi")
    @OptIn(ExperimentalEncodingApi::class)
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun handleGetPasskey(context: Context, request: ProviderGetCredentialRequest, requestInfo: Bundle?): Intent{
        Log.d(TAG, "handleGetPasskey($request, $requestInfo)")
        val option = request.credentialOptions[0] as GetPublicKeyCredentialOption
        val requestOptions = PublicKeyCredentialRequestOptions(option.requestJson)
        val credIdEnc = requestInfo!!.getString("credentialId")
        val credId = Base64.decode(credIdEnc!!)
        val clientDataHash = option.requestData.getByteArray("androidx.credentials.BUNDLE_KEY_CLIENT_DATA_HASH")
        val packageName = request.callingAppInfo.packageName
        val userHandle = requestInfo.getString("userHandle")
        val origin = credentialRepository.appInfoToOrigin(request.callingAppInfo)
        Log.d(TAG, origin)
        val response = AuthenticatorAssertionResponse(
            requestOptions = requestOptions,
            credentialId = credId,
            origin = credentialRepository.appInfoToOrigin(request.callingAppInfo),
            up = true,
            uv = true,
            be = true,
            bs = true,
            userHandle = userHandle!!.toByteArray(),
            packageName = packageName,
             clientDataHash = clientDataHash!!
        )
        val keyPair = credentialRepository.getKeyPair(context, credId)

        //TODO: Fix signature issues
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(keyPair.private as ECPrivateKey )
        sig.update(response.dataToSign())
        response.signature = sig.sign()
        val options = request.credentialOptions[0] as GetPublicKeyCredentialOption
        val json = options.requestJson
        val requestJson = JSONObject(json)


        val credential = FidoPublicKeyCredential(
            rawId = credId, response = response, authenticatorAttachment = "platform"
        )
//         add clientDataJSON to the response
        val clientDataJSONb64 = getClientDataJSONb64(origin, requestJson.get("challenge") as String)
        val delimiter = "response\":{"
        val credentialJson = credential.json().substringBeforeLast(delimiter)+ delimiter +
                "\"clientDataJSON\":\"$clientDataJSONb64\","+
                credential.json().substringAfterLast(delimiter)
        Log.d(TAG, credential.toString())
        val result = Intent()
        val passkeyCredential = PublicKeyCredential(credentialJson)
        PendingIntentHandler.setGetCredentialResponse(
            result, GetCredentialResponse(passkeyCredential)
        )
        return result
    }
    @OptIn(ExperimentalEncodingApi::class)
    private fun getClientDataJSONb64(origin: String, challenge:String): String {

        val origin = origin.replace(Regex("/$"), "")

        val jsonString =
            "{\"type\":\"webauthn.get\",\"challenge\":\"$challenge\",\"origin\":\"$origin\",\"crossOrigin\":false}"
        val jsonByteArray = jsonString.toByteArray()
        Log.d("MainActivity","+++ ClientDataJSON: $jsonString")
        return Base64.encode(jsonByteArray)
    }

}
