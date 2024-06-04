package foundation.algorand.auth.fido2

import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import io.mockk.every
import io.mockk.mockk
import okhttp3.Call
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.Before
import org.junit.Test

class AttestationApiUnitTest {
    val httpClient = mockk<OkHttpClient>()
    val attestationApi = AttestationApi(httpClient)
    @Before
    fun setUp() {
        every { httpClient.newCall(any()) } returns mockk<Call>()
    }
    @Test
    fun testAttestationApi() {
        assert(attestationApi.client == httpClient)
    }
    @Test
    fun postAttestationOptionsRequest() {
        val origin = "https://liquid-auth.onrender.com"
        val userAgent = "userAgent"
        val request = attestationApi.postAttestationOptionsRequest(origin, userAgent)
        assert(request.url.toString() == "$origin/attestation/request")
        assert(request.method == "POST")
        assert(request.headers["User-Agent"] == userAgent)
    }
    @Test
    fun postAttestationOptions() {
        val origin = "https://liquid-auth.onrender.com"
        val userAgent = "userAgent"
        val call = attestationApi.postAttestationOptions(origin, userAgent)
        assert(call is Call)
    }
    @Test
    fun postAttestationResultRequest() {
        val origin = "https://liquid-auth.onrender.com"
        val userAgent = "userAgent"

        val attestationResponse = mockk<AuthenticatorAttestationResponse>()
        every { attestationResponse.clientDataJSON } returns byteArrayOf(1, 2, 3)
        every { attestationResponse.attestationObject } returns byteArrayOf(1, 2, 3)
        val credential = mockk<PublicKeyCredential>()
        every { credential.rawId } returns byteArrayOf(1, 2, 3)
        every { credential.response } returns attestationResponse
        val request = attestationApi.postAttestationResultRequest(origin, userAgent, credential, JSONObject())
        assert(request.url.toString() == "$origin/attestation/response")
        assert(request.method == "POST")
        assert(request.headers["User-Agent"] == userAgent)

        val noLiquid = attestationApi.postAttestationResultRequest(origin, userAgent, credential)
        assert(noLiquid.url.toString() == "$origin/attestation/response")
        assert(noLiquid.method == "POST")
        assert(noLiquid.headers["User-Agent"] == userAgent)
    }
    @Test
    fun postAttestationResult() {
        val origin = "https://liquid-auth.onrender.com"
        val userAgent = "userAgent"
        val attestationResponse = mockk<AuthenticatorAttestationResponse>()
        every { attestationResponse.clientDataJSON } returns byteArrayOf(1, 2, 3)
        every { attestationResponse.attestationObject } returns byteArrayOf(1, 2, 3)
        val credential = mockk<PublicKeyCredential>()
        every { credential.rawId } returns byteArrayOf(1, 2, 3)
        every { credential.response } returns attestationResponse
        val call = attestationApi.postAttestationResult(origin, userAgent, credential)
        assert(call is Call)
    }
}
