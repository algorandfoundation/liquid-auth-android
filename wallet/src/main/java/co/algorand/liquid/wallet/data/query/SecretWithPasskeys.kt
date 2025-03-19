package co.algorand.liquid.wallet.data.query

import androidx.room.Embedded
import androidx.room.Relation
import co.algorand.liquid.wallet.data.model.Passkey
import co.algorand.liquid.wallet.data.model.Secret

data class SecretWithPasskeys(
    @Embedded val site: Secret,
    @Relation(
        parentColumn = "id",
        entityColumn = "secretId",
    )
    val passkeys: List<Passkey>,
)