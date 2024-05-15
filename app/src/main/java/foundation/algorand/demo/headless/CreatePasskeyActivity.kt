package foundation.algorand.demo.headless

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.provider.PendingIntentHandler
import foundation.algorand.demo.R
import foundation.algorand.demo.databinding.ActivityCreatePasskeyBinding


/**
 * Authentication Provider Activity
 *
 * Called by {@link foundation.algorand.demo.services.LiquidCredentialProviderService }
 *
 * @see <a href="https://developer.android.com/training/sign-in/credential-provider#handle-passkey-credential">Handle Passkey Credential</a>
 * @see <a href="https://developer.android.com/training/sign-in/credential-provider#passkeys-implement">Implement Passkey Credential</a>
 */
class CreatePasskeyActivity : AppCompatActivity() {
    private val viewModel: CreatePasskeyViewModel by viewModels()
    private lateinit var binding: ActivityCreatePasskeyBinding
    companion object {
        const val TAG = "CreatePasskeyActivity"
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate($intent)")

        // View Bindings
        binding = ActivityCreatePasskeyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.viewModel = viewModel


        // Handle Intent
        val request = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
        if (request != null && request.callingRequest is CreatePublicKeyCredentialRequest) {
            val result = viewModel.processCreatePasskey(this@CreatePasskeyActivity, request)
            Log.d(GetPasskeyActivity.TAG, "result: $result")
            setResult(Activity.RESULT_OK, result)
            finish()
        } else {
            binding.createPasskeyMessage.text = resources.getString(R.string.get_passkey_error)
        }
    }
}
