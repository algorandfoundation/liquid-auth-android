package foundation.algorand.auth.connect

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat.Builder
import androidx.core.app.ServiceCompat
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection


class SignalService : Service() {
    companion object {
        const val TAG = "auth.connect.Service"
        const val LIQUID_NOTIFICATION_ID = 1337

    }
    // Last known deep-link referrer
    var lastKnownReferer: String? = null
    var isDeepLink: Boolean = true

    // Liquid Signal Components
    var signalClient: SignalClient? = null
    var peerClient: PeerApi? = null

    // Native WebRTC Components
    var dataChannel: DataChannel? = null
    var peerConnection: PeerConnection? = null

    // Simple service binding
    inner class LocalBinder : Binder() {
        fun getServerInstance(): SignalService {
            return this@SignalService
        }
    }

    // Service Binder
    var mBinder: IBinder = LocalBinder()

    /**
     * Handle Service Binding
     */
    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    /**
     * Start the Service in the Foreground
     */
    fun startForeground(notificationBuilder: Builder, notificationId: Int) {
        try {
            ServiceCompat.startForeground(
                this,
                notificationId,
                notificationBuilder
                    .build(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {
                Log.e(TAG, "Foreground service not allowed")
            }
        }
    }

    /**
     * Notify the User
     */
    fun notify(
        notificationBuilder: Builder,
        notificationId: Int
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            notificationId,
            notificationBuilder.build()
        )
    }

    /**
     * Start the Liquid WebRTC Service
     *
     * This creates a SignalClient and connects to the Signal Server
     */
    fun start(
        url: String,
        httpClient: OkHttpClient,
        notificationBuilder: Builder,
        notificationId: Int,
        activityClass: Class<out Activity>
    ) {
        startForeground(notificationBuilder.setContentIntent(createPendingIntent(activityClass, 0)), notificationId)
        val isInitialized = signalClient != null
        if (isInitialized) {
            signalClient?.disconnect()
        }
        signalClient = SignalClient(url, this@SignalService, httpClient)
    }

    /**
     * Stop the Liquid WebRTC Service
     */
    fun stop() {
        signalClient?.disconnect()
        signalClient = null
    }

    /**
     * Connect to a Peer by Request ID
     *
     * @param dataChannels optional map of channel label -> [DataChannel.Init] to
     *   open (defaults to a single `liquid` channel).
     * @param tracks optional local media tracks to add before negotiation.
     * @param onTrack optional callback invoked when a remote media track arrives.
     * @param onPresence optional callback for server-broadcast `presence`
     *   updates for the `requestId` room.
     * @param onLinkError optional callback for signaling `exception` events
     *   (e.g. `link-error` room refusals).
     * @param onConnectionStateChange optional callback for peer ICE connection
     *   state changes (`CONNECTED`, `DISCONNECTED`, `FAILED`, ...).
     */
    suspend fun peer(
        requestId: String,
        type: String,
        iceServers: List<PeerConnection.IceServer>,
        dataChannels: Map<String, DataChannel.Init>? = null,
        tracks: List<MediaStreamTrack>? = null,
        onTrack: ((MediaStreamTrack) -> Unit)? = null,
        onPresence: ((JSONObject) -> Unit)? = null,
        onLinkError: ((JSONObject) -> Unit)? = null,
        onConnectionStateChange: ((String) -> Unit)? = null,
    ) {
        // Register the socket/peer callbacks before negotiation so the socket
        // listeners (presence/exception) are attached when it is created.
        signalClient?.onPresence = onPresence
        signalClient?.onLinkError = onLinkError
        signalClient?.onConnectionStateChange = onConnectionStateChange
        dataChannel = signalClient?.peer(requestId, type, iceServers, dataChannels, tracks)
        peerClient = signalClient?.peerClient
        peerClient?.onTrack = onTrack
        peerConnection = peerClient?.peerConnection
    }

    /**
     * Abort an in-flight [peer] negotiation without stopping the service.
     */
    fun cancel() {
        signalClient?.cancel()
    }
    /**
     * Create a PendingIntent
     *
     * This PendingIntent is used to open the SignTransactionActivity when a transaction message is received
     */
    fun createPendingIntent(activityClass: Class<out Activity>, requestCode: Int = 0, msg: String? = null): PendingIntent {
        val answerIntent = Intent(this@SignalService, activityClass)
        answerIntent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        msg?.let {
            answerIntent.putExtra("msg", it)
        }
        return TaskStackBuilder.create(this@SignalService).run {
            addNextIntentWithParentStack(answerIntent)
            getPendingIntent(
                requestCode,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
    /**
     * Handle Messages and State Changes
     *
     * When the activity is visible, it will call back to the onMessage function.
     * Otherwise, it will create a notification with a PendingIntent for the AnswerActivity
     */
    fun handleMessages(
        activity: Activity,
        onMessage: (label: String, msg: String) -> Unit,
        onStateChange: ((label: String, state: String?) -> Unit)? = null,
        notificationBuilder: Builder,
        notificationId: Int = LIQUID_NOTIFICATION_ID,
        activityClass: Class<out Activity>
    ) {
        var requestCode = 1
        val serviceIntentRequestCode = 0
        // Register observers on every negotiated data channel
        signalClient?.handleDataChannels({ label, msg ->
            if (activity.hasWindowFocus()) {
                onMessage(label, msg)
                return@handleDataChannels
            }
            Log.d(TAG, "DataChannel[$label] Message: $msg")
            notify(
                notificationBuilder
                    .setContentText(msg)
                    .setContentIntent(createPendingIntent(activityClass, requestCode, msg)),
                notificationId
            )
            requestCode += 1
        }, { label, state ->
            if (state == "CLOSED" || state == "CLOSING") {
                notify(
                    notificationBuilder
                        .setContentText("Tap to open the app.")
                        .setOnlyAlertOnce(true)
                        .setContentIntent(createPendingIntent(activityClass, serviceIntentRequestCode,null))
                , notificationId
                )
            }
            onStateChange?.invoke(label, state)
        })
    }

    fun updateLastKnownReferer(referer: String?) {
        lastKnownReferer = referer
    }

    fun updateDeepLinkFlag(isDeepLink: Boolean) {
        this.isDeepLink = isDeepLink
    }

    /**
     * Send a Message over the primary (`liquid`) channel
     */
    fun send(msg: String) {
        Log.d(TAG, "Sending: $msg from $lastKnownReferer")
        peerClient?.send(msg)
    }

    /**
     * Send a Message over a specific named channel
     */
    fun send(label: String, msg: String) {
        Log.d(TAG, "Sending to [$label]: $msg from $lastKnownReferer")
        peerClient?.send(label, msg)
    }
}
