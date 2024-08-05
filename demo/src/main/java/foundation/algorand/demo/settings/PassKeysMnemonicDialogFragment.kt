package foundation.algorand.demo.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import foundation.algorand.demo.credential.CredentialRepository
import foundation.algorand.demo.databinding.FragmentPassKeysMnemonicDialogBinding
// import cash.z.ecc.android.bip39.Mnemonics

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
    binding.generateNewMnemonicButton.setOnClickListener { generateNewMnemonic() }
    binding.storeButton.setOnClickListener { storeMnemonic(context) }
  }

  private fun generateNewMnemonic() {
    val mnemonic = "salon zoo engage submit smile frost later decide wing sight chaos renew lizard rely canal coral scene hobby scare step bus leaf tobacco slice"
    // val mnemonic = Mnemonics.MnemonicCode(Mnemonics.WordCount.COUNT_24).joinToString(" ")

    binding.mnemonicInputField.setText(mnemonic)
  }

  private fun storeMnemonic(context: Context?) {
    val mnemonic = binding.mnemonicInputField.text.toString().toCharArray()
    // FIXME: Not secure, just saves derivedParentSecret directly to the database
    // The mnemonic is turned into the derivedParentSecret
    context?.let {
      credentialRepository.saveDerivedParentSecret(it, mnemonic)
      dismiss()
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
