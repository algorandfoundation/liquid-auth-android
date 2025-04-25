package co.algorand.liquid.wallet.ui.keys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import co.algorand.liquid.wallet.crypto.MnemonicManager
import co.algorand.liquid.wallet.data.KeysRepository

class KeyViewModelFactory(
    private val keysRepository: KeysRepository,
    private val keyManager: MnemonicManager,
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return KeyViewModel(
            keysRepository = keysRepository,
            keyManager = keyManager
        ) as T
    }
}