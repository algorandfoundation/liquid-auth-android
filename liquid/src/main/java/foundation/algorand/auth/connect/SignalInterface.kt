package foundation.algorand.auth.connect

import android.content.Context
import android.graphics.Bitmap
import okhttp3.OkHttpClient
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.PeerConnection
import org.webrtc.DataChannel
import org.webrtc.MediaStreamTrack
import org.webrtc.SessionDescription
import kotlin.math.floor
import com.fasterxml.uuid.Generators

class LinkMessage(val requestId: String, val wallet: String, val credId: String? = null) {
    companion object {
        const val TAG = "connect.LinkMessage"
        fun fromJson(json: String): LinkMessage {
            val data = JSONObject(json).get("data") as JSONObject
            val requestId = data.get("requestId").toString()
            val wallet = data.get("wallet").toString()
            val credId = data.get("credId").toString()
            return LinkMessage(requestId, wallet, credId)
        }
    }
    fun toJson(): JSONObject {
        val result = JSONObject()
        result.put("requestId", requestId)
        result.put("wallet", wallet)
        result.put("credId", credId)
        return result
    }
}

interface SignalInterface {
    val url: String // URL of Signal Server
    val client: OkHttpClient // HTTP Client
    val socket: Socket? // Socket IO Client
    val context: Context? // Android Context

    companion object {
        fun generateRequestId(): String {
            val uuid = Generators.timeBasedEpochRandomGenerator().generate()
            return uuid.toString()
        }
    }

    /**
     * Generate a random Request ID
     */
    fun generateRequestId(): String

    /**
     * Generate a QR Code
     */
    fun qrCode(requestId: String, logo: Bitmap?, logoSize: Int? = null, color: String? = null, backgroundColor: String? = null): Bitmap

    /**
     * Top Level Peer Connection
     *
     * @param dataChannels optional map of channel label -> [DataChannel.Init] to
     *   open when acting as the offerer (mirrors `options.dataChannels` in
     *   `liquid-auth-js`). Defaults to a single `liquid` channel.
     * @param tracks optional local media tracks to add before negotiation
     *   (mirrors `options.tracks` in `liquid-auth-js`).
     */
    suspend fun peer(
        requestId: String,
        type: String,
        iceServers: List<PeerConnection.IceServer>?,
        dataChannels: Map<String, DataChannel.Init>? = null,
        tracks: List<MediaStreamTrack>? = null
    ): DataChannel?
    /**
     * Waits for a remote client to authenticate with the server
     */
    suspend fun link(requestId: String): LinkMessage

    /**
     * Exchange descriptions with the remote client
     */
    suspend fun signal(type: String): SessionDescription
}
