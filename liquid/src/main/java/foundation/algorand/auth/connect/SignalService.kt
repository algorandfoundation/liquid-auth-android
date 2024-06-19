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
import org.webrtc.DataChannel
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
     */
    suspend fun peer(
        requestId: Double,
        type: String,
        iceServers: List<PeerConnection.IceServer>,
    ) {
        dataChannel = signalClient?.peer(requestId, type, iceServers)
        peerClient = signalClient?.peerClient
        peerConnection = peerClient?.peerConnection
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
        onMessage: (msg: String) -> Unit,
        onStateChange: ((state: String?) -> Unit)? = null,
        notificationBuilder: Builder,
        notificationId: Int = LIQUID_NOTIFICATION_ID,
        activityClass: Class<out Activity>
    ) {
        var requestCode = 1
        var serviceIntentRequestCode = 0
        // If the Data Channel is available, handle messages
        dataChannel?.let {
            // Handle Data Channel Messages
            signalClient?.handleDataChannel(it, { msg ->
                if (activity.hasWindowFocus()) {
                    onMessage(msg)
                    return@handleDataChannel
                }
                Log.d(TAG, "DataChannel Message: $msg")
                notify(
                    notificationBuilder
                        .setContentText(msg)
                        .setContentIntent(createPendingIntent(activityClass, requestCode, msg)),
                    notificationId
                )
                requestCode += 1
            }, { state ->
                if (state == "CLOSED" || state == "CLOSING") {
                    notify(
                        notificationBuilder
                            .setContentText("Tap to open the app.")
                            .setOnlyAlertOnce(true)
                            .setContentIntent(createPendingIntent(activityClass, serviceIntentRequestCode,null))
                    , notificationId
                    )
                }
                onStateChange?.invoke(state)
            })
        }
    }

    fun updateLastKnownReferer(referer: String?) {
        lastKnownReferer = referer
    }

    fun updateDeepLinkFlag(isDeepLink: Boolean) {
        this.isDeepLink = isDeepLink
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
