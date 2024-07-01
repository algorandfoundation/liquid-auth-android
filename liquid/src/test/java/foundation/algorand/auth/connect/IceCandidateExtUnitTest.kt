package foundation.algorand.auth.connect
import org.junit.Test

import org.junit.Assert.*
import org.webrtc.IceCandidate

class IceCandidateExtUnitTest {
    @Test
    fun toJSON() {
        val json = IceCandidate("sdpMid", 1, "sdp").toJSON()
        assertEquals("sdp", json.getString("candidate"))
        assertEquals("sdpMid", json.getString("sdpMid"))
        assertEquals(1, json.getInt("sdpMLineIndex"))
    }
}
