package foundation.algorand.demo

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.algorand.algosdk.account.Account
import com.algorand.algosdk.transaction.Transaction
import com.algorand.algosdk.util.Encoder
import com.algorand.algosdk.v2.client.common.AlgodClient
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import foundation.algorand.auth.connect.AuthMessage
import foundation.algorand.demo.credential.CredentialRepository
import foundation.algorand.demo.credential.db.Credential

/**
 * Demo View Model
 *
 * Minimal state to handle FIDO2 PublicKeyCredentials and Proof of Knowledge
 */
class AnswerViewModel: ViewModel() {
    private val credentialRepository = CredentialRepository()
    // Main Account
    private val _account = MutableLiveData<Account>().apply {
        value = Account()
    }
    val account: LiveData<Account> = _account
    // Selected Account, defaults to the main account
    private val _selected = MutableLiveData<Account>().apply {
        value = _account.value
    }
    val selected: LiveData<Account> = _selected
    fun setSelected(account: Account){
        _selected.value = account
    }
    // Rekey Account
    private val _rekey = MutableLiveData<Account>().apply {
        value = Account()
    }
    val rekey: LiveData<Account> = _rekey
    private val _session = MutableLiveData<String>().apply {
        value = "Logged Out"
    }
    val session: LiveData<String> = _session

    fun setSession(cookie: String?){
        if(cookie !== null){
            _session.value = cookie
        }
    }

    private val _message = MutableLiveData<AuthMessage?>().apply {
        value = null
    }

    val message: LiveData<AuthMessage?> = _message

    fun setMessage(msg: AuthMessage?){
        _message.value = msg
    }


    private val _credential = MutableLiveData<PublicKeyCredential?>().apply {
        value = null
    }

    val credential: LiveData<PublicKeyCredential?> = _credential

    fun setCredential(cred: PublicKeyCredential?){
        _credential.value = cred
    }

    private val _count = MutableLiveData<Int>().apply {
        value = 0
    }

    val count = _count

    fun setCount(i: Int){
        _count.value = i
    }
    suspend fun saveCredential(context: Context, origin: String, credential: PublicKeyCredential){
        credentialRepository.saveCredential(
            context,
            Credential(
                credentialId = credential.id!!,
                userHandle = account.value!!.address.toString(),
                origin = origin,
                publicKey = "",
                privateKey = "",
                count = 0,
            )
        )
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
