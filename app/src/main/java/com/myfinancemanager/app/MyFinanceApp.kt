package com.myfinancemanager.app

import android.app.Application
import com.myfinancemanager.app.di.AppContainer

class MyFinanceApp : Application() {
    lateinit var container: AppContainer
        private set

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
