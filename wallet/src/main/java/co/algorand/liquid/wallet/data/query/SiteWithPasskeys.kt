package co.algorand.liquid.wallet.data.query

import androidx.room.Embedded
import androidx.room.Relation
import co.algorand.liquid.wallet.data.model.Passkey
import co.algorand.liquid.wallet.data.model.Site

data class SiteWithPasskeys(
    @Embedded val site: Site,
    @Relation(
        parentColumn = "id",
        entityColumn = "siteId",
    )
    val passkeys: List<Passkey>,
)