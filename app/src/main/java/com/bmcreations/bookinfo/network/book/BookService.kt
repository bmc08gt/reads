package com.bmcreations.bookinfo.network.book

import com.bmcreations.bookinfo.network.book.model.BookVolumeResult
import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import retrofit2.http.Query

interface BookService {
    @GET("volumes")
    fun lookupBooksByIsbnAsync(@Query("q") query: String,
                               @Query("key") key: String): Deferred<BookVolumeResult>
}