package com.bmcreations.bookinfo

import android.app.Application
import com.google.firebase.FirebaseApp

class BookInfoApp: Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}