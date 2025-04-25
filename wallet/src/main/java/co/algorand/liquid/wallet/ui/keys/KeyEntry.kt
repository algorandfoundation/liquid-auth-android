package co.algorand.liquid.wallet.ui.keys

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.algorand.liquid.wallet.Dimensions
import co.algorand.liquid.wallet.R
import co.algorand.liquid.wallet.data.model.Passkey


@Composable
fun KeyEntry(
    key: Passkey,
    onKeySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
){
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimensions.padding_medium),
        border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(
            defaultElevation = Dimensions.padding_small,
        ),
        onClick = { onKeySelected(key.credentialId) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Image(
                modifier = Modifier
                    .padding(Dimensions.padding_medium)
                    .size(Dimensions.padding_extra_large, Dimensions.padding_extra_large),
                imageVector = Icons.Filled.Lock,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                contentDescription = stringResource(R.string.lock),
            )
            Text(
                text = key.userHandle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}