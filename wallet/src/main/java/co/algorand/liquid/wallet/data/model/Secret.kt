package co.algorand.liquid.wallet.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "secrets",
    indices = [
        Index("address", unique = true),
    ],
)
data class Secret(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "privateKey") val privateKey: String,
)
