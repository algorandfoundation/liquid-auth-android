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

    // The `requestId` the live connection is bound to, so a re-attaching app
    // can hydrate which room/peer the background service is connected to. Set
    // by [peer] and cleared by [stop].
    private var connectedRequestId: String? = null

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

    // Optional heartbeat keep-alive. While the consuming app is offline the JS
    // ping/pong reply is dead, so the service itself answers the peer's
    // keepalive `ping` with a `pong` on the configured channel, keeping the
    // peer's liveness watchdog satisfied so it does not tear the connection
    // down while the app is away. The consumer supplies the channel + tokens
    // ([HeartbeatConfig]) so the shared library stays label- and
    // content-agnostic. `null` disables the behavior (legacy).
    private var heartbeat: HeartbeatConfig? = null

    // Persistent-notification state. The single ongoing foreground notification
    // text reflects the service state: connected (app foreground), idle ("tap
    // to open" — app closed with nothing waiting), or messages ("you have new
    // messages" — deliverable messages arrived while closed). The consumer
    // supplies the copy ([NotificationStatus]) so the shared library stays
    // content-agnostic. Captured on [handleMessages]/[attach] so [setActive]
    // and the offline message path can update the SAME notification.
    private var notificationBuilder: Builder? = null
    private var statusNotificationId: Int = LIQUID_NOTIFICATION_ID
    private var notificationActivityClass: Class<out Activity>? = null
    private var notificationStatus: NotificationStatus? = null

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
        // Preserve an already-running client so the app re-attaching (e.g. after
        // a relaunch that reconnected to the still-running foreground service)
        // does NOT tear down the live connection the service was keeping alive.
        // Only build a client when there is none — first start, or after an
        // explicit disconnect()/stop() cleared it. The re-attaching consumer
        // rebinds to the live peer via attach() instead of renegotiating.
        if (signalClient == null) {
            signalClient = SignalClient(url, this@SignalService, httpClient)
        }
    }

    /**
     * Stop the Liquid WebRTC Service
     */
    fun stop() {
        signalClient?.disconnect()
        signalClient = null
        connectedRequestId = null
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
        connectedRequestId = requestId
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
        // Bring the app's EXISTING task to the foreground and deliver to the
        // already-running activity via onNewIntent, instead of clearing the task
        // and launching a fresh instance. The previous TaskStackBuilder approach
        // applied an implicit FLAG_ACTIVITY_CLEAR_TASK, which tore down and
        // recreated the task — resetting the JS runtime (the app "opened fresh",
        // losing state) and, with expo-router, triggering the "linking configured
        // in multiple places" error from two concurrent NavigationContainers.
        // FLAG_ACTIVITY_NEW_TASK + FLAG_ACTIVITY_SINGLE_TOP with the activity's
        // singleTask launchMode resumes the running instance and preserves its
        // state so the consumer can hydrate from the live service.
        val intent = Intent(this@SignalService, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            msg?.let { putExtra("msg", it) }
        }
        return PendingIntent.getActivity(
            this@SignalService,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
        status: NotificationStatus? = null,
        queueChannels: Set<String>? = null,
        heartbeat: HeartbeatConfig? = null
    ) {
        // Remember where to deliver (and replay) messages, and which channels
        // to buffer while offline.
        this.messageSink = onMessage
        this.queueChannels = queueChannels
        this.heartbeat = heartbeat
        // Capture the notification config so [setActive] and the offline message
        // path can update the same persistent notification.
        this.notificationBuilder = notificationBuilder
        this.statusNotificationId = notificationId
        this.notificationActivityClass = activityClass
        this.notificationStatus = status
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
            // Offline keep-alive: the JS ping/pong reply is dead while the app
            // is backgrounded, so answer the peer's heartbeat `ping` with a
            // `pong` natively. This keeps the peer's liveness watchdog happy so
            // it does not close the connection while the app is away. A
            // keepalive frame is neither queued nor surfaced as a notification.
            heartbeat?.let { hb ->
                if (label == hb.channel && msg == hb.ping) {
                    send(label, hb.pong)
                    return@handleDataChannels
                }
            }
            // Offline: buffer deliverable messages so they reach the app's
            // listener once it comes back online.
            if (shouldQueue(label)) {
                enqueue(label, msg)
            }
            // Update the ongoing notification to the "you have new messages"
            // state, unless this channel is suppressed (control traffic such as
            // the stream channel). The consumer owns the copy; with none we keep
            // the legacy behavior of showing the raw message text.
            val suppressed = notificationStatus?.suppressChannels?.contains(label) == true
            if (suppressed) {
                return@handleDataChannels
            }
            showStatus(notificationStatus?.messages ?: NotificationContent(null, msg))
        }, { label, state ->
            if (state == "CLOSED" || state == "CLOSING") {
                // The p2p channel dropped; surface the idle "tap to open" state.
                showStatus(notificationStatus?.idle ?: NotificationContent(null, "Tap to open the app."))
            }
            onStateChange?.invoke(label, state)
        })
    }

    /**
     * Snapshot of the current live connection so a re-attaching app can hydrate
     * its UI (rather than assuming a fresh start). Reports whether a peer
     * connection exists with negotiated channels, its ICE connection state, the
     * `requestId` it is bound to, and each negotiated channel's current state
     * keyed by label. Read-only: this never mutates the connection.
     */
    fun getConnectionState(): Map<String, Any?> {
        val peer = signalClient?.peerClient
        val channels = peer?.dataChannels?.mapValues { it.value.state().toString() } ?: emptyMap()
        return mapOf(
            "connected" to (peer != null && peer.dataChannels.isNotEmpty()),
            "requestId" to connectedRequestId,
            "iceConnectionState" to peer?.peerConnection?.iceConnectionState()?.toString(),
            "channels" to channels
        )
    }

    /**
     * Re-attach a freshly (re)started app to the ALREADY-live connection without
     * renegotiating. Rebinds the socket/peer callbacks to the new sinks (the old
     * ones referenced a now-dead JS runtime), re-registers the data-channel
     * observers (via [handleMessages]), and re-emits each channel's current
     * state plus the peer's ICE connection state so the consumer hydrates
     * immediately — observers only fire on transitions, so a live-but-unchanged
     * channel would otherwise never notify the fresh listener. Used when
     * [getConnectionState] reports a live peer.
     */
    fun attach(
        activity: Activity,
        onMessage: (label: String, msg: String) -> Unit,
        onStateChange: ((label: String, state: String?) -> Unit)? = null,
        notificationBuilder: Builder,
        notificationId: Int = LIQUID_NOTIFICATION_ID,
        activityClass: Class<out Activity>,
        status: NotificationStatus? = null,
        queueChannels: Set<String>? = null,
        heartbeat: HeartbeatConfig? = null,
        onPresence: ((JSONObject) -> Unit)? = null,
        onLinkError: ((JSONObject) -> Unit)? = null,
        onConnectionStateChange: ((String) -> Unit)? = null,
        onTrack: ((MediaStreamTrack) -> Unit)? = null
    ) {
        // Re-attaching means the app is in the foreground and (re)wiring its
        // listeners, so it is online and consuming by definition. Mark it active
        // up front — BEFORE [handleMessages] captures the fresh sink and decides
        // whether to replay the offline queue. This makes the hydrate replay
        // self-sufficient rather than depending on the consumer having already
        // called [setActive] with the right timing: a late/racing onUnbind from
        // the previous binding (torn down on relaunch) can reset
        // [appActiveOverride] to null AFTER the app marked itself online, which
        // would otherwise cause [handleMessages] to skip drainQueue() and strand
        // every message buffered while the app was closed.
        synchronized(this) { appActiveOverride = true }
        // Rebind the live socket/peer callbacks to the new (post-relaunch) sinks.
        signalClient?.onPresence = onPresence
        signalClient?.onLinkError = onLinkError
        signalClient?.onConnectionStateChange = onConnectionStateChange
        signalClient?.peerClient?.onConnectionStateChange = onConnectionStateChange
        signalClient?.peerClient?.onTrack = onTrack
        // Re-register the data-channel observers with the fresh message/state
        // sinks (and re-arm the offline queue / heartbeat / notification config).
        handleMessages(
            activity,
            onMessage,
            onStateChange,
            notificationBuilder,
            notificationId,
            activityClass,
            status,
            queueChannels,
            heartbeat
        )
        // Re-emit the current channel + ICE state so the re-attached consumer
        // hydrates now (the observers above only fire on future transitions).
        signalClient?.peerClient?.dataChannels?.forEach { (label, channel) ->
            onStateChange?.invoke(label, channel.state().toString())
        }
        signalClient?.peerClient?.peerConnection?.iceConnectionState()?.let {
            onConnectionStateChange?.invoke(it.toString())
        }
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
     * the signaling delivery state.
     *
     * Deliberately does NOT replay the offline queue: on a relaunch the app
     * marks itself active BEFORE its fresh consumer has (re)wired the message
     * listeners, so replaying here would deliver the buffered messages to the
     * previous (stale) sink and lose them. The queue is drained when a fresh
     * sink attaches ([handleMessages] via [attach]/connect) or when the
     * consumer explicitly asks for it via [flushQueue] once its listeners are
     * wired (e.g. on a plain background -> foreground transition where the
     * existing listeners are still live).
     */
    @Synchronized
    fun setActive(active: Boolean) {
        appActiveOverride = active
        val status = notificationStatus
        if (active) {
            // Back in the foreground: restore the connected/ongoing notification.
            status?.connected?.let { showStatus(it) }
        } else {
            // App closed/backgrounded: reflect pending-messages vs. idle in the
            // ongoing notification so the user sees "you have new messages" or
            // "tap to open".
            if (status != null) {
                if (messageQueue.isNotEmpty() && status.messages != null) {
                    showStatus(status.messages)
                } else {
                    status.idle?.let { showStatus(it) }
                }
            }
        }
    }

    /**
     * Update the single ongoing foreground notification to reflect [content]
     * (title + text), keeping it ongoing and tapping through to the app. Moves
     * the persistent notification between the connected / idle / new-messages
     * states. No-op until [handleMessages]/[attach] has captured the builder.
     */
    private fun showStatus(content: NotificationContent) {
        val builder = notificationBuilder ?: return
        val activityClass = notificationActivityClass ?: return
        content.title?.let { builder.setContentTitle(it) }
        content.text?.let { builder.setContentText(it) }
        notify(
            builder
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setContentIntent(createPendingIntent(activityClass, 0, null)),
            statusNotificationId
        )
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

    /**
     * Explicitly replay (and clear) every buffered offline message to the
     * current message sink, in arrival order. Called by the consumer once its
     * message listeners are wired and the app is online — the app owns the
     * timing so a replay can never race the listener (re)wiring. No-op when
     * nothing is buffered or no sink is attached (messages stay buffered).
     */
    @Synchronized
    fun flushQueue() {
        drainQueue()
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
 * Title + text for one state of the ongoing foreground notification.
 */
data class NotificationContent(
    val title: String?,
    val text: String?
)

/**
 * Consumer-supplied copy for the single ongoing foreground-service
 * notification, whose text reflects the service state while the app is closed:
 *  - [connected]: app foregrounded / attached (e.g. "Connected …").
 *  - [idle]: app closed with nothing waiting (e.g. "Tap to open the app").
 *  - [messages]: deliverable message(s) arrived while the app was closed
 *    (e.g. "You have new messages").
 *
 * [suppressChannels] lists channel labels whose inbound messages should NOT
 * flip the notification into the [messages] state (control traffic such as the
 * stream channel) — they are still buffered/replayed, just not announced. The
 * consumer owns all copy so the shared signaling library stays
 * content-agnostic; any field left null leaves that state's text unchanged.
 */
data class NotificationStatus(
    val connected: NotificationContent? = null,
    val idle: NotificationContent? = null,
    val messages: NotificationContent? = null,
    val suppressChannels: Set<String> = emptySet()
)

/**
 * Optional heartbeat keep-alive configuration. While the consuming app is
 * offline the [SignalService] replies to an inbound [ping] frame on [channel]
 * with a [pong] on the same channel, so the peer's liveness watchdog stays
 * satisfied and keeps the connection open. Supplied by the consumer so the
 * shared library never hardcodes channel labels or message tokens.
 */
data class HeartbeatConfig(
    val channel: String,
    val ping: String,
    val pong: String
)
