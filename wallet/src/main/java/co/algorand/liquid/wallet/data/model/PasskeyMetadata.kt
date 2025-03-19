package co.algorand.liquid.wallet.data.model


data class PasskeyMetadata(
    val uid: String,
    val rpid: String,
    val username: String,
    val displayName: String,
    val credId: String,
    val credPrivateKey: String,
    val credParentKey: Long?
)
