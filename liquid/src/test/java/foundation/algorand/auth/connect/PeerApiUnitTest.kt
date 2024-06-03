package foundation.algorand.auth.connect

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.webrtc.*
import java.nio.ByteBuffer
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

class PeerApiUnitTest {
    private val context: Context = mockk(relaxed = true)
    private var peerApi: PeerApi? = null
    fun callPrivate(objectInstance: Any, methodName: String): KFunction<*>? {
        val privateMethod: KFunction<*>? =
            objectInstance::class.members.find { t -> return@find t.name == methodName } as KFunction<*>?
        privateMethod?.javaMethod?.isAccessible = true
        return privateMethod
    }
    @Before
    fun setupPeerConnection() {
        mockkStatic(PeerConnectionFactory::class)
        every { PeerConnectionFactory.initialize(any()) } returns Unit
        every {
            PeerConnectionFactory.builder().setOptions(any()).createPeerConnectionFactory()
        } returns mockk(relaxed = true)
        peerApi = PeerApi(context)

        peerApi!!.peerConnection = mockk<PeerConnection>(relaxed = true)
    }

    @Test
    fun createPeerConnection(){
        val onIceCandidate = { _: IceCandidate -> }
        val onDataChannel = { _: DataChannel -> }
        //val iceServers = listOf<PeerConnection.IceServer>()
        peerApi!!.createPeerConnection(onIceCandidate, onDataChannel)
        assert(peerApi!!.peerConnection !== null)

    }
    @Test
    fun addIceCandidate() {
        peerApi!!.addIceCandidate(IceCandidate("sdp", 0, "sdp"))
        verify { peerApi!!.peerConnection?.addIceCandidate(any()) }
        peerApi!!.peerConnection = null
        assertThrows(Exception::class.java) {
            peerApi!!.addIceCandidate(IceCandidate("sdp", 0, "sdp"))
        }
    }

    @Test
    fun setLocalDescription() {
        // TODO: Coroutine Tests
        peerApi!!.setLocalDescription(SessionDescription(SessionDescription.Type.OFFER, "sdp")) {}
        verify { peerApi!!.peerConnection?.setLocalDescription(any(), any()) }
        peerApi!!.peerConnection = null
        assertThrows(Exception::class.java) {
            peerApi!!.setLocalDescription(SessionDescription(SessionDescription.Type.OFFER, "sdp")) {
                assert(it === null)
            }
        }
    }
    @Test
    fun setRemoteDescription() {
        // TODO: Coroutine Tests
        peerApi!!.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, "sdp")) {}
        verify { peerApi!!.peerConnection?.setRemoteDescription(any(), any()) }
        peerApi!!.peerConnection = null
        assertThrows(Exception::class.java) {
            peerApi!!.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, "sdp")) {
                assert(it === null)
            }
        }
    }

    @Test
    fun createAnswer() {
        // TODO: Coroutine Tests
        peerApi!!.createAnswer() {}
        verify { peerApi!!.peerConnection?.createAnswer(any(), any()) }
        peerApi!!.peerConnection = null
        assertThrows(Exception::class.java) {
            peerApi!!.createAnswer() {
                assert(it === null)
            }
        }
    }

    @Test
    fun createOffer() {
        // TODO: Coroutine Tests
        peerApi!!.createOffer() {}
        verify { peerApi!!.peerConnection?.createOffer(any(), any()) }
        peerApi!!.peerConnection = null
        assertThrows(Exception::class.java) {
            peerApi!!.createOffer() {
                assert(it === null)
            }
        }
    }

    @Test
    fun createDataChannel() {
        peerApi!!.createDataChannel("label")
        verify { peerApi!!.peerConnection?.createDataChannel("label", any()) }
        peerApi!!.peerConnection = null
        assertThrows(Exception::class.java) {
            peerApi!!.createDataChannel("label")
        }
    }
    @Test
    fun createSdpObserver(){
        val method = callPrivate(peerApi!!, "createSDPObserver")
        val sessionDescriptionFixture = SessionDescription(SessionDescription.Type.OFFER, "sdp")
        val observer: SdpObserver? = method?.call(peerApi, { sessionDescription: SessionDescription? ->
            if(sessionDescription !== null && sessionDescription.type !== null){
                assert(sessionDescription === sessionDescriptionFixture)
            }
        }) as SdpObserver?
        assert(observer is SdpObserver)
        observer!!.onSetFailure("error")
        observer.onCreateSuccess(sessionDescriptionFixture)
        observer.onCreateFailure("error")
        observer.onSetSuccess()
    }
    @Test
    fun createDataChannelObserver(){
        val onMessage = { message: String -> assert(message == "message") }
        val onStateChange = { state: String? -> assert(state === "null") }
        val onBufferedAmountChange = { amount: Long -> assert(amount == 0L) }

        val observer = peerApi!!.createDataChannelObserver(onMessage,onStateChange,onBufferedAmountChange)

        observer.onMessage(DataChannel.Buffer(ByteBuffer.wrap("message".toByteArray()), false))
        observer.onStateChange()
        observer.onBufferedAmountChange(0)

        val defaultObserver = peerApi!!.createDataChannelObserver(onMessage)
        defaultObserver.onMessage(DataChannel.Buffer(ByteBuffer.wrap("message".toByteArray()), false))

        peerApi!!.peerConnection = null
        assertThrows(Exception::class.java) {
            peerApi!!.createDataChannelObserver(onMessage)
        }

    }
    @Test
    fun send() {
        peerApi!!.peerConnection = mockk(relaxed = true)
        peerApi!!.dataChannel = mockk(relaxed = true)
        every { peerApi!!.dataChannel?.state() } returns DataChannel.State.OPEN
        peerApi!!.send("message")
        verify { peerApi!!.dataChannel?.send(any()) }

        every { peerApi!!.dataChannel?.state() } returns DataChannel.State.CLOSED
        assertThrows(Exception::class.java) {
            peerApi!!.send("message")
        }

        peerApi!!.dataChannel = null
        assertThrows(Exception::class.java) {
            peerApi!!.send("message")
        }

    }
    @Test
    fun destroy(){
        peerApi!!.peerConnection = mockk(relaxed = true)
        peerApi!!.dataChannel = mockk(relaxed = true)
        peerApi!!.destroy()
        assert(peerApi!!.peerConnection === null)
        assert(peerApi!!.dataChannel === null)
    }
}
