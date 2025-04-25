package co.algorand.liquid.wallet.ui.credentials

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import co.algorand.liquid.wallet.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CredentialsTopBar() {
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
    )
}