package com.bmcreations.bookinfo

import android.app.Application
import com.google.firebase.FirebaseApp
import com.squareup.picasso.Picasso
import com.squareup.picasso.OkHttp3Downloader
import java.util.Collections.singletonList
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.*


class BookInfoApp: Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        OkHttpClient.Builder()
            .protocols(singletonList(Protocol.HTTP_1_1))
            .build().apply {
                Picasso.Builder(this@BookInfoApp)
                    .downloader(OkHttp3Downloader(this))
                    .loggingEnabled(BuildConfig.DEBUG)
                    .build().apply {
                        Picasso.setSingletonInstance(this)
                    }
            }
    }
}