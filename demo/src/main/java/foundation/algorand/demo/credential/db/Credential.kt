package foundation.algorand.demo.credential.db

import androidx.room.*

@Entity
data class Credential(
    @PrimaryKey val credentialId: String,
    @ColumnInfo(name = "origin") val origin: String,
    @ColumnInfo(name = "userHandle") val userHandle: String,
    // FIXME: Not secure storage of keys, this is just for demonstration
    @ColumnInfo(name = "publicKey") val publicKey: String,
    @ColumnInfo(name = "privateKey") val privateKey: String,
    @ColumnInfo(name = "count") val count: Int
)
