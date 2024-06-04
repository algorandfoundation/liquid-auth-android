package foundation.algorand.auth

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Test

class CookieUnitTest {
    val sidFixture = "1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6"
    val connectSidFixture = "connect.sid=s%3A$sidFixture"
    @Test
    fun cookie(){
        val cookie = Cookie()
        assert(cookie != null)
    }
    @Test
    fun fromResponse() {
        val mockRequest = Request.Builder()
            .url("https://some-url.com")
            .build()
        val responseBuilder = Response.Builder()
            .request(mockRequest)
            .protocol(Protocol.HTTP_2)
            .code(401) // status code
            .message("")
            .body(
                ResponseBody.create(
                    "application/json; charset=utf-8".toMediaType(),
                "{}"
            ))
        val noCookie = Cookie.fromResponse(responseBuilder.header("set-cookie", "not-valid").build())
        assert(noCookie == null)

        val cookie = Cookie.fromResponse(
            responseBuilder
                .header("set-cookie", connectSidFixture)
                .build()
        )
        assert(cookie == connectSidFixture)
    }
    @Test
    fun getID() {
        val id = Cookie.getID(connectSidFixture)
        assert(id == sidFixture)
    }
}
