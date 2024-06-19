package foundation.algorand.demo

import android.app.*
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat.Builder
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel

class NotificationViewModel: ViewModel() {
    companion object {
        const val TAG = "NotificationViewModel"
        const val NOTIFICATION_CHANNEL_ID =  "NOTIFICATION_CHANNEL"
        const val SERVICE_NOTIFICATION_ID = 1000
    }
    /**
     * Create a Notification Channel
     *
     * This notification channel is used to group notifications from the Liquid WebRTC Service
     */
    fun createChannels(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "WebRTC Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
    /**
     * Create a Notification Builder with Defaults
     *
     * This notification builder is used to communicate to the user when the peer connection state changes.
     * It also relays transaction messages that need to be signed to the AnswerActivity
     */
    fun createNotificationBuilder(
        context: Context,
        contentText: String = "Tap to open the app.",
        contentTitle: String = "Liquid Auth",
        channelId: String = NOTIFICATION_CHANNEL_ID,

        ): Builder {
        return Builder(context, channelId)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setSmallIcon(R.drawable.baseline_account_balance_wallet_24)
    }
}
