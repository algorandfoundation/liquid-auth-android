package foundation.algorand.demo.settings

import android.app.Dialog
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import foundation.algorand.demo.R


/**
 * A simple [DialogFragment] subclass.
 */
class NotificationsDialogFragment(private val packageName: String) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder
                .setMessage("Notifications must be enabled to receive alerts.")
                .setIcon(R.drawable.baseline_warning_24)
                .setTitle("Warning")
                .setPositiveButton("Settings") { dialog, id ->
                    if(VERSION.SDK_INT >= VERSION_CODES.O){
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, "LIQUID")
                        startActivity(intent)
                    }
                }
                .setNegativeButton("Exit") { dialog, id ->
                    it.finish()
                }
                .setCancelable(false)
            // Create the AlertDialog object and return it.
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
