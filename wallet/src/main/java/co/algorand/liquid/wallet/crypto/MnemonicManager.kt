package co.algorand.liquid.wallet.crypto

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cash.z.ecc.android.bip39.Mnemonics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking


private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "root_seed_preferences"
)
class MnemonicManager(private val applicationContext: Context) {
    companion object {
        val PREFERENCE_KEY = stringPreferencesKey("secret")
    }

    fun clear() {
        runBlocking {
            applicationContext.dataStore.edit {
                it.clear()
            }
        }
    }
    fun save(mnemonic: Mnemonics.MnemonicCode) {
        runBlocking {
            applicationContext.dataStore.edit {
                it[PREFERENCE_KEY] = mnemonic.joinToString(" ")
            }
        }
    }

    fun fetch(): Mnemonics.MnemonicCode? {
        var value: String? = null
        runBlocking {
            val values = applicationContext.dataStore.data.first()
            value = values[PREFERENCE_KEY]
        }
        return if (value !== null) Mnemonics.MnemonicCode(value) else value
    }
}