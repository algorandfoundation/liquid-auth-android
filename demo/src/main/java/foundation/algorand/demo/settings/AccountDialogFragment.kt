package foundation.algorand.demo.settings

import android.app.Dialog
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.algorand.algosdk.account.Account
import foundation.algorand.demo.WalletViewModel
import foundation.algorand.demo.databinding.FragmentAccountDialogBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * A simple [DialogFragment] subclass.
 */
class AccountDialogFragment(private val account: Account, private val rekey: Account, private val selected: Account) : DialogFragment() {
    companion object {
        const val TAG = "AccountDialogFragment"
    }

    private val wallet: WalletViewModel by activityViewModels()
    private lateinit var binding: FragmentAccountDialogBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccountDialogBinding.inflate(inflater, container, false)
        binding.account = account
        binding.rekey = rekey
        binding.selected = selected
        binding.balance = "loading"
        wallet.selected.observe(viewLifecycleOwner) {
            binding.selected = it
        }
        return binding.root
    }
    override fun onResume() {
        lifecycleScope.launch {
            binding.balance = wallet.algod.AccountInformation(account.address).execute().body().amount.toString()
            if (binding.balance == "0") {
                delay(5000)
                binding.balance = wallet.algod.AccountInformation(account.address).execute().body().amount.toString()
            }
        }
        super.onResume()
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }
}
