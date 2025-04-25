package co.algorand.liquid.wallet.ui.credentials

import co.algorand.liquid.wallet.data.query.SiteWithPasskeys


data class CredentialsUiState(
    val siteList: List<SiteWithPasskeys> = emptyList(),
)