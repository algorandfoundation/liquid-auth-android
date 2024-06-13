package foundation.algorand.demo.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import foundation.algorand.auth.connect.PeerApi
import foundation.algorand.auth.connect.SignalClient
import foundation.algorand.demo.AnswerActivity
import foundation.algorand.demo.BuildConfig
import foundation.algorand.demo.R
import okhttp3.OkHttpClient
import org.webrtc.DataChannel
import org.webrtc.PeerConnection


class LiquidWebRTCService : Service() {
    companion object {
        const val TAG = "LiquidWebRTCService"
        const val LIQUID_NOTIFICATION_CHANNEL_ID = "LIQUID_CHANNEL"
        const val LIQUID_PEER_NOTIFICATION_CHANNEL_ID = "LIQUID_PEER_CHANNEL"
        const val LIQUID_NOTIFICATION_ID = 1337
    }

    // Random Notification ID
    var nextNotificationId = LIQUID_NOTIFICATION_ID + 1

    // Last known deep-link referrer
    var lastKnownReferer: String? = null

    // Liquid Signal Components
    var signalClient: SignalClient? = null
    var peerClient: PeerApi? = null

    // Native WebRTC Components
    var dataChannel: DataChannel? = null
    var peerConnection: PeerConnection? = null
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80").setUsername(BuildConfig.TURN_USERNAME)
            .setPassword(
                BuildConfig.TURN_CREDENTIAL
            ).createIceServer(),
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80?transport=tcp")
            .setUsername(BuildConfig.TURN_USERNAME).setPassword(
                BuildConfig.TURN_CREDENTIAL
            ).createIceServer(),
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:443").setUsername(BuildConfig.TURN_USERNAME)
            .setPassword(
                BuildConfig.TURN_CREDENTIAL
            ).createIceServer(),
        PeerConnection.IceServer.builder("turns:global.relay.metered.ca:443?transport=tcp")
            .setUsername(BuildConfig.TURN_USERNAME).setPassword(
                BuildConfig.TURN_CREDENTIAL
            ).createIceServer()
    )

    // Simple service binding
    inner class LocalBinder : Binder() {
        fun getServerInstance(): LiquidWebRTCService {
            return this@LiquidWebRTCService
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
     * Handle Service Creation
     *
     * Creates a Notification Channel and starts the service in the foreground
     */
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground()
    }

    /**
     * Create a Notification Channel
     *
     * This notification channel is used to group notifications from the Liquid WebRTC Service
     */
    fun createNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                LIQUID_NOTIFICATION_CHANNEL_ID,
                "WebRTC Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                LIQUID_PEER_NOTIFICATION_CHANNEL_ID,
                "P2P Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Start the Service in the Foreground
     */
    fun startForeground() {
        try {
            ServiceCompat.startForeground(
                this,
                LIQUID_NOTIFICATION_ID,
                createNotificationBuilder()
                    .setContentIntent(createPendingIntent())
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
     * Create a Notification Builder with Defaults
     *
     * This notification builder is used to communicate to the user when the peer connection state changes.
     * It also relays transaction messages that need to be signed to the AnswerActivity
     */
    fun createNotificationBuilder(
        contentText: String? = "Tap to open the app.",
        contentTitle: String? = ContextCompat.getString(this@LiquidWebRTCService, R.string.app_name),
        channelId: String = LIQUID_NOTIFICATION_CHANNEL_ID
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(this@LiquidWebRTCService, channelId)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setOnlyAlertOnce(true)
            .setColor(ContextCompat.getColor(this@LiquidWebRTCService, R.color.md_theme_primary))
            .setSmallIcon(R.drawable.baseline_account_balance_wallet_24)
    }
    /**
     * Create a PendingIntent
     *
     * This PendingIntent is used to open the SignTransactionActivity when a transaction message is received
     */
    fun createPendingIntent(msg: String? = null): PendingIntent {
        val answerIntent = Intent(this@LiquidWebRTCService, AnswerActivity::class.java)
        answerIntent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        msg?.let {
            answerIntent.putExtra("msg", it)
        }
        return TaskStackBuilder.create(this@LiquidWebRTCService).run {
            addNextIntentWithParentStack(answerIntent)
            getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    /**
     * Notify the User
     */
    fun notify(
        notificationBuilder: NotificationCompat.Builder,
        notificationId: Int = LIQUID_NOTIFICATION_ID
    ) {
        if (notificationId != 1337) {
            nextNotificationId = notificationId + 1
        }
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
    fun start(url: String, httpClient: OkHttpClient) {
        val isInitialized = signalClient != null
        if (isInitialized) {
            signalClient?.disconnect()
        }
        signalClient = SignalClient(url, this@LiquidWebRTCService, httpClient)
        notify(createNotificationBuilder("Connecting to $url"))
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
     */
    suspend fun peer(requestId: Double, type: String) {
        dataChannel = signalClient?.peer(requestId, type, iceServers)
        peerClient = signalClient?.peerClient
        peerConnection = peerClient?.peerConnection
        notify(createNotificationBuilder("Connected to ${signalClient?.url}"))
    }

    /**
     * Handle Messages and State Changes
     *
     * When the activity is visible, it will call back to the onMessage function.
     * Otherwise, it will create a notification with a PendingIntent for the AnswerActivity
     */
    fun handleMessages(
        activity: Activity,
        onMessage: (msg: String) -> Unit,
        onStateChange: ((state: String?) -> Unit)? = null
    ) {
        // If the Data Channel is available, handle messages
        dataChannel?.let {
            var peerInfo = "Connecting..."
            peerConnection!!.getStats { stats ->
                stats.statsMap.forEach { (_, value) ->
                    val state = value.members["state"] as String?
                    val candidateId = value.members["remoteCandidateId"] as String?
                    if(value.type == "candidate-pair" && state != null){
                        Log.d(TAG, "State: $value")
                        if(state != "waiting" && state != "failed") {
                            peerInfo = state

                            notify(
                                createNotificationBuilder(
                                    "Peer(${
                                        candidateId?.replace(
                                            "RTCIceCandidate_",
                                            ""
                                        )
                                    }): $peerInfo"
                                )
                            )
                        }
                    }
                }
            }
            notify(createNotificationBuilder("Peer: $peerInfo"))

            // Handle Data Channel Messages
            signalClient?.handleDataChannel(it, { msg ->
                if(peerInfo != "succeeded"){
                    peerConnection!!.getStats { stats ->
                        stats.statsMap.forEach { (_, value) ->
                            val status = value.members["state"] as String?
                            val candidateId = value.members["remoteCandidateId"] as String?
                            if(value.type == "candidate-pair" && status != null){
                                if(status != "waiting" && status != "failed"){
                                    peerInfo = status.toString()
                                    notify(
                                        createNotificationBuilder(
                                            "Peer(${
                                                candidateId?.replace(
                                                    "RTCIceCandidate_",
                                                    ""
                                                )
                                            }): $peerInfo"
                                        )
                                    )
                                }
                            }
                        }
                    }
                    notify(createNotificationBuilder("Peer: $peerInfo"))
                }
                if(activity.hasWindowFocus()){
                    onMessage(msg)
                    return@handleDataChannel
                }
                Log.d(TAG, "DataChannel Message: $msg")
                notify(
                    createNotificationBuilder(msg, "Data Channel Message", LIQUID_PEER_NOTIFICATION_CHANNEL_ID)
                        .setAutoCancel(true)
                        .setContentIntent(createPendingIntent(msg))
                        .setOnlyAlertOnce(false),
                    nextNotificationId
                )
            }, { state ->
                onStateChange?.invoke(state)
                if(state == DataChannel.State.CLOSED.toString() || state == DataChannel.State.CLOSING.toString()){
                    notify(createNotificationBuilder("Tap to open the app.").setContentIntent(createPendingIntent()))
                }
            })
        }
    }
    fun updateLastKnownReferer(referer: String?) {
        lastKnownReferer = referer
    }
    /**
     * Send a Message
     */
    fun send(msg: String) {
        Log.d(TAG, "Sending: $msg from $lastKnownReferer")
        if (dataChannel != null) {
            peerClient?.send(msg)
        }
    }
}
