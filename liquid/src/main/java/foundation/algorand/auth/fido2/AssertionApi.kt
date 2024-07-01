package foundation.algorand.auth.fido2

import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import foundation.algorand.auth.crypto.toBase64
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject

class AssertionApi @Inject constructor(
    val client: OkHttpClient
) {
    fun postAssertionOptionsRequest(
        origin: String,
        userAgent: String,
        credentialId: String,
        liquidExt: Boolean? = true
    ): Request {
        val payload = JSONObject()
        if(liquidExt == true) {
            payload.put("extensions", liquidExt)
        }
        val path = "$origin/assertion/request/$credentialId"
        return Request.Builder()
            .url(path)
            .method("POST", JSONObject().toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .addHeader("User-Agent", userAgent)
            .build()
    }
    /**
     * @deprecated("Use postAssertionOptionsRequest instead")
     */
    fun postAssertionOptions(
        origin: String,
        userAgent: String,
        credentialId: String,
        liquidExt: Boolean? = true
    ): Call {
        return client.newCall(postAssertionOptionsRequest(origin, userAgent, credentialId, liquidExt))
    }

    fun postAssertionResultRequest(
        origin: String,
        userAgent: String,
        credential: PublicKeyCredential,
        liquidExt: JSONObject?
    ): Request {
        val rawId = credential.rawId!!.toBase64()
        val response = credential.response as AuthenticatorAssertionResponse

        val payload = JSONObject()
        payload.put("id", rawId)
        payload.put("type", "${PublicKeyCredentialType.PUBLIC_KEY}")
        payload.put("rawId", rawId)
        if(liquidExt != null) {
            val clientExtensionResults = JSONObject()
            clientExtensionResults.put("liquid", liquidExt)
            payload.put("clientExtensionResults", clientExtensionResults)
        }
        val jsonResponse = JSONObject()
        jsonResponse.put("clientDataJSON", response.clientDataJSON.toBase64())
        jsonResponse.put("authenticatorData", response.authenticatorData.toBase64())
        jsonResponse.put("signature", response.signature.toBase64())
        jsonResponse.put("userHandle", response.userHandle?.toBase64())

        payload.put("response", jsonResponse)
        return Request.Builder()
            .url("$origin/assertion/response")
            .addHeader("User-Agent", userAgent)
            .method("POST", payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
    }
    /**
     */
    fun postAssertionResult(
        origin: String,
        userAgent: String,
        credential: PublicKeyCredential,
        liquidExt: JSONObject?
    ): Call {
       return client.newCall(postAssertionResultRequest(origin, userAgent, credential, liquidExt))
    }
}
