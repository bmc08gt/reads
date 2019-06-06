package com.bmcreations.bookinfo.ui.camera

import android.content.Context
import android.net.Uri
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.File

class CameraBookScraper(val context: Context) : AnkoLogger {

    private val detector by lazy {
        FirebaseVision.getInstance().visionBarcodeDetector
    }

    fun analyze(file: File?, cb: ((MutableList<String>) -> Unit)) {
        file?.let {
            val image = FirebaseVisionImage.fromFilePath(context, Uri.fromFile(it))
            detector.detectInImage(image)
                .addOnSuccessListener { upc -> handleResult(upc, cb) }
                .addOnFailureListener { cause -> cause.printStackTrace() }
        } ?: info { "Unable to analyze file"}
    }

    private fun handleResult(upcs: MutableList<FirebaseVisionBarcode>, cb: ((MutableList<String>) -> Unit)? = null) {
        val ret = mutableListOf<String>()
        for (upc in upcs) {
            upc.rawValue?.let { ret.add(it) }
        }
        info { ret }
        cb?.invoke(ret)
    }
}