package com.example.clientkurswork

import android.app.Application

class MyApp : Application() {
    lateinit var netHandler: NetworkHandler

    override fun onCreate() {
        super.onCreate()
        netHandler = NetworkHandler(applicationContext)
    }
}
