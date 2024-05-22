package foundation.algorand.demo.headless

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import foundation.algorand.demo.R

/**
 * Placeholder for Begin action
 * @todo: Investigate the necessity of this intent activity
 */
class BeginGetPasskeyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_begin_get_passkey)
        // TODO: Handle Begin Request?
        //val beginRequest =
        //    PendingIntentHandler.retrieveBeginGetCredentialRequest(intent)
        //Log.d(TAG, beginRequest.toString())
    }
}
