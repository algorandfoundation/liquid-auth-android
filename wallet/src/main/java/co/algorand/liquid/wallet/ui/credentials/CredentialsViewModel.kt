package co.algorand.liquid.wallet.ui.credentials

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.algorand.liquid.wallet.data.model.Passkey
import co.algorand.liquid.wallet.data.KeysRepository
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import androidx.core.net.toUri


class CredentialsViewModel(
    private val scanner: GmsBarcodeScanner?,
    private val keysRepository: KeysRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialsUiState())
    val uiState: StateFlow<CredentialsUiState> = _uiState.asStateFlow()

    fun onPasskeyDelete(passkey: Passkey) {
        viewModelScope.launch {
            keysRepository.removePasskey(passkey)
        }
    }

    fun onScanQrCode(context: Context,
                     onScanCompleted: (uri: Uri)-> Unit,
                     onScanFailure: (message: String)-> Unit
    ){
        scanner?.startScan()
            ?.addOnSuccessListener { barcode ->
                if(barcode.displayValue !== null){
                    onScanCompleted(barcode.displayValue!!.toUri())
                } else {
                    onScanFailure("Invalid Barcode")
                }
            }
            ?.addOnCanceledListener {
                Toast.makeText(context, "Canceled", Toast.LENGTH_LONG).show()
            }
            ?.addOnFailureListener { e ->
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
    }
    // Initialize the home screen with list of saved credentials from calling apps.
    init {
        viewModelScope.launch {
            keysRepository.siteListWithCredentials().collect { siteList ->
                _uiState.value = CredentialsUiState(siteList = siteList)
            }
        }
    }
}
