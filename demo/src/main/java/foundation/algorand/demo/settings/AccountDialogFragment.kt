package foundation.algorand.demo.settings

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.algorand.algosdk.account.Account
import com.algorand.algosdk.v2.client.common.AlgodClient
import foundation.algorand.demo.AnswerViewModel
import foundation.algorand.demo.databinding.FragmentAccountDialogBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * A simple [DialogFragment] subclass.
 */
class AccountDialogFragment constructor(private val account: Account, private val rekey: Account, private val selected: Account) : DialogFragment() {
    private val viewModel: AnswerViewModel by activityViewModels()
    private lateinit var binding: FragmentAccountDialogBinding
    private val algod = AlgodClient("https://testnet-api.algonode.cloud", 443, "")
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
        viewModel.selected.observe(viewLifecycleOwner) {
            binding.selected = it
        }
        return binding.root
    }
    override fun onResume() {
        Log.d("AccountDialogFragment", "onResume")
        lifecycleScope.launch {
            binding.balance = algod.AccountInformation(account.address).execute().body().amount.toString()
            if (binding.balance == "0") {
                delay(5000)
                binding.balance = algod.AccountInformation(account.address).execute().body().amount.toString()
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
