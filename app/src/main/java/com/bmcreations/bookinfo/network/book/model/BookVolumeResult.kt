package com.bmcreations.bookinfo.network.book.model

import com.google.gson.annotations.SerializedName

data class BookVolumeResult(
    @SerializedName("kind") val kind: String,
    @SerializedName("totalItems") val count: Int,
    @SerializedName("items") private val _results: List<BookVolume>?
) {
    var results: List<BookVolume> = emptyList()
        get() = _results ?: emptyList()
}