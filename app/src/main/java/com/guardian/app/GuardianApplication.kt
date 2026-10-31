package com.guardian.app

import android.app.Application
import com.guardian.app.core.data.GuardianDatabase

class GuardianApplication : Application() {

    val database: GuardianDatabase by lazy {
        GuardianDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: GuardianApplication
            private set
    }
}
