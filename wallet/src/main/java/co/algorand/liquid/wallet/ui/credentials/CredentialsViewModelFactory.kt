package co.algorand.liquid.wallet.ui.credentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import co.algorand.liquid.wallet.data.KeysRepository

class CredentialsViewModelFactory(
    private val keysRepository: KeysRepository,
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CredentialsViewModel(
            scanner = null,
            keysRepository= keysRepository,
        ) as T
    }
}