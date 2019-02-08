package com.example.shounak.bargainingbot.ui.main.food

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.shounak.bargainingbot.data.db.entity.Food
import com.example.shounak.bargainingbot.data.repository.MenuRepository
import com.example.shounak.bargainingbot.internal.lazyDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FoodMenuViewModel(private val menuRepository: MenuRepository) : ViewModel() {

    val food by lazyDeferred {
        return@lazyDeferred menuRepository.getFoodMenu()
    }

    suspend fun getFoodMenuTitles(list: List<Food>): ArrayList<String> {
        return withContext(Dispatchers.Default) {
            val newList = list.distinctBy {
                it.type
            }
            Log.d("foodviewmodel", newList.toString())
            val titleList: ArrayList<String> = ArrayList(10)
            newList.forEach {
                titleList.add(it.type)
            }
            Log.d("foodviewmodel", titleList.toString())
            return@withContext titleList
        }
    }

    suspend fun getFoodListByType(type: String) : List<Food>{
        return menuRepository.getFoodListByType(type)
    }

}
