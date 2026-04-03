package com.example.clientkurswork

import android.app.Application
import com.example.clientkurswork.online_game.NetworkHandler

class MyApp : Application() {
    lateinit var netHandler: NetworkHandler

    override fun onCreate() {
        super.onCreate()
        netHandler = NetworkHandler(applicationContext)
    }
}
