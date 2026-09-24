package io.github.mdshakib007.appwall

import android.app.Application

class AppWallApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}
