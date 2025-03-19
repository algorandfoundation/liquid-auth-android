package co.algorand.liquid.wallet.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.algorand.liquid.wallet.Dimensions
import co.algorand.liquid.wallet.R
import co.algorand.liquid.wallet.data.model.Passkey
import co.algorand.liquid.wallet.data.model.Site
import co.algorand.liquid.wallet.data.query.SiteWithPasskeys

/**
 * This composable holds the UI logic to show credential details of selected domain/calling app
 * @param site : selected domain/site
 * @param onCancel : method to call on back press
 * @param onPasswordDelete : method to call on selected password credential delete
 * @param onPasskeyDelete : method to call on selected passkey credential delete
 * @param modifier : modifier for the composable
 */
@Composable
fun ShowCredentialsScreen(
    site: SiteWithPasskeys,
    onCancel: () -> Unit,
    onPasskeyDelete: (Passkey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    ShowCredentialsScreen(
        snackbarHostState,
        site,
        onCancel,
        onPasskeyDelete,
        modifier,
    )
}

/**
 * This composable holds the UI logic to show credential details of selected domain/calling app
 *
 * @param snackbarHostState The state of the SnackbarHost
 * @param site The selected domain/site
 * @param onCancel The callback to be invoked when the user clicks the back button
 * @param onPasswordDelete The callback to be invoked when the user clicks the delete button for a password credential
 * @param onPasskeyDelete The callback to be invoked when the user clicks the delete button for a passkey credential
 * @param modifier The modifier to be applied to the composable
 */
@Composable
fun ShowCredentialsScreen(
    snackbarHostState: SnackbarHostState,
    site: SiteWithPasskeys,
    onCancel: () -> Unit,
    onPasskeyDelete: (Passkey) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onCancel()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBarContent(site, onCancel, Modifier)
        },
        modifier = modifier,
    ) { innerPadding ->
        CredentialsEntry(innerPadding, site, onPasskeyDelete, Modifier)
    }
}

/**
 * This composable holds the UI logic to show the top app bar with the site name and a back button.
 *
 * @param site The selected domain/site
 * @param onCancel The callback to be invoked when the user clicks the back button
 * @param modifier The modifier to be applied to the composable
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TopAppBarContent(
    site: SiteWithPasskeys,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.credentials_for, site.site.url),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        navigationIcon = {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
    )
}

/**
 * Renders the credential entries for the selected domain/site.
 *
 * @param innerPadding The padding to apply to the inner content.
 * @param site The SiteWithCredentials object representing the site and its credentials.
 * @param onPasskeyDelete The callback to be invoked when a passkey is deleted.
 * @param onPasswordDelete The callback to be invoked when a password is deleted.
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
private fun CredentialsEntry(
    innerPadding: PaddingValues,
    site: SiteWithPasskeys,
    onPasskeyDelete: (Passkey) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .padding(innerPadding)
            .fillMaxWidth()
            .padding(Dimensions.padding_large)
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,

    ) {
        items(site.passkeys) {
            PasskeyEntry(
                passkey = it,
                onPasskeyDelete = onPasskeyDelete,
                Modifier,
            )
        }
    }
}



/**
 * This composable holds the UI logic to render a single passkey entry.
 *
 * @param passkey The PasskeyItem object representing the passkey
 * @param onPasskeyDelete The callback to be invoked when the user clicks the delete button
 * @param modifier The modifier to be applied to the composable
 */
@Composable
fun PasskeyEntry(
    passkey: Passkey,
    onPasskeyDelete: (Passkey) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Dimensions.padding_medium),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.padding_medium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.padding_medium),
        ) {
            TextField(
                value = passkey.username,
                onValueChange = {},
                readOnly = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedTextColor = MaterialTheme.colorScheme.outline,
                ),
            )
            Button(
                modifier = Modifier
                    .padding(horizontal = Dimensions.padding_small)
                    .align(Alignment.End),
                onClick = { onPasskeyDelete(passkey) },
            ) {
                Text(text = stringResource(R.string.delete))
            }
        }
    }
}

/**
 * This composable function provides a preview of the ShowCredentialsScreen composable.
 */
@Preview
@Composable
fun ShowCredentialsScreenPreview() {
    ShowCredentialsScreen(
        snackbarHostState = SnackbarHostState(),
        onCancel = {},
        onPasskeyDelete = {},
        site = SiteWithPasskeys(Site(), emptyList()),
        modifier = Modifier,
    )
}
