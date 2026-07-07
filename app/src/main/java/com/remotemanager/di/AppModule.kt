package com.remotemanager.di

import android.content.Context
import androidx.room.Room
import com.remotemanager.data.db.AppDatabase
import com.remotemanager.data.repository.ServerRepository
import com.remotemanager.data.security.CryptoManager
import com.remotemanager.ui.viewmodel.ServerListViewModel
import com.remotemanager.ui.viewmodel.SftpViewModel
import com.remotemanager.ui.viewmodel.SshViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    single { provideDatabase(androidContext()) }
    single { get<AppDatabase>().serverDao() }
    singleOf(::CryptoManager)
    singleOf(::ServerRepository)

    viewModel { ServerListViewModel(get()) }
    viewModel { (serverId: Long) -> SshViewModel(serverId, get()) }
    viewModel { (serverId: Long) -> SftpViewModel(serverId, get()) }
}

private fun provideDatabase(context: Context): AppDatabase {
    return Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "remote_manager.db"
    ).build()
}
