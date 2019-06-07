package com.bmcreations.bookinfo.ui.camera

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bmcreations.bookinfo.extensions.uiScope
import com.bmcreations.bookinfo.network.Outcome
import com.bmcreations.bookinfo.network.book.model.BookVolumeResult
import com.bmcreations.bookinfo.network.book.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.File


class BookCaptureViewModel private constructor(val context: Context): ViewModel(), AnkoLogger {

    companion object {
        fun create(context: Context): BookCaptureViewModel {
            return BookCaptureViewModel(context)
        }
    }

    val results: MutableLiveData<Outcome<BookVolumeResult>> = MutableLiveData()


    private val repo by lazy {
        BookRepository(context)
    }

    init {
        results.value = Outcome.loading(false)
    }

    fun capture(imageCapture: ImageCapture?) {
        val file = File(context.externalMediaDirs.first(),
            "reads-${System.currentTimeMillis()}.jpg")
        imageCapture?.takePicture(file, object: ImageCapture.OnImageSavedListener {
            override fun onImageSaved(file: File) {
                info { "Photo capture success: ${file.absolutePath}" }
                CameraBookScraper(context).apply {
                    this.analyze(file) {
                        it.forEach { line ->
                            uiScope.launch(Dispatchers.IO) {
                                repo.lookupBooksForIsbn(line).also {
                                    uiScope.launch { results.value = it }
                                }
                            }
                        }
                    }
                }
            }

            override fun onError(useCaseError: ImageCapture.UseCaseError, message: String, cause: Throwable?) {
                info { "Photo capture failed: $message" }
                cause?.printStackTrace()
            }
        })
    }
}