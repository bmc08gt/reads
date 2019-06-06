package com.bmcreations.bookinfo.network.book.repository

import android.content.Context
import com.bmcreations.bookinfo.R
import com.bmcreations.bookinfo.extensions.strings
import com.bmcreations.bookinfo.network.Outcome
import com.bmcreations.bookinfo.network.book.model.BookVolumeResult
import com.bmcreations.bookinfo.network.provideBookService
import com.bmcreations.bookinfo.network.provideRetrofit
import org.jetbrains.anko.AnkoLogger

class BookRepository(val context: Context): AnkoLogger {

    companion object {
        const val API_VERSION = 1
        const val BASE_URL = "https://www.googleapis.com/books"
    }

    private val retrofit by lazy {
        provideRetrofit(baseUrl = "$BASE_URL/v$API_VERSION/")
    }

    private val service by lazy {
        provideBookService(retrofit)
    }

    suspend fun lookupBooksForIsbn(isbn: String): Outcome<BookVolumeResult> {
        val req = service.lookupBooksByIsbnAsync(isbn, key = context.strings[R.string.googlebooks_api_key])
        var ret : Outcome<BookVolumeResult>
        try {
            req.await().run {

                ret = Outcome.success(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ret = Outcome.failure(e)
        }
        return ret

    }
}