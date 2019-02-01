package com.example.shounak.bargainingbot

import android.app.Application
import com.example.shounak.bargainingbot.data.db.BotDatabase
import com.example.shounak.bargainingbot.data.network.UserFirestoreDatabase
import com.example.shounak.bargainingbot.data.network.UserNetworkDataSource
import com.example.shounak.bargainingbot.data.network.UserNetworkDataSourceImpl
import com.example.shounak.bargainingbot.data.repository.UserRepository
import com.example.shounak.bargainingbot.data.repository.UserRepositoryImpl
import com.example.shounak.bargainingbot.ui.login.LoginViewModelFactory
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

/**
 * Created by Shounak on 30-Jan-19
 */
class BotApplication : Application() , KodeinAware {
        override val kodein = Kodein.lazy {
        import(androidXModule(this@BotApplication))

        bind() from singleton { BotDatabase(instance()) }
        bind() from singleton { instance<BotDatabase>().userDao() }
        bind() from singleton { UserFirestoreDatabase() }
        bind<UserNetworkDataSource>() with singleton { UserNetworkDataSourceImpl(instance()) }
        bind<UserRepository>() with singleton { UserRepositoryImpl(instance(),instance()) }
        bind() from provider { LoginViewModelFactory(instance()) }
    }


}