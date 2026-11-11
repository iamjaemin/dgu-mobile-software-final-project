package com.example.dgumobilesoftwarefinalproject.data

import com.example.dgumobilesoftwarefinalproject.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

internal fun JSONObject.nutrients() = Nutrients(
    getDouble("energyKcal"), getDouble("carbohydrateG"), getDouble("proteinG"), getDouble("fatG")
)

private fun Nutrients.toJson() = JSONObject().put("energyKcal", energyKcal)
    .put("carbohydrateG", carbohydrateG).put("proteinG", proteinG).put("fatG", fatG)

object MealCodec {
    fun encode(meals: List<Meal>): String = JSONObject().put("schemaVersion", 1).put("meals",
        JSONArray().apply {
            meals.forEach { meal ->
                put(JSONObject().put("id", meal.id).put("eatenAt", meal.eatenAt.toString())
                    .put("type", meal.type.name).put("photoName", meal.photoName ?: JSONObject.NULL)
                    .put("items", JSONArray().apply {
                        meal.items.forEach { item ->
                            put(JSONObject().put("foodCode", item.foodCode).put("name", item.name)
                                .put("amount", item.amount).put("unit", item.unit)
                                .put("nutrients", item.nutrients.toJson()))
                        }
                    }))
            }
        }).toString()

    fun decode(text: String): List<Meal> {
        val root = JSONObject(text)
        require(root.getInt("schemaVersion") == 1) { "지원하지 않는 기록 형식입니다." }
        val array = root.getJSONArray("meals")
        val result = (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val items = obj.getJSONArray("items")
            Meal(obj.getString("id"), LocalDateTime.parse(obj.getString("eatenAt")),
                MealType.valueOf(obj.getString("type")),
                if (obj.isNull("photoName")) null else obj.getString("photoName"),
                (0 until items.length()).map { j ->
                    val item = items.getJSONObject(j)
                    MealItem(item.getString("foodCode"), item.getString("name"), item.getDouble("amount"),
                        item.getString("unit"), item.getJSONObject("nutrients").nutrients())
                })
        }
        require(result.map { it.id }.distinct().size == result.size) { "중복된 식사 기록이 있습니다." }
        return result.sortedByDescending { it.eatenAt }
    }
}
