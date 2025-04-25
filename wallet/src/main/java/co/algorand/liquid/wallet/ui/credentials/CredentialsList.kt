package co.algorand.liquid.wallet.ui.credentials

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.algorand.liquid.wallet.data.query.SiteWithPasskeys
import co.algorand.liquid.wallet.Dimensions
import co.algorand.liquid.wallet.R


@Composable
fun CredentialsList(
    sites: List<SiteWithPasskeys>,
    onSiteSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(Dimensions.padding_medium),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.instructions),
            textAlign = TextAlign.Left,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.credentials_instructions),
            textAlign = TextAlign.Left,
            style = MaterialTheme.typography.titleMedium,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .2f))
        if (sites.isEmpty()) {
            Text(
                text= buildAnnotatedString {
                    append(stringResource(R.string.credentials_not_found))
                    append(". Visit ")
                    withLink(LinkAnnotation.Url("https://webauthn.io",
                        TextLinkStyles(style = SpanStyle(color = Color.Magenta))
                    )) {
                        append("WebAuthn")
                    }
                    append(" or the ")
                    withLink(LinkAnnotation.Url("https://liquidauth.com#get-connected",
                        TextLinkStyles(style = SpanStyle(color = Color.Magenta))
                    )) {
                        append("Liquid Auth")
                    }
                    append(" website to add credentials.")
                }
            )
        }
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
                items(sites) {
                    CredentialEntry(
                        site = it,
                        onSiteSelected = onSiteSelected,
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun CredentialsListPreview() {
    val list: List<SiteWithPasskeys> = emptyList()

    CredentialsList(
        sites = list,
        onSiteSelected = {},
        modifier = Modifier,
    )
}