package com.mosquishe.today

import android.app.Application
import com.mosquishe.today.di.AppContainer

/** Application entry point. Builds and holds the manual-DI [AppContainer]. */
class TodayApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
