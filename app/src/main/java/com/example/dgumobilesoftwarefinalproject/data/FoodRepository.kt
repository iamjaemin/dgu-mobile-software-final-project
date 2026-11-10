package com.example.dgumobilesoftwarefinalproject.data

import android.content.Context
import com.example.dgumobilesoftwarefinalproject.model.Food
import org.json.JSONObject

class FoodRepository(context: Context) {
    val foods: List<Food> = context.assets.open("foods.json").bufferedReader().use {
        val root = JSONObject(it.readText())
        require(root.getInt("schemaVersion") == 1)
        val rows = root.getJSONArray("foods")
        (0 until rows.length()).map { i ->
            val row = rows.getJSONObject(i)
            Food(row.getString("code"), row.getString("name"), row.getString("category"),
                row.getString("origin"), row.getDouble("basisAmount"), row.getString("basisUnit"),
                row.getJSONObject("nutrients").nutrients())
        }
    }
}
