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

    // Primary Data Channel to send and receive messages (kept for backwards
    // compatibility). Points at the `liquid` channel when present, otherwise the
    // most recently created/received channel.
    private var dataChannel: DataChannel? = null

    // All negotiated Data Channels, keyed by their label. This allows callers to
    // open and address multiple named channels (e.g. `ac2-v1`, `ac2-stream`).
    val dataChannels = mutableMapOf<String, DataChannel>()

    // Invoked when a remote media track is added to the connection.
    var onTrack: ((MediaStreamTrack) -> Unit)? = null

    // Invoked when the peer connection's ICE connection state changes
    // (e.g. `CONNECTED`, `DISCONNECTED`, `FAILED`). Lets consumers monitor
    // connectivity/reduced connection stats without a direct handle on the
    // native `PeerConnection`.
    var onConnectionStateChange: ((String) -> Unit)? = null

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
                    p0?.let {
                        dataChannels[it.label()] = it
                        dataChannel = it
                        onDataChannel(it)
                    }
                }

                override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                    Log.d(TAG, "onIceConnectionChange($p0)")
                    p0?.let { onConnectionStateChange?.invoke(it.toString()) }
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
                    p0?.track()?.let { onTrack?.invoke(it) }
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
     * Add a local media track to the Peer Connection.
     *
     * Mirrors the `options.tracks` handling in the `liquid-auth-js` client,
     * where each supplied track is added to the peer before negotiation.
     */
    fun addTrack(track: MediaStreamTrack, streamIds: List<String> = emptyList()) {
        if (peerConnection === null) {
            throw Exception("peerConnection is null, ensure you are connected")
        }
        peerConnection?.addTrack(track, streamIds)
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

    /**
     * Create an observer for a specific [dataChannel].
     *
     * The channel `label` is forwarded to every callback so multiple named
     * channels can be multiplexed onto the same handlers.
     */
    fun createDataChannelObserver(
        dataChannel: DataChannel,
        label: String,
        onMessage: (label: String, message: String) -> Unit,
        onStateChange: ((label: String, state: String?) -> Unit)? = null,
        onBufferedAmountChange: ((label: String, amount: Long) -> Unit)? = null
    ): DataChannel.Observer {
        if (peerConnection === null) {
            throw Exception("peerConnection is null")
        }
        return object : DataChannel.Observer {
            override fun onBufferedAmountChange(p0: Long) {
                Log.d(TAG, "onBufferedAmountChange($label, $p0)")
                onBufferedAmountChange?.invoke(label, p0)
            }

            override fun onStateChange() {
                Log.d(TAG, "onStateChange($label)")
                onStateChange?.invoke(label, dataChannel.state().toString())
            }

            /**
             * Handle DataChannel messages
             */
            override fun onMessage(p0: DataChannel.Buffer?) {
                Log.d(TAG, "onMessage($label, $p0)")
                p0?.data?.let {
                    val bytes = ByteArray(it.remaining())
                    p0.data.get(bytes)
                    val payload = String(bytes)
                    onMessage(label, payload)
                }
            }
        }
    }

    /**
     * Create a named Data Channel.
     *
     * The optional [init] mirrors `RTCDataChannelInit` from `liquid-auth-js`
     * (`ordered`, `maxRetransmits`, `negotiated`, etc.). The created channel is
     * tracked by label so it can be addressed later via [send] or [getDataChannel].
     */
    fun createDataChannel(label: String, init: DataChannel.Init = DataChannel.Init()): DataChannel? {
        if (peerConnection === null) {
            throw Exception("peerConnection is null")
        }
        dataChannels[label]?.close()
        val channel = peerConnection?.createDataChannel(label, init)
        if (channel != null) {
            dataChannels[label] = channel
            dataChannel = channel
        }
        return channel
    }

    /**
     * Lookup a previously negotiated channel by label.
     */
    fun getDataChannel(label: String): DataChannel? {
        return dataChannels[label]
    }

    /**
     * Send a message over the primary (or `liquid`) channel.
     */
    fun send(message: String) {
        val channel = dataChannels["liquid"] ?: dataChannel
            ?: throw Exception("dataChannel is null")
        sendToChannel(channel, message)
    }

    /**
     * Send a message over a specific named channel.
     */
    fun send(label: String, message: String) {
        val channel = dataChannels[label]
            ?: throw Exception("dataChannel '$label' is null")
        sendToChannel(channel, message)
    }

    private fun sendToChannel(channel: DataChannel, message: String) {
        if (channel.state() !== DataChannel.State.OPEN) {
            throw Exception("dataChannel '${channel.label()}' is not open")
        }
        val buffer = ByteBuffer.wrap(message.toByteArray())
        channel.send(DataChannel.Buffer(buffer, false))
    }

    fun destroy() {
        dataChannels.values.forEach { it.close() }
        dataChannels.clear()
        dataChannel?.close()
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
        dataChannel = null
    }
}
