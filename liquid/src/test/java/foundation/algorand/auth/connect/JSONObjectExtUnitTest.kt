package foundation.algorand.auth.connect

import junit.framework.TestCase.assertEquals
import org.json.JSONObject
import org.junit.Test

class JSONObjectExtUnitTest {
    @Test
    fun toIceCandidate() {
        JSONObject().apply {
            put("candidate", "sdp")
            put("sdpMid", "sdpMid")
            put("sdpMLineIndex", 1)
        }.toIceCandidate().apply {
            assertEquals("sdp", sdp)
            assertEquals("sdpMid", sdpMid)
            assertEquals(1, sdpMLineIndex)
        }
    }
}
