package foundation.algorand.demo.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import foundation.algorand.demo.R
import foundation.algorand.demo.credential.CredentialRepository
import foundation.algorand.demo.credential.db.Credential
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ManualAddPassKeysDialogFragment : DialogFragment() {

    private val credentialRepository = CredentialRepository()

    companion object {
        const val TAG = "ManualAddPassKeysDialogFragment"
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_manual_add_pass_keys_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val originInput = view.findViewById<EditText>(R.id.originInput)
        val userHandleInput = view.findViewById<EditText>(R.id.userHandleInput)
        val recreateCredentialButton = view.findViewById<Button>(R.id.recreateCredentialButton)

        recreateCredentialButton.setOnClickListener {
            val origin = originInput.text.toString()
            val userHandle = userHandleInput.text.toString()
            if (validateInputs(origin, userHandle)) {
                // Launch a coroutine to call the suspend function
                CoroutineScope(Dispatchers.Main).launch {
                    createOrRecreateCredential(origin, userHandle)
                    dismiss() // Close the dialog
                }
            } else {
                if (origin.isEmpty()) {
                    originInput.error = "Origin is required"
                }
                if (userHandle.isEmpty()) {
                    userHandleInput.error = "User Handle is required"
                }
            }
        }
    }

    private fun validateInputs(origin: String, userHandle: String): Boolean {
        // Add your validation logic here
        return origin.isNotEmpty() && userHandle.isNotEmpty()
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun createOrRecreateCredential(origin: String, userHandle: String) {
        // Generate a key pair
        val keyPair =
                credentialRepository.createDeterministicKeyPair(
                        requireContext(),
                        origin,
                        userHandle
                )

        // Deterministically generate a credentialId
        val credentialId = credentialRepository.generateCredentialId(keyPair)

        // Check that the credential does not already exist, and if so, create it
        if (credentialRepository.getCredential(requireContext(), credentialId) == null) {
            // Credential already exists, delete it
            // Save passkey in the database
            credentialRepository.saveCredential(
                    requireContext(),
                    Credential(
                            credentialId = Base64.encode(credentialId),
                            userHandle = userHandle,
                            userId = "",
                            origin = origin,
                            publicKey = Base64.encode(keyPair.public.encoded),
                            privateKey = Base64.encode(keyPair.private.encoded),
                            count = 0,
                    )
            )
        }
    }
}
