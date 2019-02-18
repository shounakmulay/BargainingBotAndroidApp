package com.example.shounak.bargainingbot.ui.main.bot

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shounak.bargainingbot.R
import com.example.shounak.bargainingbot.data.db.entity.Message
import com.example.shounak.bargainingbot.internal.MessageFrom
import com.example.shounak.bargainingbot.internal.NavigatedFrom
import com.example.shounak.bargainingbot.ui.base.ScopedFragment
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.bot_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance

class BotFragment : ScopedFragment(), KodeinAware {


    override val kodein: Kodein by closestKodein()
    private val viewModelFactory: BotViewModelFactory by instance()

    private lateinit var viewModel: BotViewModel
    private lateinit var groupAdapter: GroupAdapter<ViewHolder>
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var navigatedFrom: NavigatedFrom
//    private var savedMessages: List<Message>? = null

    private var navigated = false
    private var isBundleChecked = false
    private var isYesNoFragDisplayed = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkBundleOnCreate()
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

//        TODO("Acknowledge final order")

        Log.d("BotFragment", "OnCreateView")
        navigated = true
        val fragmentTransaction = childFragmentManager.beginTransaction()
        val botChatButtonUIFragment = BotChatButtonUIFragment()
        fragmentTransaction.apply {
            replace(R.id.bot_bottom_fragment, botChatButtonUIFragment)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            commit()
        }
        return inflater.inflate(R.layout.bot_fragment, container, false)


    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d("BotFragment", "OnActivityCreated")
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(BotViewModel::class.java)
        cancel_chip.visibility = View.GONE


        launch {
            viewModel.apply {
                setupApiAiService(this@BotFragment.context!!)

                response.observe(this@BotFragment, Observer {
                    addBotMessage(it)
                })

                userMessage.observe(this@BotFragment, Observer {
                    if (!it.equals("payment done", true)) {
                        addUserMessage(it)
                    }
                })

                action.observe(this@BotFragment, Observer {
                    manageUIWithAction(it)
                })

                fragmentToReplaceWith.observe(this@BotFragment, Observer {
                    Log.d("called", "Observer")
                    if (it != null) {
                        replaceBottomFragmentWith(it)
                    }
                })

                viewModel.messageHistory.await().observe(this@BotFragment, Observer {
                    if (navigated) {
                        Log.d("BotFragment", "navigated is true")
                        groupAdapter.clear()
                        addSavedMessages(it)
                        navigated = false
                        if (viewModel.foodAcknowledgement != "") {
                            addFoodAck(foodAcknowledgement)
                        }
                    }

                })


            }
        }

        initRecyclerView(this.context)

        actionOnBundleCheck()

        cancel_chip.setOnClickListener {
            launch {
                viewModel.sendAiRequest("Cancel")
            }
            replaceBottomFragmentWith(BotChatButtonUIFragment())
            cancel_chip.visibility = View.GONE
        }

    }

    private fun manageUIWithAction(action: String?) {
        when (action) {
            "OrderDrinksCounter", "OrderDrinksAccept", "OrderDrinksOfferLow" -> {
                if (!isYesNoFragDisplayed) {
                    replaceBottomFragmentWith(BotYesNoFragment())
                    cancel_chip.visibility = View.VISIBLE
                    isYesNoFragDisplayed = true
                }

            }
            "OrderDrinksTaunt" -> {
                replaceBottomFragmentWith(BotTauntResponseFragment())
                cancel_chip.visibility = View.VISIBLE
                isYesNoFragDisplayed = false
            }

            "MakeNewOffer" -> {
                replaceBottomFragmentWith(BotNewOfferFragment())
                cancel_chip.visibility = View.VISIBLE
                isYesNoFragDisplayed = false
            }
            "PlaceDrinksOrder" -> {
                replaceBottomFragmentWith(BotChatButtonUIFragment())
                cancel_chip.visibility = View.GONE
                isYesNoFragDisplayed = false
            }
        }
    }

    private fun addFoodAck(foodAcknowledgement: String) {
        runBlocking {
            withContext(Dispatchers.IO) {
                groupAdapter.add(MessageFromBotItem(foodAcknowledgement))
                linearLayoutManager.scrollToPosition(groupAdapter.itemCount - 1)
                viewModel.addFoodAcknowledgement(foodAcknowledgement)
            }

        }
    }

    private fun addSavedMessages(savedMessages: List<Message>?) {
        if (savedMessages != null && !savedMessages.isEmpty() && navigated) {
            for (message in savedMessages) {
                Log.d("addSavedMessages", message.toString())
                if (message.from == MessageFrom.USER) {
                    addUserMessage(message.message)
                } else if (message.from == MessageFrom.BOT) {
                    addBotMessage(message.message)
                }
            }
        }
    }


    private fun initRecyclerView(context: Context?) {
        groupAdapter = GroupAdapter<ViewHolder>()
        val recyclerView = bot_fragment_recycler_view

        recyclerView.apply {
            linearLayoutManager = LinearLayoutManager(context)
            linearLayoutManager.stackFromEnd = true
//            linearLayoutManager.reverseLayout = true
            layoutManager = linearLayoutManager
            adapter = groupAdapter
        }


    }


    private fun checkBundleOnCreate() {
        if (!isBundleChecked) {
            val args: BotFragmentArgs by navArgs()


            when {
                args.orderDrinks -> {
                    navigatedFrom = NavigatedFrom.DRINKS_MENU
                    isBundleChecked = true
                }
                args.orderFood -> {
                    navigatedFrom = NavigatedFrom.FOOD_MENU
                    isBundleChecked = true
                }
                args.paymentDone -> {
                    navigatedFrom = NavigatedFrom.ORDERS_FRAGMENT
                    isBundleChecked = true
                }
                else -> {
                    navigatedFrom = NavigatedFrom.NONE
                    isBundleChecked = true
                }
            }

        }
    }


    //TODO: Multiple chat entries appear
    private fun actionOnBundleCheck() {

        viewModel.setBundleDetails(navigatedFrom, isBundleChecked)

        val args: BotFragmentArgs by navArgs()
        launch {
            viewModel.actionOnBundleCheck(args, this@BotFragment.context!!)
        }
    }


    private fun addUserMessage(message: String?) {
        val url = viewModel.getPhotoUrl()
        groupAdapter.add(
            MessageFromUserItem(
                message,
                url,
                this@BotFragment.context!!
            )
        )
        linearLayoutManager.scrollToPosition(groupAdapter.itemCount - 1)
    }

    private fun addBotMessage(message: String?) {
        if (message != null && message != "") {
            groupAdapter.add(
                MessageFromBotItem(
                    message
                )
            )
            linearLayoutManager.scrollToPosition(groupAdapter.itemCount - 1)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("BotFragment", "OnDestroyView")
        viewModel.apply {
            response.removeObservers(this@BotFragment)
            userMessage.removeObservers(this@BotFragment)
        }

    }

    private fun replaceBottomFragmentWith(fragment: Fragment) {
        Log.d("called", "replaceBottomFragment")

        val fragmentTransaction = childFragmentManager.beginTransaction()
        fragmentTransaction.apply {
            replace(R.id.bot_bottom_fragment, fragment)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
//            addToBackStack("bottom_fragments")
            commit()
        }


    }
}


