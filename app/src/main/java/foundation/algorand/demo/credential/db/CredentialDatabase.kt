package foundation.algorand.demo.credential.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
@Database(entities = [Credential::class], version = 1)
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
                    "credential"
                )
                    .allowMainThreadQueries()
                    .build()
            }

            return INSTANCE!!
        }
    }

}
