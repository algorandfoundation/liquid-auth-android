package foundation.algorand.demo.settings

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import foundation.algorand.demo.R


/**
 * A simple [DialogFragment] subclass.
 */
class SettingsDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder
                .setMessage("Device is not secure, please enter a PIN or Fingerprint to continue.")
                .setIcon(R.drawable.baseline_warning_24)
                .setTitle("Warning")
                .setPositiveButton("Settings") { dialog, id ->
                    startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
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
