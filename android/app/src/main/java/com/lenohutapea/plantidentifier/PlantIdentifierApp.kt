package com.lenohutapea.plantidentifier

import android.app.Application
import com.lenohutapea.plantidentifier.di.AppContainer

class PlantIdentifierApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
