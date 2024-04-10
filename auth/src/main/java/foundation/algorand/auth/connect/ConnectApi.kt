package foundation.algorand.auth.connect

import android.content.Context
import android.util.Log
import foundation.algorand.auth.Cookie
import foundation.algorand.auth.crypto.KeyPairs
import foundation.algorand.auth.crypto.decodeBase64
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.commons.codec.binary.Base64
import org.json.JSONObject
import org.webrtc.*
import ru.gildor.coroutines.okhttp.await
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import javax.inject.Inject

class ConnectApi @Inject constructor(
    private val client: OkHttpClient,
) {
    companion object {
        const val TAG = "connect.Client"
    }

    private var socket: Socket? = null
    private var peerApi: PeerApi? = null

    /**
     * Submit Verification Message
     *
     */
    fun submit(message: Message, keyPair: KeyPair?) : Call {
        if(message.wallet === null){
            throw Exception("Missing wallet public key")
        }
        if(message.signature === null){
            if(keyPair === null){
                throw Exception("Message is not signed and no keyPair provided")
            } else {
                message.sign(keyPair)
            }
        }

        // TODO: Create specification for URL Scheme
        val path = "${message.origin}/connect/response"
        val body = message.toJSON().toString().toRequestBody("application/json".toMediaTypeOrNull())
        return client.newCall(
            Request.Builder()
                .url(path)
                .method("POST", body)
                .build()
        )
    }

    /**
     * Connect to the Origin Signalling Server
     */
    suspend fun connect(context: Context, message: Message, keyPair: KeyPair? = null): String? {
        // Submit the message to the origin server,
        // this allows for a valid session between the two parties
        val response = submit(message, keyPair).await()

        // Handle existing connections
        if(socket !== null){
            socket?.close()
            socket?.disconnect()
        }

        // Configure Socket Options to use the same client
        val options = IO.Options.builder()
            .build()
        options.callFactory = client
        options.webSocketFactory = client

        // Connect to the messages origin
        socket = IO.socket(message.origin, options)
        socket?.connect()
        socket!!.emit("handshake", message.toJSON())
        // Create WebRTC Peer API
        peerApi = PeerApi(context)
        peerApi?.createPeerConnection({ iceCandidate ->
            Log.d(TAG, "onIceOfferCandidate($iceCandidate)")
            socket?.emit("call-candidate", iceCandidate.toJSON())
        })

        // Handle SDP Answers
        socket?.on("answer-description") {
            Log.d(TAG, "onAnswerDescription(${it[0]})")

            val description = SessionDescription(SessionDescription.Type.ANSWER, it[0].toString())
            peerApi?.setRemoteDescription(description)
        }

        socket?.on("answer-candidate") {
            Log.d(TAG, "onIceAnswerCandidate($it)")
            peerApi?.addIceCandidate((it[0] as JSONObject).toIceCandidate())
        }

        // Handle DataChannel Messages
        peerApi?.createDataChannel("data") {
            Log.d(TAG, "onDataChannelMessage($it)")
            val bytes = it.decodeBase64()
            val signatureBytes = KeyPairs.rawSignBytes(bytes, keyPair!!.private)
            val sig = Base64.encodeBase64URLSafeString(signatureBytes)
            peerApi?.send(sig)
        }

        // Create the Peering Offer
        peerApi?.createOffer {
            Log.d(TAG, "createOffer(${it?.description})")
            socket!!.emit("call-description", it?.description.toString())
        }
        return Cookie.fromResponse(response)
    }
    fun disconnect(){
        socket?.close()
        socket?.disconnect()
        peerApi?.destroy()
    }
}
