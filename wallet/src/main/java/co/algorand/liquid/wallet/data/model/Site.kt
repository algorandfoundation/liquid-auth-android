package co.algorand.liquid.wallet.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sites",
    indices = [
        Index("url", unique = true),
    ],
)
data class Site (
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "url") val url: String = "",
    @ColumnInfo(name = "packageName") val packageName: String = "",
    @ColumnInfo(name = "name") val name: String = "",
)