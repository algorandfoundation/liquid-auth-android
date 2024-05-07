package foundation.algorand.demo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import foundation.algorand.auth.connect.AuthMessage

class OfferViewModel: ViewModel() {
    private val _message = MutableLiveData<AuthMessage?>().apply {
        value = null
    }

    val message: LiveData<AuthMessage?> = _message

    fun setMessage(msg: AuthMessage?){
        _message.value = msg
    }

    private val _address = MutableLiveData<String?>().apply {
        value = null
    }
    val address: LiveData<String?> = _address
    fun setAddress(addr: String?){
        _address.postValue(addr)
    }
}
