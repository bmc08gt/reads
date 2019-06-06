package com.bmcreations.bookinfo.network.book.model


import com.google.gson.annotations.SerializedName

data class BookVolume(
    @SerializedName("accessInfo")
    val accessInfo: AccessInfo?,
    @SerializedName("etag")
    val etag: String?, // 1LB88R/O4jw
    @SerializedName("id")
    val id: String?, // LvbvAAAAMAAJ
    @SerializedName("kind")
    val kind: String?, // books#volume
    @SerializedName("saleInfo")
    val saleInfo: SaleInfo?,
    @SerializedName("searchInfo")
    val searchInfo: SearchInfo?,
    @SerializedName("selfLink")
    val selfLink: String?, // https://www.googleapis.com/books/v1/volumes/LvbvAAAAMAAJ
    @SerializedName("volumeInfo")
    val volumeInfo: VolumeInfo?
) {
    data class AccessInfo(
        @SerializedName("accessViewStatus")
        val accessViewStatus: String?, // NONE
        @SerializedName("country")
        val country: String?, // US
        @SerializedName("embeddable")
        val embeddable: Boolean?, // false
        @SerializedName("epub")
        val epub: Epub?,
        @SerializedName("pdf")
        val pdf: Pdf?,
        @SerializedName("publicDomain")
        val publicDomain: Boolean?, // false
        @SerializedName("quoteSharingAllowed")
        val quoteSharingAllowed: Boolean?, // false
        @SerializedName("textToSpeechPermission")
        val textToSpeechPermission: String?, // ALLOWED
        @SerializedName("viewability")
        val viewability: String?, // NO_PAGES
        @SerializedName("webReaderLink")
        val webReaderLink: String? // http://play.google.com/books/reader?id=LvbvAAAAMAAJ&hl=&printsec=frontcover&source=gbs_api
    ) {
        data class Pdf(
            @SerializedName("isAvailable")
            val isAvailable: Boolean? // false
        )

        data class Epub(
            @SerializedName("isAvailable")
            val isAvailable: Boolean? // false
        )
    }

    data class SaleInfo(
        @SerializedName("country")
        val country: String?, // US
        @SerializedName("isEbook")
        val isEbook: Boolean?, // false
        @SerializedName("saleability")
        val saleability: String? // NOT_FOR_SALE
    )

    data class SearchInfo(
        @SerializedName("textSnippet")
        val textSnippet: String? // A mother tells her daughter a bedtime story in which each of several animals hears the same bedtime story from its mother.
    )

    data class VolumeInfo(
        @SerializedName("allowAnonLogging")
        val allowAnonLogging: Boolean?, // false
        @SerializedName("authors")
        val authors: List<String?>?,
        @SerializedName("canonicalVolumeLink")
        val canonicalVolumeLink: String?, // https://books.google.com/books/about/A_Sleepy_Story.html?hl=&id=LvbvAAAAMAAJ
        @SerializedName("categories")
        val categories: List<String?>?,
        @SerializedName("contentVersion")
        val contentVersion: String?, // 1.1.1.0.preview.0
        @SerializedName("description")
        val description: String?, // A mother tells her daughter a bedtime story in which each of several animals hears the same bedtime story from its mother.
        @SerializedName("imageLinks")
        val imageLinks: ImageLinks?,
        @SerializedName("industryIdentifiers")
        val industryIdentifiers: List<IndustryIdentifier?>?,
        @SerializedName("infoLink")
        val infoLink: String?, // http://books.google.com/books?id=LvbvAAAAMAAJ&dq=A+Sleepy&hl=&source=gbs_api
        @SerializedName("language")
        val language: String?, // en
        @SerializedName("maturityRating")
        val maturityRating: String?, // NOT_MATURE
        @SerializedName("pageCount")
        val pageCount: Int?, // 24
        @SerializedName("previewLink")
        val previewLink: String?, // http://books.google.com/books?id=LvbvAAAAMAAJ&q=A+Sleepy&dq=A+Sleepy&hl=&cd=3&source=gbs_api
        @SerializedName("printType")
        val printType: String?, // BOOK
        @SerializedName("publishedDate")
        val publishedDate: String?, // 1982
        @SerializedName("publisher")
        val publisher: String?, // Golden Books
        @SerializedName("readingModes")
        val readingModes: ReadingModes?,
        @SerializedName("title")
        val title: String? // A Sleepy Story
    ) {
        data class ReadingModes(
            @SerializedName("image")
            val image: Boolean?, // false
            @SerializedName("text")
            val text: Boolean? // false
        )

        data class IndustryIdentifier(
            @SerializedName("identifier")
            val identifier: String?, // 9780307101358
            @SerializedName("type")
            val type: String? // ISBN_13
        )

        data class ImageLinks(
            @SerializedName("smallThumbnail")
            val smallThumbnail: String?, // http://books.google.com/books/content?id=LvbvAAAAMAAJ&printsec=frontcover&img=1&zoom=5&source=gbs_api
            @SerializedName("thumbnail")
            val thumbnail: String? // http://books.google.com/books/content?id=LvbvAAAAMAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api
        )
    }
}