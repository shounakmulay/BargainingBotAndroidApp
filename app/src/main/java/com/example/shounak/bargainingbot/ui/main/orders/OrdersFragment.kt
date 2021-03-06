package com.example.shounak.bargainingbot.ui.main.orders

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shounak.bargainingbot.R
import com.example.shounak.bargainingbot.data.provider.PreferenceProvider
import com.example.shounak.bargainingbot.ui.base.ScopedFragment
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.orders_fragment.*
import kotlinx.coroutines.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance

/**
 * Orders Fragment
 */

class OrdersFragment : ScopedFragment(), KodeinAware {
    override val kodein: Kodein by closestKodein()
    private val viewModelFactory: OrdersViewModelFactory by instance()


    private lateinit var viewModel: OrdersViewModel
    private val groupAdapter: GroupAdapter<ViewHolder> = GroupAdapter()
    private var itemsCount = MutableLiveData<Int>()
    private var totalCost = MutableLiveData<Int>()

    private val GST_PERCENT = 0.09

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.orders_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        pay_progressBar.visibility = View.GONE
        pay_processing_textView.visibility = View.GONE
        orders_loading_group.visibility = View.VISIBLE

        ordersFragmentLayout.isClickable = false

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(OrdersViewModel::class.java)


        launch {
            viewModel.apply {
                val orders = orders.await()

                orders.observe(this@OrdersFragment, Observer {
                    groupAdapter.clear()
                    var total = 0
                    for (order in it) {
                        groupAdapter.add(
                            OrdersListItem(
                                name = order.name,
                                quantity = order.quantity,
                                cost = order.cost
                            )
                        )
                        total += order.cost
                    }
                    totalCost.postValue(total)
                    itemsCount.postValue(groupAdapter.itemCount)

                })

                isDrinksLoadingCompleted.observe(this@OrdersFragment, Observer {
                    Log.d("drinksloaded", it.toString())
                    if (it == true) {
                        orders_loading_group.visibility = View.GONE
                        ordersFragmentLayout.isClickable = true
                        isDrinksLoadingCompleted.value = false
                    }
                })
            }
        }

        initRecyclerView(this.context)

        bindUI()

    }


    private fun initRecyclerView(context: Context?) {

        orders_fragment_recycler_view.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }

    }


    private fun bindUI() {

        itemsCount.observe(this@OrdersFragment, Observer {
            val text = "You have ordered $it items."
            order_food_item_count_text.text = text
        })

        totalCost.observe(this@OrdersFragment, Observer {

            cost_before_gst_textView.text = it.toString()
            val gstCost = (it * 0.09).toInt()
            Log.e("GST cost", gstCost.toString())
            cgst_textView.text = gstCost.toString()
            sgst_textView.text = gstCost.toString()
            val total = (it + (gstCost * 2))
            orders_total_amount_text_view.text = total.toString()
        })


        order_fragment_pay_button.setOnClickListener {

            launch {
                val a = async {
                    pay_progressBar.visibility = View.VISIBLE
                    pay_processing_textView.visibility = View.VISIBLE
                }
                val b = async {
                    if (groupAdapter.itemCount != 0) {

                        val userId = PreferenceProvider.getPrefrences(this@OrdersFragment.context!!)
                            .getString(PreferenceProvider.USER_ID, "")

                        runBlocking {
                            val checkout = async {
                                val orderList = viewModel.orders.await().value
                                val gstCost = totalCost.value?.times(GST_PERCENT)
                                val total = totalCost.value?.plus(gstCost!! * 2)
                                viewModel.checkoutWithUserId(userId, orderList, gstCost?.toInt()!!, total?.toInt()!!)
                            }

                            val clearOrders = async { viewModel.clearOrders(userId!!) }

                            awaitAll(checkout, clearOrders)
                        }

                        withContext(Dispatchers.Main) { viewModel.clearLocalOrders() }

                        val toBotFragment = OrdersFragmentDirections.actionToBotFragment(paymentDone = true)

                        PreferenceProvider.getPrefrences(this@OrdersFragment.context!!).edit()
                            .putBoolean(PreferenceProvider.FIRST_LAUNCH, true).apply()

                        Navigation.findNavController(this@OrdersFragment.view!!).navigate(toBotFragment)
                    }
                }
                awaitAll(a, b)
            }






        }

    }

}


