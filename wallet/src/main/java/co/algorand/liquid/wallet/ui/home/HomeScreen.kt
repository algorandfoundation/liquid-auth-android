package co.algorand.liquid.wallet.ui.home

import android.graphics.Bitmap
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.algorand.liquid.wallet.R
import co.algorand.liquid.wallet.data.model.Passkey
import co.algorand.liquid.wallet.data.query.SiteWithPasskeys

/**
 * This composable holds the stateful version of Home screen
 * @param homeViewModel : viewmodel instance handling business logic for Home Screen
 * @param openDrawer : method to open the drawer on click
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val hasShownCredentials = rememberSaveable { mutableStateOf(false) }
    val currentSiteId = rememberSaveable { mutableLongStateOf(0L) }

    HomeScreen(
        openDrawer,
        uiState,
        homeViewModel::onPasskeyDelete,
        hasShownCredentials,
        currentSiteId,
        modifier,
    )
}

/**
 * This class holds the stateless version of Home screen to ease preview
 * @param openDrawer : method to open the drawer on click
 * @param uiState : MutableStateFlow to retrieve updated state from viewmodel
 * @param onPasskeyDelete : Method to be called on passkey delete button click
 * @param onPasswordDelete : Method to be called on password delete button click
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
fun HomeScreen(
    openDrawer: () -> Unit,
    uiState: HomeUiState,
    onPasskeyDelete: (Passkey) -> Unit,
    hasShownCredentials: MutableState<Boolean>,
    currentSiteId: MutableLongState,
    modifier: Modifier = Modifier,
) {
    val site = uiState.siteList.find { it.site.id == currentSiteId.longValue }
    if (site != null && hasShownCredentials.value) {
        ShowCredentialsScreen(
            modifier = modifier,
            site = site,
            onCancel = { hasShownCredentials.value = false },
            onPasskeyDelete = {
                onPasskeyDelete(it)
            },
        )
    } else {
        HomeScreenContent(
            openDrawer = openDrawer,
            sites = uiState.siteList,
            iconMap = uiState.iconMap,
            { siteId ->
                currentSiteId.longValue = siteId
                hasShownCredentials.value = true
            },
            modifier,
        )
    }
}

/**
 * This composable contain the UI logic rendered on Home screen
 *
 * @param openDrawer The method to open the drawer on click.
 * @param sites The list of sites with credentials.
 * @param iconMap The map of site names to their corresponding icons.
 * @param onSiteSelected The callback to be invoked when a site is selected
 * @param modifier The modifier to be applied to the composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    openDrawer: () -> Unit,
    sites: List<SiteWithPasskeys>,
    iconMap: Map<String, Bitmap>,
    onSiteSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBarContent(openDrawer)
        },
        modifier = modifier,
    ) { innerPadding ->
        CredentialsList(
            sites = sites,
            iconMap = iconMap,
            onSiteSelected = onSiteSelected,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TopAppBarContent(openDrawer: () -> Unit) {
    CenterAlignedTopAppBar(
        modifier = Modifier,
        title = {
            Text(
                text = stringResource(R.string.credentials),
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.credentials),
                )
            }
        },
    )
}

/**
 * This composable function provides a preview of the HomeScreen composable.
 */
@Preview
@Composable
fun HomeScreenPreview() {
    val hasShownCredentials = rememberSaveable { mutableStateOf(false) }
    val currentSiteId = rememberSaveable { mutableLongStateOf(0L) }

    HomeScreen(
        openDrawer = {},
        uiState = HomeUiState(),
        onPasskeyDelete = {},
        hasShownCredentials = hasShownCredentials,
        currentSiteId = currentSiteId,
        modifier = Modifier,
    )
}