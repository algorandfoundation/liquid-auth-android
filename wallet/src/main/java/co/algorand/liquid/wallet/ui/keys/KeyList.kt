package co.algorand.liquid.wallet.ui.keys

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.algorand.liquid.wallet.Dimensions
import co.algorand.liquid.wallet.data.model.Passkey



@Composable
fun KeysList(
    keys: List<Passkey>,
    onKeySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(Dimensions.padding_medium),
        modifier = modifier
    ){
        var headerText = "These keys have been derived from the saved Mnemonic"
        if(keys.isEmpty()){
            headerText = "No keys to display"
        }
        Text(
            text = headerText,
            modifier = Modifier.padding(Dimensions.padding_large),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .2f))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.padding_medium),
            shape = RoundedCornerShape(5),
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy((-1).dp),
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            ) {
                items(keys) {
                    KeyEntry(
                        key = it,
                        onKeySelected = onKeySelected,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun KeysListPreview() {
    val list: List<Passkey> = emptyList()

    KeysList(
        keys = list,
        onKeySelected = {},
        modifier = Modifier,
    )
}