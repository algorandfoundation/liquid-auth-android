package foundation.algorand.auth.connect

import android.content.Context
import android.graphics.Bitmap
import okhttp3.OkHttpClient
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.SessionDescription
import kotlin.math.floor

class LinkMessage(val requestId: Double, val wallet: String, val credId: String? = null) {
    companion object {
        const val TAG = "connect.LinkMessage"
        fun fromJson(json: String): LinkMessage {
            val data = JSONObject(json).get("data") as JSONObject
            val requestId = data.get("requestId").toString().toDouble()
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
        fun generateRequestId(): Double {return floor(Math.random() * 1000000)}
    }

    /**
     * Generate a random Request ID
     */
    fun generateRequestId(): Double

    /**
     * Generate a QR Code
     */
    fun qrCode(requestId: Double): Bitmap

    /**
     * Top Level Peer Connection
     */
    suspend fun peer(requestId: Double, type: String): DataChannel?
    /**
     * Waits for a remote client to authenticate with the server
     */
    suspend fun link(requestId: Double): LinkMessage

    /**
     * Exchange descriptions with the remote client
     */
    suspend fun signal(type: String): SessionDescription
}
