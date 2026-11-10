package com.example.dgumobilesoftwarefinalproject.data

import android.content.Context
import android.util.AtomicFile
import com.example.dgumobilesoftwarefinalproject.model.Meal
import java.io.File

/** Calls are serialized by the ViewModel and run on Dispatchers.IO. */
class MealRepository(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "meals.json"))

    fun load(): List<Meal> {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return emptyList()
        return file.openRead().bufferedReader().use { MealCodec.decode(it.readText()) }
    }

    fun save(meals: List<Meal>) {
        val bytes = MealCodec.encode(meals).toByteArray(Charsets.UTF_8)
        val stream = file.startWrite()
        try {
            stream.write(bytes)
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }
}
