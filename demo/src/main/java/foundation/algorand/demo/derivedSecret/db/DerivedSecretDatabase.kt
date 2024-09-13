package foundation.algorand.demo.derivedSecret.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [DerivedSecret::class], version = 1)
@TypeConverters(Converters::class)
abstract class DerivedSecretDatabase : RoomDatabase() {
    abstract fun derivedSecretDao(): DerivedSecretDao

    companion object {
        const val TAG = "DerivedSecretDatabase"
        private var INSTANCE: DerivedSecretDatabase? = null
        fun getInstance(context: Context): DerivedSecretDatabase {
            Log.d(TAG, "getInstance($context)")
            if (INSTANCE == null) {
                INSTANCE =
                        Room.databaseBuilder(
                                        context,
                                        DerivedSecretDatabase::class.java,
                                        "derivedSecret"
                                )
                                .allowMainThreadQueries()
                                .build()
            }

            return INSTANCE!!
        }
    }
}
