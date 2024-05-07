package foundation.algorand.auth.connect

import android.content.Context
import android.util.Log
import org.webrtc.*
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PeerApi(context: Context) {
    companion object {
        const val TAG = "connect.PeerApi"
    }

    // Data Channel to send and receive messages
    private var dataChannel: DataChannel? = null

    // Create the Peer Connection Factory
    private var peerConnectionFactory: PeerConnectionFactory

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        peerConnectionFactory = PeerConnectionFactory
            .builder()
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = false
                disableNetworkMonitor = false
            })
            .createPeerConnectionFactory()
    }

    // Current Peer Connection
    var peerConnection: PeerConnection? = null

    /**
     * Create a new Peer Connection
     */
    fun createPeerConnection(
        onIceCandidate: (IceCandidate) -> Unit,
        onDataChannel: (DataChannel) -> Unit,
        iceServers: List<PeerConnection.IceServer>? = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                .createIceServer()
        )
    ) {
        if (peerConnection !== null) {
            peerConnection?.close()
        }

        peerConnection = peerConnectionFactory.createPeerConnection(
            iceServers,
            object : PeerConnection.Observer {
                override fun onIceCandidate(p0: IceCandidate?) {
                    p0?.let {
                        onIceCandidate(it)
                    }
                }

                override fun onDataChannel(p0: DataChannel?) {
                    Log.d(TAG, "onDataChannel($p0)")
                    dataChannel = p0
                    onDataChannel(p0!!)
                }

                override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                    Log.d(TAG, "onIceConnectionChange($p0)")
                    if (p0 === PeerConnection.IceConnectionState.FAILED) {
                        Log.e(TAG, "ICE Connection Failed")
                    }
                }

                override fun onIceConnectionReceivingChange(p0: Boolean) {
                    Log.d(TAG, "onIceConnectionReceivingChange($p0)")
                }

                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                    Log.d(TAG, "onIceGatheringChange($p0)")
                }

                override fun onAddStream(p0: MediaStream?) {
                    Log.d(TAG, "onAddStream($p0)")
                }

                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                    Log.d(TAG, "onSignalingChange($p0)")
                }

                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
                    Log.d(TAG, "onIceCandidatesRemoved($p0)")
                }

                override fun onRemoveStream(p0: MediaStream?) {
                    Log.d(TAG, "onRemoveStream($p0)")
                }

                override fun onRenegotiationNeeded() {
                    Log.d(TAG, "onRenegotiationNeeded()")
                }

                override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
                    Log.d(TAG, "onAddTrack($p0, $p1)")
                }
            }
        )
    }
    suspend fun createPeerConnection(onIceCandidate: (IceCandidate) -> kotlin.Unit, iceServers: List<PeerConnection.IceServer>? = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
            .createIceServer()) ): DataChannel {
        return suspendCoroutine { continuation ->
            createPeerConnection(onIceCandidate,{
                continuation.resume(it)
            },iceServers)
        }
    }
    /**
     * Add an ICE Candidate
     */
    fun addIceCandidate(candidate: IceCandidate) {
        if (peerConnection === null) {
            throw Exception("peerConnection is null, ensure you are connected")
        }
        peerConnection?.addIceCandidate(candidate)
    }
    fun setLocalDescription(description: SessionDescription, onSessionDescription: (SessionDescription?) -> Unit) {
        if (peerConnection === null) {
            throw Exception("peerConnection is null, ensure you are connected")
        }
        peerConnection?.setLocalDescription(createSDPObserver(onSessionDescription), description)
    }
    /**
     * Set the Remote Description
     *
     * Handles Remote Description with a Callback Function
     */
    fun setRemoteDescription(description: SessionDescription, onSessionDescription: (SessionDescription?) -> Unit) {
        if (peerConnection === null) {
            throw Exception("peerConnection is null, ensure you are connected")
        }
        peerConnection?.setRemoteDescription(createSDPObserver(onSessionDescription), description)
    }

    /**
     * Set the Remote Description
     *
     * Handles Remote Description using Coroutines
     */
    suspend fun setRemoteDescription(description: SessionDescription): SessionDescription? {
        return suspendCoroutine { continuation ->
            setRemoteDescription(description) { sessionDescription ->
                continuation.resume(sessionDescription)
            }
        }
    }

    /**
     * Create an SDP Observer
     *
     * Used for Local and Remote Description handling
     */
    private fun createSDPObserver(onSessionDescription: (SessionDescription?) -> Unit): SdpObserver {
        return object : SdpObserver {
            override fun onSetFailure(p0: String?) {
                Log.e(TAG, "onSetFailure: $p0")
            }

            override fun onSetSuccess() {
                Log.d(TAG, "onSetSuccess")
                onSessionDescription(peerConnection?.localDescription)
            }

            override fun onCreateSuccess(p0: SessionDescription?) {
                Log.e(TAG, "onCreateSuccess")
                onSessionDescription(p0)
            }

            override fun onCreateFailure(p0: String?) {
                Log.e(TAG, "onCreateFailure: $p0")
                onSessionDescription(null)

            }
        }
    }
    fun createAnswer(onSessionDescription: (SessionDescription?) -> Unit) {
        Log.d(TAG, "createAnswer")
        if (peerConnection === null) {
            throw Exception("peerConnection is null")
        }
        peerConnection?.createAnswer(createSDPObserver(onSessionDescription), MediaConstraints())
    }
    suspend fun createAnswer(): SessionDescription? {
        return suspendCoroutine { continuation ->
            createAnswer { sessionDescription ->
                continuation.resume(sessionDescription)
            }
        }
    }
    /**
     * Create an Offer
     *
     * Handles Offer Creation with a Callback Function
     */
    fun createOffer(onSessionDescription: (SessionDescription?)->Unit) {
        if (peerConnection === null) {
            throw Exception("peerConnection is null")
        }
        peerConnection?.createOffer(createSDPObserver(onSessionDescription), MediaConstraints())
    }

    /**
     * Create an Offer
     *
     * Handles Offer Creation using Coroutines
     */
    suspend fun createOffer(): SessionDescription? {
        return suspendCoroutine { continuation ->
            createOffer { sessionDescription ->
                continuation.resume(sessionDescription)
            }
        }
    }

    fun createDataChannelObserver(
        onMessage: (String) -> Unit,
        onStateChange: ((String?) -> Unit)? = null,
        onBufferedAmountChange: ((Long) -> Unit)? = null
    ): DataChannel.Observer {
        if (peerConnection === null) {
            throw Exception("peerConnection is null")
        }
        return object : DataChannel.Observer {
            override fun onBufferedAmountChange(p0: Long) {
                Log.d(TAG, "onBufferedAmountChange($p0)")
                onBufferedAmountChange?.invoke(p0)
            }

            override fun onStateChange() {
                Log.d(TAG, "onStateChange")
                onStateChange?.invoke(dataChannel?.state().toString())
            }

            /**
             * Handle DataChannel messages
             *
             * @todo: Implement Web Provider API messages
             */
            override fun onMessage(p0: DataChannel.Buffer?) {
                Log.d(TAG, "onMessage($p0)")
                p0?.data?.let {
                    val bytes = ByteArray(it.remaining())
                    p0.data.get(bytes)
                    val payload = String(bytes)
                    onMessage(payload)
                }
            }
        }
    }

    fun createDataChannel(label: String): DataChannel? {
        if (peerConnection === null) {
            throw Exception("peerConnection is null")
        }
        dataChannel?.close()
        dataChannel = peerConnection?.createDataChannel(label, DataChannel.Init())
        return dataChannel
    }

    fun send(message: String) {
        if (dataChannel === null) {
            throw Exception("dataChannel is null")
        }
        dataChannel?.state()?.let {
            if (it !== DataChannel.State.OPEN) {
                throw Exception("dataChannel is not open")
            }
        }
        val buffer = ByteBuffer.wrap(message.toByteArray())
        dataChannel?.send(DataChannel.Buffer(buffer, false))
    }

    fun destroy() {
        dataChannel?.close()
//        dataChannel?.dispose()
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
        dataChannel = null
    }
}
