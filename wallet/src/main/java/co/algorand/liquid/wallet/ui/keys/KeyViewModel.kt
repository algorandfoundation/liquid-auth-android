package co.algorand.liquid.wallet.ui.keys

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.Mnemonics.MnemonicCode
import co.algorand.liquid.wallet.AppDependencies
import co.algorand.liquid.wallet.crypto.MnemonicManager
import co.algorand.liquid.wallet.data.KeysRepository
import co.algorand.liquid.wallet.data.model.Passkey
import co.algorand.liquid.wallet.data.model.PasskeyMetadata
import co.algorand.liquid.wallet.encoding.b64Encode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey


const val EXCEPTION_EMPTY_MNEMONIC = "Please specify a mnemonic phrase"

class KeyViewModel(
    private val keysRepository: KeysRepository,
    private val keyManager: MnemonicManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(KeyUiState())

    val uiState: StateFlow<KeyUiState> = _uiState.asStateFlow()

    fun onUndo(){
        if(_uiState.value.mnemonic !== null){
            val currentMnemonic = _uiState.value.mnemonic!!.joinToString(" ")
            if(currentMnemonic !== _uiState.value.text){
                Log.d(TAG, "onUndo() - ${_uiState.value.text}")
                _uiState.value = _uiState.value.copy(text =currentMnemonic, isDirty = false, error = null)
            }
        }
    }

    fun onChange(value: String){
        Log.d(TAG, "onChange(${value})")
        try {
            val nextMnemonic = MnemonicCode(value)
            nextMnemonic.validate()
            if (nextMnemonic.words.size == 24) {
                _uiState.value = _uiState.value.copy(
                    text = nextMnemonic.joinToString(" "),
                    isDirty = _uiState.value.mnemonic?.joinToString(" ") != nextMnemonic.joinToString(" "),
                    error = null,
                )
                return
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error=e)
        }
        _uiState.value = _uiState.value.copy(text=value, isDirty = true)
    }

    fun onSave(){
        Log.d(TAG, "onSave(${_uiState.value.text})")
        val nextMnemonic = MnemonicCode(_uiState.value.text)
        nextMnemonic.validate()
        keyManager.save(nextMnemonic)
        _uiState.value = _uiState.value.copy(mnemonic = nextMnemonic, isDirty = false, error = null, isSaved = true)
    }

    fun onGenerate() {
        val nextMnemonic = MnemonicCode(Mnemonics.WordCount.COUNT_24)
        Log.d(TAG, "onGenerate(): ${nextMnemonic.joinToString(" ")}")
        _uiState.value = _uiState.value.copy(text = nextMnemonic.joinToString(" "), isDirty = true, error=null)
    }

    fun onCancelDelete(){
        Log.d(TAG, "onCancel()")
        _uiState.value = _uiState.value.copy(showConfirm = false)
    }
    fun onConfirmDelete(){
        Log.d(TAG, "onConfirmation()")
        runBlocking {
            keysRepository.clear()
            _uiState.value = KeyUiState(error = Exception(EXCEPTION_EMPTY_MNEMONIC), showConfirm = false)
        }
    }
    fun onDelete() {
        Log.d(TAG, "onDelete()")
        _uiState.value = _uiState.value.copy(showConfirm = true)
    }

    fun onRecoverPasskey(origin: String, userHandle: String){
        Log.d(TAG, "onRecoverPasskey(${origin}, ${userHandle})")
        runBlocking {
            val keyPair = AppDependencies.xHDKeyManager.generatePasskey(origin, userHandle.lowercase())
            keysRepository.addNewPasskey(
                PasskeyMetadata(
                    uid = "",
                    rpid = origin,
                    username = userHandle,
                    displayName = userHandle,
                    credId = b64Encode(AppDependencies.xHDKeyManager.generateCredentialId(keyPair)),
                    credPublicKey = b64Encode((keyPair.public as ECPublicKey).encoded),
                    credPrivateKey = b64Encode((keyPair.private as ECPrivateKey).s.toByteArray()),
                ),
            )
        }

    }
    fun onCancelRecovery(){
        _uiState.value = _uiState.value.copy(showRecovery = false)
    }
    fun onShowRecovery(){
        _uiState.value = _uiState.value.copy(showRecovery = true)
    }

    init {
//        viewModelScope.launch {
//            keysRepository.secretListWithCredentials().collect { secretList ->
                var savedMnemonic = keyManager.fetch()
//                Log.d(TAG, "init(${savedMnemonic?.joinToString(" ") ?: ""})")
                _uiState.value = KeyUiState(
                    text = savedMnemonic?.joinToString(" ") ?: "",
                    mnemonic = savedMnemonic,
                    isSaved = savedMnemonic !== null,
                    error = if( savedMnemonic !== null) null else Exception(EXCEPTION_EMPTY_MNEMONIC)
                )
//            }
//        }
    }
    companion object {
        const val TAG = "KeyViewModel"
    }
}


data class KeyUiState(
    val text: String = "",
    val showConfirm: Boolean = false,
    val showRecovery: Boolean = false,
    val isSaved: Boolean = false,
    val isDirty: Boolean = false,
    val error: Exception? = null,
    val mnemonic: MnemonicCode? = null,
    val secretList: List<Passkey> = emptyList(),
)