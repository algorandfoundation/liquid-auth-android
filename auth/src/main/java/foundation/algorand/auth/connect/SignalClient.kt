package foundation.algorand.auth.connect

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import qrcode.QRCode
import qrcode.color.Colors
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Signal Client
 *
 * Has two modes:
 * - Offer: Create a Peer Offer
 * - Answer: Create a Peer Answer
 */
class SignalClient @Inject constructor(
    /**
     * Origin of the Service
     */
    override val url: String,
    /**
     * Android Context
     */
    override val context: Context,
    /**
     * HTTP Client
     */
    override val client: OkHttpClient,
) : SignalInterface {
    companion object {
        const val TAG = "connect.SignalClient"
        fun generateRequestId(): Double {
            return SignalInterface.generateRequestId()
        }
    }

    var type: String? = null
    override var socket: Socket? = null
    var peerClient: PeerApi? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Generate a random Request ID
     * @TODO: Replace with UUID
     */
    override fun generateRequestId(): Double {
        return SignalClient.generateRequestId()
    }

    /**
     * Generate a QR Code
     */
    override fun qrCode(requestId: Double): Bitmap {
        val data = JSONObject()
        data.put("origin", url)
        data.put("requestId", requestId)

        val helloWorld = QRCode.ofSquares()
            .withColor(Colors.DEEP_SKY_BLUE)
            .build(data.toString())
            .render()
            .nativeImage()
        if (helloWorld !is Bitmap) {
            throw Exception("Invalid Type")
        }
        return helloWorld

    }

    /**
     * Top Level Peer Connection
     *
     * The type parameter is used to specify the type of remote peer
     */
    override suspend fun peer(requestId: Double, type: String): DataChannel? {
        createSocket()
        return suspendCoroutine { continuation ->
            scope.launch {
                val clientType = if (type === "offer") "answer" else "offer"
                peerClient = PeerApi(context)
                // Buffer ICE Candidates if they arrive before the Peer Connection is established
                val candidatesBuffer = mutableListOf<IceCandidate>()
                // If we are waiting on an offer, create a link to the address
                if(type === "offer"){
                    link(requestId)
                }
                // Listen to Remote ICE Candidates
                socket!!.on("${type}-candidate") {
                    Log.d(
                        TAG,
                        "onIce${type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}Candidate(${it[0]})"
                    )
                    val candidate = it[0] as JSONObject
                    // Buffer Candidates if the Peer Connection is not established
                    if (peerClient!!.peerConnection === null) {
                        candidatesBuffer.add(candidate.toIceCandidate())
                    } else {
                        peerClient?.addIceCandidate(candidate.toIceCandidate())
                    }
                }
                // Create Peer Connection
                peerClient!!.createPeerConnection(
                    // Handle Local ICECandidates
                    { iceCandidate ->
                    Log.d(
                        TAG,
                        "onIce${clientType.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}Candidate(${iceCandidate.toJSON()})"
                    )
                    // Send Local ICECandidates to Peer
                    socket?.emit("${clientType}-candidate", iceCandidate.toJSON())
                }, {
                    // Handle a Data Channel from the Peer
                    // This only happens for a client that creates an Answer,
                    // Offer clients are responsible for creating a datachannel
                    continuation.resume(it)
                })

                // Wait for Offer, then create Answer
                if (type === "offer") {
                    val sdp = signal(type)
                    Log.d(TAG, "Recieved the SDP!(${sdp})")
                    peerClient?.setRemoteDescription(sdp)
                    Log.d(TAG, "Set the SDP!(${sdp})")
                    if(candidatesBuffer.isNotEmpty()){
                        candidatesBuffer.forEach { candidate ->
                            peerClient?.addIceCandidate(candidate)
                        }
                    }
                    peerClient?.createAnswer { answerDescription ->
                        peerClient!!.setLocalDescription(answerDescription!!) { hasDescription ->
                            if(hasDescription === null) {
                                throw Exception("Failed to set local description")
                            }
                        }
                        Log.d(TAG, "createAnswer(${answerDescription.description})")
                        socket!!.emit("answer-description", answerDescription.description.toString())
                    }

                }
                // Create an Offer, wait for answer
                else if (type === "answer") {
                    // Create the DataChannel
                    val dc = peerClient?.createDataChannel("liquid")
                    // Create the Peering Offer
                    val offer = peerClient?.createOffer()
                    peerClient?.setLocalDescription(offer!!) {
                        if(it === null) {
                            throw Exception("Failed to set local description")
                        }
                    }
                    Log.d(TAG, "peer.createOffer(${offer?.description})")
                    socket!!.emit("offer-description", offer?.description.toString())
                    val sdp = signal(type)
                    Log.d(TAG, "peer.onAnswer(${sdp})")
                    peerClient!!.setRemoteDescription(sdp) {
                        if(it === null){
                            throw Exception("Failed to set remote description")
                        }
                        if(candidatesBuffer.isNotEmpty()){
                            candidatesBuffer.forEach { candidate ->
                                peerClient?.addIceCandidate(candidate)
                            }
                        }
                    }
                    continuation.resume(dc)
                }
            }
        }
    }

    fun handleDataChannel(
        dataChannel: DataChannel,
        onMessage: (String) -> Unit,
        onStateChange: ((String?) -> Unit)? = null,
        onBufferedAmountChange: ((Long) -> Unit)? = null
    ) {
        dataChannel.registerObserver(
            peerClient!!.createDataChannelObserver(
                onMessage,
                onStateChange,
                onBufferedAmountChange
            )
        )
    }

    /**
     * Wait for a Session Description
     */
    override suspend fun signal(type: String): SessionDescription {
        return suspendCoroutine { continuation ->
            this.socket!!.once("$type-description") {
                val description = it[0] as String
                Log.d(
                    TAG,
                    "signal.on${type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}Description($description)"
                )
                val sdpType = if (type === "offer") SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER
                continuation.resume(SessionDescription(sdpType, description))
            }
        }
    }

    override suspend fun link(
        requestId: Double
    ): LinkMessage {
        return suspendCoroutine { continuation ->
            val linkBody = JSONObject()
            linkBody.put("requestId", requestId)
            socket!!.emit("link", linkBody, Ack { args: Array<Any> ->
                val response = args[0] as JSONObject
                Log.d(TAG, "link.ack($response)")
                continuation.resume(LinkMessage.fromJson(response.toString()))
            })
        }
    }

    private fun createSocket() {
        // Handle existing connections
        if (socket !== null) {
            socket?.close()
            socket?.disconnect()
        }

        // Configure Socket Options to use the same client
        val options = IO.Options.builder()
            .build()
        options.callFactory = client
        options.webSocketFactory = client

        // Connect to the messages origin
        socket = IO.socket(url, options)
        socket?.connect()
    }

    fun disconnect() {
        socket?.close()
        socket?.disconnect()
        peerClient?.destroy()
    }
}
