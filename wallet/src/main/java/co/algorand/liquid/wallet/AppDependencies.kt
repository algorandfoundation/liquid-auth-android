/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.algorand.liquid.wallet

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import androidx.room.Room
import co.algorand.liquid.wallet.data.CredentialDatabase
import co.algorand.liquid.wallet.data.CredentialRepository
import co.algorand.liquid.wallet.data.RPIconDataSource
import co.algorand.liquid.wallet.data.ServiceRepository

/**
 * This class is an application-level singleton object which is providing dependencies required for the app to function.
 * We recommend using dependency injection framework while working on production apps.
 */
object AppDependencies {
    lateinit var database: CredentialDatabase
    lateinit var sharedPreferences: SharedPreferences
    lateinit var credentialsRepository: CredentialRepository
    lateinit var serviceRepository: ServiceRepository

    var providerIcon: Icon? = null
    lateinit var rpIconDataSource: RPIconDataSource

    /**
     * Initializes the core components required for the application's data storage and icon handling.
     * This includes:
     * * **sharedPreference:** Creates a sharedpreference instance for storing application metadata.
     * * **database:** Creates a Room database instance for storing application data.
     * * **RPIconDataSource:** Initializes a data source for handling Relying Party icons (rpicons).
     * * **provider icon:** Sets a default icon to represent secure data providers.
     *
     * @param context The application context, used for accessing resources and file storage.
     */
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(
            context.packageName,
            Context.MODE_PRIVATE,
        )

        database = Room.databaseBuilder(context, CredentialDatabase::class.java, "credentials.db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()

        rpIconDataSource = RPIconDataSource(context.applicationInfo.dataDir)
        providerIcon = Icon.createWithResource(context, R.drawable.android_secure)


        credentialsRepository =
            CredentialRepository(
                credentialDao = database.credentialDao(),
                applicationContext = context
            )
        serviceRepository = ServiceRepository(
            credentialRepository = credentialsRepository,
            applicationContext = context
        )
    }
}
