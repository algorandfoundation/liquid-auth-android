package foundation.algorand.auth.connect

import android.content.Context
import android.graphics.Bitmap
import okhttp3.OkHttpClient
import io.socket.client.Socket
import org.webrtc.PeerConnection
import org.webrtc.DataChannel
import org.webrtc.SessionDescription
import kotlin.math.floor

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
    fun qrCode(requestId: Double, logo: Bitmap?, logoSize: Int? = null, color: String? = null, backgroundColor: String? = null): Bitmap

    /**
     * Top Level Peer Connection
     */
    suspend fun peer(requestId: Double, type: String, iceServers: List<PeerConnection.IceServer>?): DataChannel?
    /**
     * Waits for a remote client to authenticate with the server
     */
    suspend fun link(requestId: Double): LinkMessage

    /**
     * Exchange descriptions with the remote client
     */
    suspend fun signal(type: String): SessionDescription
}
