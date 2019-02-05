package com.example.shounak.bargainingbot.ui.main.bot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.example.shounak.bargainingbot.R
import com.example.shounak.bargainingbot.data.provider.PreferenceProvider
import com.example.shounak.bargainingbot.ui.base.ScopedFragment
import kotlinx.android.synthetic.main.bot_fragment.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance

class BotFragment : ScopedFragment(), KodeinAware {
    override val kodein: Kodein by closestKodein()
    private val viewModelFactory: BotViewModelFactory by instance()

    private lateinit var viewModel: BotViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bot_fragment, container, false)


    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(BotViewModel::class.java)


//        TODO("The following code should be moved to main activity and set the data in nav drawer.")

        val pref = PreferenceProvider.getPrefrences(this@BotFragment.context!!)
        val uid = pref.getString(PreferenceProvider.USER_ID, "Not Available")

        user_id_textview.text = uid
    }


}


