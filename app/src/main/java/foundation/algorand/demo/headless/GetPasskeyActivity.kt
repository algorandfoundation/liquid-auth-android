package foundation.algorand.demo.headless

import android.app.Activity
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.credentials.provider.PendingIntentHandler
import androidx.lifecycle.lifecycleScope
import foundation.algorand.demo.R
import foundation.algorand.demo.databinding.ActivityGetPasskeyBinding
import kotlinx.coroutines.launch

class GetPasskeyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGetPasskeyBinding
    val viewModel: GetPasskeyViewModel by viewModels()
    companion object {
        const val TAG = "GetPasskeyActivity"
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate($intent)")

        // Initialize Layout
        binding = ActivityGetPasskeyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.viewModel = viewModel

        // Handle Intent
        val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        if (request != null) {
            lifecycleScope.launch {
                val result = viewModel.processGetPasskey(this@GetPasskeyActivity, request, intent.getBundleExtra("CREDENTIAL_DATA"))
                Log.d(TAG, "result: $result")
                setResult(Activity.RESULT_OK, result)
                finish()
            }
        } else {
            binding.getPasskeyMessage.text = resources.getString(R.string.get_passkey_error)
        }
    }
}
