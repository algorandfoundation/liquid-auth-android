package co.algorand.liquid.wallet.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import co.algorand.liquid.wallet.data.CredentialRepository
import co.algorand.liquid.wallet.data.RPIconDataSource

/**
 * This class is a factory for creating instances of the {@link HomeViewModel} class.
 *
 * <p>This factory is used by the {@link ViewModelProvider} to create instances of the {@link
 * HomeViewModel} class. The factory takes two parameters, {@code credentialsDataSource} and {@code
 * rpIconDataSource}, which are used to initialize the {@link HomeViewModel} instance.
 */
class HomeViewModelFactory(
    private val credentialRepository: CredentialRepository,
    private val RPIconDataSource: RPIconDataSource,
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(credentialRepository, RPIconDataSource) as T
    }
}