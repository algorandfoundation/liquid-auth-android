package co.algorand.liquid.wallet.data

import android.content.Context
import android.util.Log
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import co.algorand.liquid.wallet.data.model.Passkey
import co.algorand.liquid.wallet.data.model.Secret
import co.algorand.liquid.wallet.data.model.Site
import co.algorand.liquid.wallet.data.query.SiteWithPasskeys
import kotlinx.coroutines.flow.Flow

@Database(entities = [Passkey::class, Secret::class, Site::class], version = 1)
abstract class CredentialDatabase : RoomDatabase() {
    abstract fun credentialDao(): CredentialDao
    companion object {
        const val TAG = "CredentialDatabase"
        private var INSTANCE: CredentialDatabase? = null
        fun getInstance(context: Context): CredentialDatabase {
            Log.d(TAG, "getInstance($context)")
            if(INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context,
                    CredentialDatabase::class.java,
                    "credentials"
                )
                    .allowMainThreadQueries()
                    .build()
            }

            return INSTANCE!!
        }
    }

}

@Dao
interface CredentialDao {
    // Secret Methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecret(entity: Secret): Long
    @Query("SELECT * from secrets WHERE address = :address")
    suspend fun getSecret(address: String): Secret?
    @Update
    suspend fun updateSecret(entity: Secret)
    @Delete
    suspend fun deleteSecret(entity: Secret)

    // Passkey Methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPasskey(entity: Passkey): Long
    @Query("SELECT * from passkeys WHERE credentialId = :credId")
    fun getPasskey(credId: String): Passkey?
    @Update
    suspend fun updatePasskey(entity: Passkey)
    @Delete
    suspend fun deletePasskey(entity: Passkey)
    @Query("SELECT COUNT(*) FROM sites WHERE url = :site")
    fun countPasskeys(site: Long): Int

    // Site Methods
    @Query("SELECT * FROM sites WHERE url = :url")
    suspend fun getSite(url: String): Site?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(entity: Site): Long
    @Delete
    suspend fun deleteSite(entity: Site)
    @Query("SELECT COUNT(*) FROM sites WHERE url = :url")
    fun getSiteCount(url: String): Int?

    // Lookup Methods
    @Transaction
    @Query("SELECT * FROM sites WHERE url = :url")
    fun getCredentialsFromSite(url: String): SiteWithPasskeys?
    @Transaction
    @Query("SELECT * FROM sites ORDER BY url")
    fun siteListWithCredentials(): Flow<List<SiteWithPasskeys>>

}