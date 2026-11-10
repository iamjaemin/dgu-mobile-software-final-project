package com.example.dgumobilesoftwarefinalproject.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import java.util.UUID

class PhotoRepository(private val context: Context) {
    private val folder = File(context.filesDir, "meal_photos").apply { mkdirs() }

    fun file(name: String): File {
        require(name.matches(Regex("[a-f0-9-]+\\.jpg"))) { "잘못된 사진 경로입니다." }
        return File(folder, name)
    }

    fun import(uri: Uri): String {
        val bitmap = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val scale = minOf(1.0, 1600.0 / maxOf(info.size.width, info.size.height))
                decoder.setTargetSize(maxOf(1, (info.size.width * scale).toInt()), maxOf(1, (info.size.height * scale).toInt()))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "사진을 읽을 수 없습니다." }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: error("사진을 읽을 수 없습니다.")
        }
        val name = "${UUID.randomUUID()}.jpg"
        try {
            file(name).outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it)) }
        } catch (error: Exception) {
            file(name).delete()
            throw error
        } finally {
            bitmap.recycle()
        }
        return name
    }

    fun deleteUnreferenced(namesToKeep: Set<String>) {
        folder.listFiles()?.filter { it.name !in namesToKeep }?.forEach { it.delete() }
    }
}
