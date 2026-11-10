package com.example.dgumobilesoftwarefinalproject

import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dgumobilesoftwarefinalproject.data.*
import com.example.dgumobilesoftwarefinalproject.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDateTime
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MealStorageTest {
    @Test fun selectedPhotoIsCopiedAndOnlyUnreferencedPhotosAreDeleted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "photo-test-${UUID.randomUUID()}").apply { mkdirs() }
        val isolated = object : ContextWrapper(context) { override fun getFilesDir() = directory }
        try {
            val original = File(directory, "original.png")
            val image = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)
            image.eraseColor(android.graphics.Color.rgb(248, 120, 32))
            original.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            image.recycle()
            val photos = PhotoRepository(isolated)
            val name = photos.import(Uri.fromFile(original))
            original.delete()
            assertTrue(photos.file(name).exists())
            val decoded = BitmapFactory.decodeFile(photos.file(name).path)
            assertEquals(80, decoded.width)
            decoded.recycle()
            photos.deleteUnreferenced(setOf(name))
            assertTrue(photos.file(name).exists())
            photos.deleteUnreferenced(emptySet())
            assertFalse(photos.file(name).exists())
        } finally { directory.deleteRecursively() }
    }

    @Test fun writesReloadsAndDeletesSnapshotsWithoutLosingPrecision() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "storage-test-${UUID.randomUUID()}").apply { mkdirs() }
        val isolated = object : ContextWrapper(context) { override fun getFilesDir() = directory }
        try {
            val repo = MealRepository(isolated)
            assertEquals(emptyList<Meal>(), repo.load())
            val meal = Meal("test", LocalDateTime.of(2026, 11, 11, 12, 30), MealType.LUNCH, null,
                listOf(MealItem("A", "밥과 반찬", 150.5, "g", Nutrients(205.5, 23.91, 10.05, 7.74))))
            repo.save(listOf(meal))
            assertEquals(listOf(meal), MealRepository(isolated).load())
            repo.save(emptyList())
            assertTrue(MealRepository(isolated).load().isEmpty())
        } finally { directory.deleteRecursively() }
    }

    @Test fun bundledCatalogHasAllSelectedFoodsAndBothUnits() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val foods = FoodRepository(context).foods
        assertEquals(2784, foods.size)
        assertEquals(foods.size, foods.map { it.code }.distinct().size)
        assertEquals(setOf("g", "mL"), foods.map { it.basisUnit }.toSet())
    }

    @Test fun malformedStorageDoesNotSilentlyReturnEmptyRecords() {
        assertThrows(Exception::class.java) { MealCodec.decode("not json") }
        assertThrows(Exception::class.java) { MealCodec.decode("{\"schemaVersion\":99,\"meals\":[]}") }
    }
}
