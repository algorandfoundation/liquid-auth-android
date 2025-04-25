package co.algorand.liquid.wallet.ui.credentials

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import co.algorand.liquid.wallet.data.model.Passkey
import co.algorand.liquid.wallet.data.query.SiteWithPasskeys
import co.algorand.liquid.wallet.ui.icons.QRCode

@Composable
fun CredentialScreen(
    navController: NavController,
    credentialsViewModel: CredentialsViewModel,
    modifier: Modifier = Modifier,
    onScan: (uri: Uri) -> Unit = {},
) {
    val ctx = LocalContext.current
    val uiState by credentialsViewModel.uiState.collectAsStateWithLifecycle()
    val hasShownCredentials = rememberSaveable { mutableStateOf(false) }
    val currentSiteId = rememberSaveable { mutableLongStateOf(0L) }

    CredentialScreen(
        uiState = uiState,
        onScan = {
            credentialsViewModel.onScanQrCode(
                ctx,
                onScanFailure = { },
                onScanCompleted = onScan
            )},
        onPasskeyDelete = credentialsViewModel::onPasskeyDelete,
        onNavigate = {navController.navigate(it)},
        hasShownCredentials = hasShownCredentials,
        currentSiteId = currentSiteId,
        modifier = modifier,
    )
}

@Composable
fun CredentialScreen(
    uiState: CredentialsUiState,
    onScan: () -> Unit,
    onNavigate: (String) -> Unit,
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
        CredentialScreenContent(
            sites = uiState.siteList,
            onNavigate = onNavigate,
            onScan = onScan,
            onSiteSelected = { siteId ->
                currentSiteId.longValue = siteId
                hasShownCredentials.value = true
            },
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialScreenContent(
    sites: List<SiteWithPasskeys>,
    onScan: () -> Unit,
    onNavigate: (location: String) -> Unit,
    onSiteSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            CredentialsTopBar()
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = {onNavigate("keys")}) {
                        Icon(Icons.Filled.Lock, contentDescription = "Edit Secret")
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { onScan()},
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Icon(QRCode, "Scan new site")
                    }
                }
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        CredentialsList(
            sites = sites,
            onSiteSelected = onSiteSelected,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * This composable function provides a preview of the CredentialScreen composable.
 */
@Preview
@Composable
fun CredentialScreenPreview() {
    val hasShownCredentials = rememberSaveable { mutableStateOf(false) }
    val currentSiteId = rememberSaveable { mutableLongStateOf(0L) }

    CredentialScreen(
        uiState = CredentialsUiState(),
        onScan = {},
        onNavigate = {},
        onPasskeyDelete = {},
        hasShownCredentials = hasShownCredentials,
        currentSiteId = currentSiteId,
        modifier = Modifier,
    )
}