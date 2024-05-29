package foundation.algorand.auth.connect

import android.net.Uri
import com.google.mlkit.vision.barcode.common.Barcode
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class AuthMessageUnitTest {

    @Test
    fun fromJSONString() {
        val origin = "https://localhost:3000"
        val requestId = SignalClient.generateRequestId()
        val message = AuthMessage.fromString("{\"requestId\":\"$requestId\", \"origin\":\"$origin\"}")
        assertEquals(requestId, message.requestId, 0.0)
        assertEquals(origin, message.origin)
    }
    @Test
    fun fromURI(){
        val origin = "liquid://localhost"
        val requestId = SignalClient.generateRequestId()

        mockkStatic(Uri::class)

        every { Uri.parse(any()).host } returns origin.replace("liquid://", "")
        every { Uri.parse(any()).query } returns "requestId=$requestId"
        val message = AuthMessage.fromUri(Uri.parse("liquid://$origin/?requestId=$requestId"))
        assertEquals(requestId, message.requestId, 0.0)
        assertEquals(message.origin, "https://localhost")
    }

    @Test
    fun fromBarcode(){
        // Fixtures
        val origin = "localhost"
        val requestId = SignalClient.generateRequestId()

        // Mocks
        val barcode = mockk<Barcode>()
        every{barcode.displayValue} returns "liquid://$origin/?requestId=$requestId"
        mockkStatic(Uri::class)
        every { Uri.parse(any()).host } returns origin.replace("liquid://", "")
        every { Uri.parse(any()).query } returns "requestId=$requestId"

        // Parse Message
        val message = AuthMessage.fromBarcode(barcode)
        assertEquals(requestId, message.requestId, 0.0)
        assertEquals(message.origin, "https://localhost")
    }

}
