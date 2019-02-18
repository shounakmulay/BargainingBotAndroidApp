package com.example.shounak.bargainingbot.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.shounak.bargainingbot.data.db.Dao.OrderDao
import com.example.shounak.bargainingbot.data.db.OrderType
import com.example.shounak.bargainingbot.data.db.entity.FoodCartOrder
import com.example.shounak.bargainingbot.data.db.entity.Order
import com.example.shounak.bargainingbot.data.db.entity.User
import com.example.shounak.bargainingbot.data.network.OrderNetworkDataSource
import com.example.shounak.bargainingbot.data.network.SendGridAPIService
import com.google.firebase.firestore.DocumentChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*

class OrderRepositoryImpl(
    private val orderDao: OrderDao,
    private val orderNetworkDataSource: OrderNetworkDataSource,
    private val sendGridAPIService: SendGridAPIService
) : OrderRepository {

    override var isDrinksLoadingCompleted = MutableLiveData<Boolean>()

    private var isCartCleared = false

    init {
        isDrinksLoadingCompleted.value = false
        orderNetworkDataSource.drinksOrderChangeList.observeForever {
            runBlocking {
                updateOrderDatabase(it)
                isDrinksLoadingCompleted.value = true
            }
        }
    }

    private suspend fun updateOrderDatabase(list: List<DocumentChange>?) {
        withContext(Dispatchers.IO){
            if (!list.isNullOrEmpty()){

                orderDao.deleteDrinksOrder(OrderType.DRINKS)

                for (document in list){
                    val data = document.document.data
                    Log.d("firestore", data.toString())
                    val time  = Date().time
                    @Suppress("UNCHECKED_CAST")
                    val drink = data["drinks"] as Map<String, Any>
                    val searchedEntry = orderDao.findInOrders(drink["name"] as String)
                    val drinkQuantity = (drink["qty"] as String).toFloat().toInt()
                    val drinkCost = (drink["price"] as String).toFloat().toInt()
                    if (searchedEntry == null){
                        orderDao.addOrder(Order(
                            time = time,
                            type = OrderType.DRINKS,
                            name = drink["name"] as String,
                            quantity =  drinkQuantity,
                            cost = drinkCost * drinkQuantity
                        ))
                        Log.d("firestore", "added to orders database")
                    }else{
                        val newQuantity = drinkQuantity.plus(searchedEntry.quantity)
                        val newCost = searchedEntry.cost.plus(drinkQuantity*drinkCost)
                        orderDao.addOrder(Order(
                            time = time,
                            type = OrderType.DRINKS,
                            name = drink["name"] as String,
                            quantity = newQuantity,
                            cost = newCost
                        ))
                        Log.d("firestore", "added to orders database")

                    }

                }
            }else{
                if (isCartCleared){
                    withContext(Dispatchers.IO){
                        orderDao.clearOrders()
                    }
                }
            }
        }
    }

    override suspend fun getOrders(context: Context): LiveData<List<Order>> {
        return withContext(Dispatchers.IO){
            initFirestoreListeners(context)
            orderDao.getOrders()
        }
    }

    private suspend fun initFirestoreListeners(context: Context) {
        orderNetworkDataSource.getDrinksOrders(context)
    }

    override suspend fun getFoodCart(): LiveData<List<FoodCartOrder>> {
        return withContext(Dispatchers.IO) {
            orderDao.getFoodCart()
        }
    }

    override suspend fun addItemToFoodCart(time: Long, name: String, quantity: Int, cost: Int) {
        withContext(Dispatchers.IO) {
            val searchedEntry = orderDao.findInFoodCart(name)
            if(searchedEntry == null){
                orderDao.addFoodOrderToCart(
                    FoodCartOrder(time, name, quantity, cost)
                )
            }else{
                val newQuantity = searchedEntry.quantity + quantity
                val newCost = searchedEntry.cost + cost
                orderDao.addFoodOrderToCart(
                    FoodCartOrder(time,name, newQuantity, newCost)
                )
            }

        }
    }

    override suspend fun addCartToOrders(cartList : List<FoodCartOrder>){
       withContext(Dispatchers.IO){
           for (item in cartList){
               val searchedEntry = orderDao.findInOrders(item.name)
               if(searchedEntry == null){
                   orderDao.addOrder(Order(
                       item.time,
                       OrderType.FOOD,
                       item.name,
                       item.quantity,
                       item.cost
                   ))
               }else{
                   val newCost = searchedEntry.cost + item.cost
                   val newQuantity = searchedEntry.quantity + item.quantity
                   orderDao.addOrder(Order(
                       item.time,
                       OrderType.FOOD,
                       item.name,
                       newQuantity,
                       newCost
                   ))
               }

           }
       }

    }

    override suspend fun clearFoodCart(){
        withContext(Dispatchers.IO){
            orderDao.clearFoodCart()
        }
    }

    override suspend fun clearOrders(userId : String){

        withContext(Dispatchers.IO){
            orderNetworkDataSource.clearOrders(userId)
            isCartCleared = true
        }
    }

    override suspend fun clearLocalOrders(){
        withContext(Dispatchers.IO){
            orderDao.clearOrders()
            isCartCleared = true
        }
    }

    override suspend fun deleteItemFromFoodCart(name: String){
        withContext(Dispatchers.IO){
            orderDao.deleteItemFromFoodCart(name)
        }
    }

    override suspend fun checkOut(data : HashMap<String, Any>, user : User) {

        sendGridAPIService.sendEmail(data, user)
        orderNetworkDataSource.addToPreviousOrders(data)
    }

//    override suspend fun sendConfirmationEmail(){
//
//    }
}