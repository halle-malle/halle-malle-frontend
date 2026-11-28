package com.do_it

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import com.google.firebase.storage.FirebaseStorage
import java.io.File

object ProfileImageLoader {
    fun load(view: ImageView, url: String) {
        view.tag = url
        view.setImageResource(R.drawable.ic_person)
        if (url.isBlank()) return
        val file = try {
            File.createTempFile("profile_", ".image", view.context.cacheDir)
        } catch (_: java.io.IOException) {
            return
        }
        try {
            FirebaseStorage.getInstance().getReferenceFromUrl(url).getFile(file)
                .addOnSuccessListener {
                    Thread {
                        try {
                            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(file.path, options)
                            options.inSampleSize = sampleSize(options)
                            options.inJustDecodeBounds = false
                            val bitmap = BitmapFactory.decodeFile(file.path, options)
                            view.post {
                                if (view.tag == url && bitmap != null) view.setImageBitmap(bitmap)
                            }
                        } catch (_: Exception) {
                            // 읽을 수 없는 사진은 기본 이미지로 표시
                        } finally {
                            file.delete()
                        }
                    }.start()
                }
                .addOnFailureListener { file.delete() }
        } catch (_: IllegalArgumentException) {
            file.delete()
        }
    }

    fun showSelected(view: ImageView, uri: Uri): Boolean {
        return try {
            val resolver = view.context.contentResolver
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            if (options.outWidth <= 0 || options.outHeight <= 0) return false
            options.inSampleSize = sampleSize(options)
            options.inJustDecodeBounds = false
            val bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return false
            view.tag = uri.toString()
            view.setImageBitmap(bitmap)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun sampleSize(options: BitmapFactory.Options): Int {
        var size = 1
        while (options.outWidth / size > 512 || options.outHeight / size > 512) size *= 2
        return size
    }
}
