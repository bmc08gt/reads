package com.bmcreations.bookinfo.ui.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.File
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.text.FirebaseVisionText


class CameraBookScraper(val context: Context) : AnkoLogger {

    private val detector = FirebaseVision.getInstance().onDeviceTextRecognizer

    fun analyze(file: File?) {
        file?.let {
            val image = FirebaseVisionImage.fromFilePath(context, Uri.fromFile(it))
            detector.processImage(image)
                .addOnSuccessListener { text -> handleResult(text) }
                .addOnFailureListener { cause -> cause.printStackTrace() }
        } ?: info { "Unable to analyze file"}
    }

    fun analyze(proxy: ImageProxy?) {
        proxy?.image?.let {
            val image = FirebaseVisionImage.fromMediaImage(it, 0)
            detector.processImage(image)
                .addOnSuccessListener { text -> handleResult(text) }
                .addOnFailureListener { cause -> cause.printStackTrace() }
        } ?:info {  "Unable to analyze imageProxy" }
    }

    private fun handleResult(text: FirebaseVisionText) {
        val ret = mutableListOf<String>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                ret.add(line.text)
            }
        }
        info { ret }
    }
}