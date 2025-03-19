package co.algorand.liquid.wallet.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo.Builder
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.PendingIntentHandler
import androidx.fragment.app.FragmentActivity
import co.algorand.liquid.wallet.AppDependencies
import co.algorand.liquid.wallet.R
import co.algorand.liquid.wallet.data.CredentialRepository
import co.algorand.liquid.wallet.data.ServiceRepository

/**
 * Activity responsible for coordinating the secure unlock process of the MyVault application.
 * This includes:
 *  * Handling biometric or device credential authentication.
 *  * Processing credential retrieval requests (using the CredentialsRepository).
 *  * Providing an appropriate response to the system after successful authentication.
 */
class UnlockActivity : FragmentActivity() {

    companion object {
        private const val TAG = "Liquid"
    }

    private lateinit var serviceRepo: ServiceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        serviceRepo = ServiceRepository(
            credentialRepository = AppDependencies.credentialsRepository,
            applicationContext
        )

        val request = PendingIntentHandler.retrieveBeginGetCredentialRequest(intent)
        if (request != null) {
            unlock(request)
        }
    }

    /**
     * Initiates the biometric unlock process.
     *
     * @param request The BeginGetCredentialRequest obtained from the intent.
     */
    private fun unlock(request: BeginGetCredentialRequest) {
        val biometricPrompt = BiometricPrompt(
            this,
            mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, getString(R.string.authentication_error, errString))
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.e(TAG, getString(R.string.authentication_failed))
                    finish()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    super.onAuthenticationSucceeded(result)
                    // applocked to false
                    processGetCredentialRequest(request)
                }
            },
        )
        authenticate(biometricPrompt)
    }

    /**
     * Processes the BeginGetCredentialRequest, generating a response and finishing the activity.
     *
     * @param request The BeginGetCredentialRequest to process.
     */
    private fun processGetCredentialRequest(request: BeginGetCredentialRequest) {
        val authenticationResultIntent = Intent()

        val responseBuilder = BeginGetCredentialResponse.Builder()

        if (serviceRepo.processGetCredentialsRequest(request, responseBuilder)) {
            PendingIntentHandler.setBeginGetCredentialResponse(
                authenticationResultIntent,
                responseBuilder.build(),
            )
        }
        setResult(RESULT_OK, authenticationResultIntent)
        finish()
    }

    /**
     * Configures and displays the biometric authentication prompt.
     *
     * @param biometricPrompt The BiometricPrompt instance used for authentication.
     */
    private fun authenticate(biometricPrompt: BiometricPrompt) {
        val promptInfo = Builder()
            .setTitle(getString(R.string.unlock_app))
            .setSubtitle(getString(R.string.unlock_app_to_access_credentials))
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }
}
