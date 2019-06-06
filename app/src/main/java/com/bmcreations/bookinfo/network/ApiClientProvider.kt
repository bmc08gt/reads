package com.bmcreations.bookinfo.network

import android.content.Context
import androidx.annotation.WorkerThread
import com.bmcreations.bookinfo.BuildConfig
import com.bmcreations.bookinfo.network.book.BookService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

fun provideGson(): Gson {
    return GsonBuilder().create()
}

fun provideBookService(retrofit: Retrofit): BookService {
    return retrofit.create(BookService::class.java)
}

fun provideOkHttpClientBuilder(): OkHttpClient.Builder {
    return OkHttpClient.Builder().apply {
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().also {
                it.level = HttpLoggingInterceptor.Level.BODY
            }
            this.addInterceptor(logging)
        }
    }
}

fun provideOkHttpClient(): OkHttpClient {
    return provideOkHttpClientBuilder().build()
}

fun provideRetrofit(gson: Gson = provideGson(), httpClient: OkHttpClient = provideOkHttpClient(), baseUrl: String): Retrofit {
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(httpClient)
        .build()
}