package foundation.algorand.demo.derivedSecret.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
@Dao
interface DerivedSecretDao {
    @Query("SELECT * FROM derivedSecret") fun getAll(): Flow<List<DerivedSecret>>
    @Query("SELECT * FROM derivedSecret") fun getAllRegular(): List<DerivedSecret>
    @Query("SELECT * FROM derivedSecret WHERE id IN (:ids)")
    fun loadAllByIds(ids: List<String>): List<DerivedSecret>
    @Query("SELECT * FROM derivedSecret WHERE id LIKE :id LIMIT 1")
    fun findById(id: String): DerivedSecret?
    @Query("SELECT * FROM derivedSecret WHERE type LIKE :secretType LIMIT 1")
    fun findBySecretType(secretType: SecretType): DerivedSecret?

    @Insert suspend fun insertAll(vararg derivedSecret: DerivedSecret)

    @Delete fun delete(derivedSecret: DerivedSecret)

    @Insert fun insertAllNoSuspend(vararg derivedSecrets: DerivedSecret)
}
