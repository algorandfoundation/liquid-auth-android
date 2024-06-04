package foundation.algorand.auth.fido2

import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import io.mockk.every
import io.mockk.mockk
import okhttp3.Call
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.Before
import org.junit.Test

class AssertionApiUnitTest {
    val httpClient = mockk<OkHttpClient>()
    val assertionApi = AssertionApi(httpClient)
    @Before
    fun setUp() {
        every { httpClient.newCall(any()) } returns mockk<Call>()
    }
    @Test
    fun testAssertionApi() {
        assert(assertionApi.client == httpClient)
    }
    @Test
    fun postAssertionOptionsRequest(){
        val origin = "https://liquid-auth.onrender.com"
        val userAgent = "userAgent"
        val credentialId = "credentialId"
        val request = assertionApi.postAssertionOptionsRequest(origin, userAgent, credentialId)
        assert(request.url.toString() == "$origin/assertion/request/$credentialId")
        assert(request.method == "POST")
        assert(request.headers["User-Agent"] == userAgent)
        // TODO: assert payload

        val noLiquidRequest = assertionApi.postAssertionOptionsRequest(origin, userAgent, credentialId, false)
        assert(noLiquidRequest.url.toString() == "$origin/assertion/request/$credentialId")
        assert(noLiquidRequest.method == "POST")
        assert(noLiquidRequest.headers["User-Agent"] == userAgent)
        // TODO: assert payload

    }
    @Test
    fun postAssertionOptions(){
        val origin = "https://liquid-auth.onrender.com"
        val userAgent = "userAgent"
        val credentialId = "credentialId"
        val call = assertionApi.postAssertionOptions(origin, userAgent, credentialId, false)
        assert(call is Call)

        val defaultCall = assertionApi.postAssertionOptions(origin, userAgent, credentialId)
        assert(call is Call)
    }
    @Test
    fun postAssertionResultRequest(){
        val origin = "https://liquid-auth.onrender.com"
        val userAgent = "userAgent"
        val credential = mockk<PublicKeyCredential>()
        val assertionResponse = mockk<AuthenticatorAssertionResponse>()
        every { assertionResponse.clientDataJSON } returns byteArrayOf(1, 2, 3)
        every { assertionResponse.authenticatorData } returns byteArrayOf(1, 2, 3)
        every { assertionResponse.signature } returns byteArrayOf(1, 2, 3)
        every { assertionResponse.userHandle } returns byteArrayOf(1, 2, 3)
        every { credential.rawId } returns byteArrayOf(1, 2, 3)
        every { credential.response } returns assertionResponse
        val liquidExt = mockk<JSONObject>()
        val request = assertionApi.postAssertionResultRequest(origin, userAgent, credential, liquidExt)
        assert(request.url.toString() == "$origin/assertion/response")
        assert(request.method == "POST")
        assert(request.headers["User-Agent"] == userAgent)
    }
    @Test
    fun postAssertionResult(){
        val origin = "https://liquid-auth.onrender.com"
        val userAgent = "userAgent"
        val credential = mockk<PublicKeyCredential>()
        val assertionResponse = mockk<AuthenticatorAssertionResponse>()
        every { assertionResponse.clientDataJSON } returns byteArrayOf(1, 2, 3)
        every { assertionResponse.authenticatorData } returns byteArrayOf(1, 2, 3)
        every { assertionResponse.signature } returns byteArrayOf(1, 2, 3)
        every { assertionResponse.userHandle } returns byteArrayOf(1, 2, 3)
        every { credential.rawId } returns byteArrayOf(1, 2, 3)
        every { credential.response } returns assertionResponse
        val liquidExt = mockk<JSONObject>()
        val call = assertionApi.postAssertionResult(origin, userAgent, credential, liquidExt)
        assert(call is Call)
    }
}
