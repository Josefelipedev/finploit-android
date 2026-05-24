package com.finploit.android.di

import android.content.Context
import androidx.room.Room
import com.finploit.android.data.local.dao.BudgetLimitDao
import com.finploit.android.data.local.dao.TransactionCacheDao
import com.finploit.android.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "finploit.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideBudgetLimitDao(db: AppDatabase): BudgetLimitDao = db.budgetLimitDao()

    @Provides
    fun provideTransactionCacheDao(db: AppDatabase): TransactionCacheDao = db.transactionCacheDao()
}
