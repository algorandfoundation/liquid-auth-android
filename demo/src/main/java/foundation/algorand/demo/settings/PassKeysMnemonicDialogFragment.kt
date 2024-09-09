package foundation.algorand.demo.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.bip39.Mnemonics
import foundation.algorand.demo.credential.CredentialRepository
import foundation.algorand.demo.databinding.FragmentPassKeysMnemonicDialogBinding
import foundation.algorand.demo.derivedSecret.DerivedSecretRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PassKeysMnemonicDialogFragment : DialogFragment() {
  companion object {
    const val TAG = "PassKeysMnemonicFragment"
  }

  private val derivedSecretRepository = DerivedSecretRepository()
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
              Mnemonics.MnemonicCode(Mnemonics.WordCount.COUNT_24).joinToString(" ")
            }

    withContext(Dispatchers.Main) {
      // Update the UI on the main thread
      binding.mnemonicInputField.setText(mnemonic)
    }
  }

  private suspend fun storeMnemonic(context: Context?) {
    withContext(Dispatchers.Main) { binding.progressBar.visibility = View.VISIBLE }

    withContext(Dispatchers.IO) {
      val mnemonic = binding.mnemonicInputField.text.toString().toCharArray()
      // FIXME: Not secure, just saves derivedParentSecret directly to the database
      // The mnemonic is turned into the derivedParentSecret
      context?.let {
        derivedSecretRepository.saveDerivedParentSecret(it, mnemonic)
        withContext(Dispatchers.Main) {
          dismiss()
          binding.progressBar.visibility = View.GONE
        }
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
