package foundation.algorand.auth.connect

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.webrtc.DataChannel
import org.webrtc.PeerConnectionFactory


val context = mockk<Context>()
val url = "https://liquid-auth.onrender.com"
val httpClient = mockk<OkHttpClient>()

class SignalClientUnitTest {
    var signalClient: MockSignalClient? = null

    class MockSignalClient : SignalClient(url, context, httpClient) {
        override fun createSocket() {
            if (socket == null) {
                socket = mockk<Socket>(relaxed = true)
            }
        }

        private val scope = CoroutineScope(Dispatchers.Main)
    }

    @Before
    fun setUp() {
        val httpClient = mockk<OkHttpClient>()
        // Peer Connection Factory
        mockkStatic(PeerConnectionFactory::class)
        every { PeerConnectionFactory.initialize(any()) } returns Unit
        every {
            PeerConnectionFactory.builder().setOptions(any()).createPeerConnectionFactory()
        } returns mockk(relaxed = true)
        // SocketIO
        mockkStatic(IO::class)
        every { IO.socket(url) } returns mockk<Socket>(relaxed = true)

        signalClient = MockSignalClient()
        signalClient!!.createSocket()
        signalClient!!.peerClient = mockk<PeerApi>(relaxed = true)
    }

    @Test
    fun testSignalClient() {
        assert(signalClient!!.url == url)
    }

    @Test
    fun generateRequestId() {
        val requestId = signalClient?.generateRequestId()
        assert(requestId is Double)
    }

    @Test
    @Ignore("TODO: QR Code Tests")
    fun qrCode() {
        TODO("QR Code Tests")
    }

    @Test
    @Ignore("TODO: Peer Test")
    fun peer() {
        TODO("Peer Test")
    }

    @Test
    fun handleDataChannel() {
        val dataChannel = mockk<DataChannel>()
        every { dataChannel.registerObserver(any()) } returns Unit
        val onMessage = { msg: String -> assert(msg == "hello") }
        signalClient?.handleDataChannel(dataChannel, onMessage)
    }

    @Test
    @Ignore("TODO: Signal test")
    fun signal() {
        TODO("Signal test")
    }

    @Test
    @Ignore("TODO: Link test")
    fun link() {
        TODO("Link test")
    }

    @Test
    fun disconnect() {
        signalClient?.disconnect()
        verify { signalClient?.socket?.close() }
        verify { signalClient?.socket?.disconnect() }
    }

}
