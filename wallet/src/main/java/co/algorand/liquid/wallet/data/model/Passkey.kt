package co.algorand.liquid.wallet.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "passkeys",
    indices = [
        Index("credentialId", unique = true),
    ],
)
data class Passkey(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,

    // User Data
    @ColumnInfo(name = "userId") val userId: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "userHandle") val userHandle: String,

    // Key Information
    @ColumnInfo(name = "publicKey") val publicKey: String,
    @ColumnInfo(name = "privateKey") val privateKey: String,
    @ColumnInfo(name = "credentialId") val credentialId: String,

    @ColumnInfo(name = "count") val count: Int,
    @ColumnInfo(name = "lastUsedTimeMs") val lastUsedTimeMs: Long,
    // Lookup Columns
    @ColumnInfo(name = "siteId") val siteId: Long,
)
