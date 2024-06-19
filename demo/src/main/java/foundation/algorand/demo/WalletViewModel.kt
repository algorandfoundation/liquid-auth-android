package foundation.algorand.demo

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.algorand.algosdk.account.Account
import com.algorand.algosdk.transaction.Transaction
import com.algorand.algosdk.util.Encoder
import com.algorand.algosdk.v2.client.common.AlgodClient

class WalletViewModel: ViewModel() {
    companion object {
        const val TAG = "WalletViewModel"
    }
    // Main Account
    private val _account = MutableLiveData<Account?>().apply {
        value = null
    }
    val account: LiveData<Account?> = _account
    fun setAccount(account: Account){
        Log.d(TAG, "setAccount: ${account.address}")
        _account.value = account
    }

    // Selected Account, defaults to the main account
    private val _selected = MutableLiveData<Account?>().apply {
        value = null
    }
    val selected: LiveData<Account?> = _selected
    fun setSelected(account: Account){
        Log.d(TAG, "setSelected: ${account.address}")
        _selected.value = account
    }

    // Rekey Account
    private val _rekey = MutableLiveData<Account?>().apply {
        value = null
    }
    val rekey: LiveData<Account?> = _rekey
    fun setRekey(account: Account){
        Log.d(TAG, "setRekey: ${account.address}")
        _rekey.value = account
    }

    // Alogrand Rekey
    val algod = AlgodClient("https://testnet-api.algonode.cloud", 443, "")
    fun rekey(sender: Account, rekey: Account, signer: Account? = null){
        val tx1: Transaction = Transaction.PaymentTransactionBuilder()
            .lookupParams(algod) // lookup fee, firstValid, lastValid
            .sender(sender.address)
            .receiver(sender.address)
            .rekey(rekey.address)
            .amount(0)
            .build()
        val signedTxn = signer?.signTransaction(tx1) ?: sender.signTransaction(tx1)
        val signedTxnBytes = Encoder.encodeToMsgPack(signedTxn)
        val post = algod.RawTransaction().rawtxn(signedTxnBytes).execute()
        if (!post.isSuccessful) {
            throw Exception("Failed to post transaction")
        }
        setSelected(rekey)
    }
}
