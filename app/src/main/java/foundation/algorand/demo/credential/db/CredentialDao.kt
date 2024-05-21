package foundation.algorand.demo.credential.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialDao {
    @Query("SELECT * FROM credential")
    fun getAll(): Flow<List<Credential>>
    @Query("SELECT * FROM credential")
    fun getAllRegular(): List<Credential>
    @Query("SELECT * FROM credential WHERE credentialId IN (:credentialIds)")
    fun loadAllByIds(credentialIds: List<String>): List<Credential>
    @Query("SELECT * FROM credential WHERE credentialId LIKE :credentialId LIMIT 1")
    fun findById(credentialId: String): Credential?

    @Query("SELECT * FROM credential WHERE userHandle LIKE :userHandle LIMIT 1")
    fun findByUser(userHandle: String): Credential

    @Insert
    suspend fun insertAll(vararg credentials: Credential)

    @Delete
    fun delete(credential: Credential)
}
