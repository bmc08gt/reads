package com.bmcreations.bookinfo.network.book.model

import com.google.gson.annotations.SerializedName

open class BookReview(
    @SerializedName("reviews_widget")
    val iframe: String)

object EmptyReview: BookReview("")