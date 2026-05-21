package com.finploit.android.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// NotificationHelper usa @Inject constructor — Hilt injeta automaticamente.
// Configuration.Provider é implementado por FinploitApp — WorkManager usa diretamente.
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule
