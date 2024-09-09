package foundation.algorand.demo.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import foundation.algorand.demo.credential.CredentialRepository
import foundation.algorand.demo.databinding.FragmentPassKeysMnemonicDialogBinding
import foundation.algorand.deterministicP256.DeterministicP256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PassKeysMnemonicDialogFragment : DialogFragment() {
  companion object {
    const val TAG = "PassKeysMnemonicFragment"
  }
  private val credentialRepository = CredentialRepository()
  private var _binding: FragmentPassKeysMnemonicDialogBinding? = null
  private val binding
    get() = _binding!!

  override fun onCreateView(
          inflater: LayoutInflater,
          container: ViewGroup?,
          savedInstanceState: Bundle?
  ): View {
    _binding = FragmentPassKeysMnemonicDialogBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.generateNewMnemonicButton.setOnClickListener {
      viewLifecycleOwner.lifecycleScope.launch { generateNewMnemonic() }
    }
    binding.storeButton.setOnClickListener {
      viewLifecycleOwner.lifecycleScope.launch { storeMnemonic(context) }
    }
  }

  private suspend fun generateNewMnemonic() {
    val mnemonic =
            withContext(Dispatchers.IO) {
              // Generate the mnemonic on a background thread
              "salon zoo engage submit smile frost later decide wing sight chaos renew lizard rely canal coral scene hobby scare step bus leaf tobacco slice"
              // val mnemonic = Mnemonics.MnemonicCode(Mnemonics.WordCount.COUNT_24).joinToString("
              // ")
            }

    val publicKey =
            withContext(Dispatchers.IO) {
              // Generate the public key on a background thread
              DeterministicP256()
                      .genDomainSpecificKeypair(
                              DeterministicP256().genDerivedMainKeyWithBIP39(mnemonic),
                              "https://google.com",
                              "123"
                      )
                      .public
                      .encoded
                      .contentToString()
            }

    withContext(Dispatchers.Main) {
      // Update the UI on the main thread
      binding.mnemonicInputField.setText(mnemonic)
      binding.demonstrativeField.setText("Public Key: $publicKey")
    }
  }

  private suspend fun storeMnemonic(context: Context?) {
    withContext(Dispatchers.IO) {
      val mnemonic = binding.mnemonicInputField.text.toString().toCharArray()
      // FIXME: Not secure, just saves derivedParentSecret directly to the database
      // The mnemonic is turned into the derivedParentSecret
      context?.let {
        credentialRepository.saveDerivedParentSecret(it, mnemonic)
        withContext(Dispatchers.Main) { dismiss() }
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
