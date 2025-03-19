package co.algorand.liquid.wallet.ui.home

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.algorand.liquid.wallet.data.model.Passkey
import co.algorand.liquid.wallet.data.CredentialRepository
import co.algorand.liquid.wallet.data.RPIconDataSource
import co.algorand.liquid.wallet.data.query.SiteWithPasskeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * This class is a ViewModel that holds the business logic to operate on a list of credentials.
 * @param credentialsDataSource The data source for credentials.
 * @param RPIconDataSource The data source for rpicons.
 */
class HomeViewModel(
    private val credentialRepository: CredentialRepository,
    private val RPIconDataSource: RPIconDataSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()


    /**
     * Removes the associated passkey credential from the database.
     *
     * @param passkey The passkey credential to remove.
     */
    fun onPasskeyDelete(passkey: Passkey) {
        viewModelScope.launch {
            credentialRepository.removePasskey(passkey)
        }
    }

    // Initialize the home screen with list of saved credentials from calling apps.
    init {
        viewModelScope.launch {
            credentialRepository.siteListWithCredentials().collect { siteList ->
                // Get the icons
                val icons: MutableMap<String, Bitmap> = mutableMapOf()
                siteList.forEach {
                    val icon = RPIconDataSource.getIcon(it.site.url)
                    if (icon != null) {
                        icons[it.site.url] = icon
                    }
                }
                _uiState.value = HomeUiState(siteList = siteList, iconMap = icons)
            }
        }
    }
}

data class HomeUiState(
    val siteList: List<SiteWithPasskeys> = emptyList(),
    val iconMap: Map<String, Bitmap> = emptyMap(),
)