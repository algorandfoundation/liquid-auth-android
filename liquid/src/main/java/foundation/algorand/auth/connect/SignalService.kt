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

    // --- Offline message queue -------------------------------------------
    // Buffer for data-channel messages that arrive while the consuming app is
    // offline (its JS listener is not attached / it has been backgrounded or
    // closed). They are replayed to [messageSink] in arrival order once the
    // app comes back online (see [setActive]). This is a generic mechanism:
    // the shared library never inspects message contents — the consumer
    // decides which channels are buffered ([queueChannels]).
    private val messageQueue = ArrayDeque<Pair<String, String>>()
    @Volatile
    private var messageSink: ((label: String, msg: String) -> Unit)? = null
    // Channels whose messages are buffered while offline. `null` means buffer
    // every channel; an empty set means buffer none.
    private var queueChannels: Set<String>? = null
    // App-controlled online flag. `null` = fall back to the activity's window
    // focus (legacy behavior); once the consumer calls [setActive] it takes
    // over so the app — not the library — owns the delivery state.
    private var appActiveOverride: Boolean? = null
    // Cap so a long offline period can't grow the buffer unbounded; oldest
    // messages are dropped first.
    private var maxQueuedMessages: Int = 200

    /**
     * Handle Service Binding
     */
    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    /**
     * Keep the service running (and let the OS restart it if it is killed) so
     * the signaling connection survives the app being backgrounded. The
     * service is stopped only when the consumer explicitly disconnects.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    /**
     * The user removed the app's task (closed the app), but the foreground
     * signaling connection must keep running until the app explicitly
     * disconnects. Deliberately do NOT stop the service here.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "Task removed; keeping the signaling service alive")
        super.onTaskRemoved(rootIntent)
    }

    /**
     * All bound clients have detached (e.g. the app was closed). Drop the stale
     * message sink so buffered messages are never replayed to a dead listener;
     * they stay queued for the next fresh listener that attaches via
     * [handleMessages]. The started foreground service keeps running.
     */
    override fun onUnbind(intent: Intent?): Boolean {
        synchronized(this) {
            messageSink = null
            appActiveOverride = null
        }
        return super.onUnbind(intent)
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
        synchronized(this) {
            messageQueue.clear()
            messageSink = null
            appActiveOverride = null
        }
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
        activityClass: Class<out Activity>,
        presenter: NotificationPresenter? = null,
        queueChannels: Set<String>? = null
    ) {
        var requestCode = 1
        val serviceIntentRequestCode = 0
        // Remember where to deliver (and replay) messages, and which channels
        // to buffer while offline.
        this.messageSink = onMessage
        this.queueChannels = queueChannels
        // A fresh listener just attached (e.g. after a relaunch that reconnected
        // to the still-running service). If the app is already marked online,
        // replay anything buffered while it was gone to this new sink.
        if (appActiveOverride == true) {
            drainQueue()
        }
        // Register observers on every negotiated data channel
        signalClient?.handleDataChannels({ label, msg ->
            if (isAppActive(activity)) {
                // Online: flush anything buffered while offline first so the
                // app's listener sees messages in arrival order, then deliver.
                drainQueue()
                onMessage(label, msg)
                return@handleDataChannels
            }
            Log.d(TAG, "DataChannel[$label] Message (offline): $msg")
            // Offline: buffer deliverable messages so they reach the app's
            // listener once it comes back online.
            if (shouldQueue(label)) {
                enqueue(label, msg)
            }
            // Resolve the notification copy through the (optional) presenter. A
            // presenter fully controls the per-message-type content and may
            // suppress the notification entirely by returning null (e.g. for
            // heartbeat/stream control traffic). With no presenter we keep the
            // legacy behavior of showing the raw message text.
            val content: NotificationContent? =
                if (presenter != null) presenter.present(label, msg)
                else NotificationContent(null, msg)
            if (content == null) {
                return@handleDataChannels
            }
            content.title?.let { notificationBuilder.setContentTitle(it) }
            notify(
                notificationBuilder
                    .setContentText(content.text)
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
     * Set whether the consuming app is currently online (its JS listener is
     * attached / it is foregrounded). The app owns this signal so it controls
     * the signaling delivery state. When set active, any messages buffered
     * while offline are replayed to the message sink in arrival order.
     */
    @Synchronized
    fun setActive(active: Boolean) {
        appActiveOverride = active
        if (active) {
            drainQueue()
        }
    }

    /**
     * Whether messages should be delivered live. Uses the app-controlled
     * [appActiveOverride] when set; otherwise falls back to the activity's
     * window focus (legacy behavior).
     */
    private fun isAppActive(activity: Activity): Boolean {
        return appActiveOverride ?: activity.hasWindowFocus()
    }

    /** Whether an inbound message on [label] should be buffered while offline. */
    private fun shouldQueue(label: String): Boolean {
        val channels = queueChannels ?: return true
        return label in channels
    }

    @Synchronized
    private fun enqueue(label: String, msg: String) {
        while (messageQueue.size >= maxQueuedMessages && messageQueue.isNotEmpty()) {
            messageQueue.removeFirst()
        }
        messageQueue.addLast(label to msg)
    }

    /** Replay (and clear) every buffered message to the current sink, in order. */
    @Synchronized
    private fun drainQueue() {
        val sink = messageSink ?: return
        while (messageQueue.isNotEmpty()) {
            val (label, msg) = messageQueue.first()
            try {
                sink(label, msg)
            } catch (e: Exception) {
                // The sink is stale/dead (e.g. the app process was killed);
                // stop draining and keep the remaining messages buffered for
                // the next fresh listener.
                Log.w(TAG, "Stopped draining message queue; sink threw", e)
                return
            }
            messageQueue.removeFirst()
        }
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

/**
 * Content for a per-message notification produced by a [NotificationPresenter].
 */
data class NotificationContent(
    val title: String?,
    val text: String?
)

/**
 * Generic seam that decides how (or whether) to present a notification for an
 * inbound data-channel message while the app is backgrounded. Returning `null`
 * suppresses the notification. Consumers (the RN module / wallet) fill this in
 * with their own per-message-type copy, keeping message semantics out of the
 * shared signaling library.
 */
fun interface NotificationPresenter {
    fun present(label: String, message: String): NotificationContent?
}
