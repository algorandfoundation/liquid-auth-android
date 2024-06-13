package foundation.algorand.demo

import android.content.Context
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.algorand.algosdk.account.Account
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import foundation.algorand.auth.connect.AuthMessage
import foundation.algorand.demo.credential.CredentialRepository
import foundation.algorand.demo.credential.db.Credential
import org.json.JSONObject

/**
 * Demo View Model
 *
 * Minimal state to handle FIDO2 PublicKeyCredentials and Proof of Knowledge
 */
class AnswerViewModel: ViewModel() {
    private val credentialRepository = CredentialRepository()
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

    private val _count = MutableLiveData<Int>().apply {
        value = 0
    }

    val count = _count

    fun setCount(i: Int){
        _count.value = i
    }


    suspend fun saveCredential(context: Context, account: Account, credential: PublicKeyCredential){
        credentialRepository.saveCredential(
            context,
            Credential(
                credentialId = credential.id!!,
                userHandle = account.address.toString(),
                userId = account.address.toString(),
                origin = message.value!!.origin,
                publicKey = "",
                privateKey = "",
                count = 0,
            )
        )
    }

    fun getCredentialMessage(account: Account, credential: PublicKeyCredential): JSONObject {
        val credMessage = JSONObject()
        credMessage.put("address", account.address.toString())
        credMessage.put("device", Build.MODEL)
        credMessage.put("origin", message.value!!.origin)
        credMessage.put("id", credential.id)
        credMessage.put("prevCounter", count.value!!)
        credMessage.put("type", "credential")
        return credMessage
    }
}
