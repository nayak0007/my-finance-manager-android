package com.myfinancemanager.app

import android.app.Application
import com.myfinancemanager.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MyFinanceApp : Application() {
    lateinit var container: AppContainer
        private set

    /**
     * For work that has to outlive an activity or a broadcast receiver — notably mirroring a
     * captured SMS, which must not hold the receiver open for a network round-trip.
     */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
    }

    companion object {
        lateinit var instance: MyFinanceApp
            private set
    }
}
