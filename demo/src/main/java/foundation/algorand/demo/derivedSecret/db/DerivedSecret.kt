package foundation.algorand.demo.derivedSecret.db

import androidx.room.*
import androidx.room.TypeConverter

enum class SecretType {
    PASSKEY,
    SPENDKEY
}

class Converters {
    @TypeConverter
    fun fromSecretType(value: SecretType): String {
        return value.name
    }

    @TypeConverter
    fun toSecretType(value: String): SecretType {
        return SecretType.valueOf(value)
    }
}

@Entity
data class DerivedSecret(
        @PrimaryKey val id: String,
        // FIXME: Not secure storage of keys, this is just for demonstration
        @ColumnInfo(name = "derivedSecret") val derivedSecret: String,
        @ColumnInfo(name = "mnemonic") val mnemonic: String,
        @ColumnInfo(name = "type") val type: SecretType
)
