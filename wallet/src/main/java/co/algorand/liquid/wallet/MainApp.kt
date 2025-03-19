package co.algorand.liquid.wallet

import android.app.Application

/**
 * This is the application level class used to initialize application level dependencies
 */
class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDependencies.init(this)
    }
}