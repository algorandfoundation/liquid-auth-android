package foundation.algorand.demo.settings

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.bip39.Mnemonics
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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = false // Make the dialog non-cancelable
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = super.onCreateDialog(savedInstanceState)
    dialog.setCancelable(false) // Make the dialog non-cancelable
    return dialog
  }

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
    binding.storeButton.isEnabled = false // Initially disable the store button
    setupMnemonicInputField()
  }

  private fun setupMnemonicInputField() {
    binding.mnemonicInputField.addTextChangedListener(
            object : TextWatcher {
              override fun afterTextChanged(s: Editable?) {
                val words = s.toString()
                val mnemonic = Mnemonics.MnemonicCode(words)
                var isValid = false
                try {
                  mnemonic.validate()
                  if (mnemonic.words.size == 24) {
                    isValid = true
                  }
                } catch (e: Exception) {
                  isValid = false
                }
                if (isValid) {
                  // The mnemonic is valid
                  binding.mnemonicInputField.backgroundTintList =
                          ColorStateList.valueOf(Color.GREEN)
                  binding.storeButton.isEnabled = true
                } else {
                  // The mnemonic is invalid
                  binding.mnemonicInputField.backgroundTintList = ColorStateList.valueOf(Color.RED)
                  binding.storeButton.isEnabled = false
                }
              }

              override fun beforeTextChanged(
                      s: CharSequence?,
                      start: Int,
                      count: Int,
                      after: Int
              ) {}

              override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
    )
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
    withContext(Dispatchers.Main) {
      binding.storeButton.visibility = View.GONE
      binding.generateNewMnemonicButton.isEnabled = false // Disable the generate button
      binding.mnemonicInputField.isEnabled = false // Disable the input field
      binding.progressBar.visibility = View.VISIBLE
    }

    withContext(Dispatchers.IO) {
      val mnemonic = binding.mnemonicInputField.text.toString().toCharArray()
      context?.let {
        derivedSecretRepository.saveDerivedParentSecret(it, mnemonic)
        withContext(Dispatchers.Main) {
          binding.progressBar.visibility = View.GONE
          dismiss() // Dismiss the dialog programmatically
        }
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
