package com.example.dgumobilesoftwarefinalproject.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dgumobilesoftwarefinalproject.data.*
import com.example.dgumobilesoftwarefinalproject.domain.*
import com.example.dgumobilesoftwarefinalproject.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class DraftItem(val id: String = UUID.randomUUID().toString(), val food: Food,
                     val name: String = food.name, val amount: String = "100") {
    val quantity: Double? get() = amount.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 && it <= 100000 }
    val valid: Boolean get() = name.isNotBlank() && quantity != null
    val nutrition: Nutrients get() = quantity?.let { NutritionCalculator.portion(food, it) } ?: Nutrients()
}

data class MealUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val busy: Boolean = false,
    val foods: List<Food> = emptyList(),
    val meals: List<Meal> = emptyList(),
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    val type: MealType = MealType.BREAKFAST,
    val photo: String? = null,
    val items: List<DraftItem> = emptyList(),
    val message: String? = null,
    val saveVersion: Int = 0,
) {
    val preview: Nutrients get() = items.fold(Nutrients()) { sum, item -> sum + item.nutrition }
    val canSave: Boolean get() = !loading && !loadFailed && !busy && items.isNotEmpty() && items.all { it.valid }
}

class MealViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MealRepository(application)
    val photos = PhotoRepository(application)
    private val mutableState = MutableStateFlow(MealUiState())
    val state = mutableState.asStateFlow()

    init { load() }

    fun load() {
        if (!mutableState.value.loading && !mutableState.value.loadFailed) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, loadFailed = false) }
            try {
                val (foods, meals) = withContext(Dispatchers.IO) {
                    FoodRepository(getApplication()).foods to repository.load()
                }
                mutableState.update { it.copy(foods = foods, meals = meals, loading = false) }
                cleanup()
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, loadFailed = true,
                    message = "데이터를 읽지 못했습니다. 기존 기록을 보호하기 위해 저장을 중단했습니다.") }
            }
        }
    }

    fun setDate(value: LocalDate) { mutableState.update { it.copy(date = value) } }
    fun setTime(value: LocalTime) { mutableState.update { it.copy(time = value) } }
    fun setType(value: MealType) { mutableState.update { it.copy(type = value) } }
    fun add(food: Food) { mutableState.update { it.copy(items = it.items + DraftItem(food = food)) } }
    fun updateItem(id: String, name: String? = null, amount: String? = null) {
        mutableState.update { state -> state.copy(items = state.items.map {
            if (it.id == id) it.copy(name = name ?: it.name, amount = amount ?: it.amount) else it
        }) }
    }
    fun removeItem(id: String) { mutableState.update { state -> state.copy(items = state.items.filterNot { it.id == id }) } }
    fun clearMessage() { mutableState.update { it.copy(message = null) } }

    fun importPhoto(uri: Uri) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val name = withContext(Dispatchers.IO) { photos.import(uri) }
                mutableState.update { it.copy(photo = name) }
                cleanup()
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(message = "사진을 가져오지 못했습니다. 다른 사진을 선택해 주세요.") }
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    fun removePhoto() {
        mutableState.update { it.copy(photo = null) }
        viewModelScope.launch { cleanup() }
    }

    fun save() {
        val draft = mutableState.value
        if (!draft.canSave) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val meal = Meal(UUID.randomUUID().toString(), LocalDateTime.of(draft.date, draft.time),
                    draft.type, draft.photo, draft.items.map {
                        MealItem(it.food.code, it.name.trim(), it.quantity!!, it.food.basisUnit, it.nutrition)
                    })
                val meals = (draft.meals + meal).sortedByDescending { it.eatenAt }
                withContext(Dispatchers.IO) { repository.save(meals) }
                mutableState.update { it.copy(meals = meals, photo = null, items = emptyList(),
                    message = "식사를 기록했습니다.", saveVersion = it.saveVersion + 1) }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(message = "저장하지 못했습니다. 입력 내용을 유지했으니 다시 시도해 주세요.") }
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    fun delete(id: String) {
        if (mutableState.value.busy || mutableState.value.loadFailed) return
        val meals = mutableState.value.meals.filterNot { it.id == id }
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.save(meals) }
                mutableState.update { it.copy(meals = meals, message = "기록을 삭제했습니다.") }
                cleanup()
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(message = "기록을 삭제하지 못했습니다. 다시 시도해 주세요.") }
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    private suspend fun cleanup() {
        val keep = mutableState.value.meals.mapNotNull { it.photoName }.toSet() + listOfNotNull(mutableState.value.photo)
        withContext(Dispatchers.IO) { runCatching { photos.deleteUnreferenced(keep) } }
    }
}
