package com.finploit.android.di

import com.finploit.android.BuildConfig
import com.finploit.android.data.api.AnalysisApi
import com.finploit.android.data.api.AuthApi
import com.finploit.android.data.api.BankAccountApi
import com.finploit.android.data.api.CoupleApi
import com.finploit.android.data.api.AuthInterceptor
import com.finploit.android.data.api.UnauthorizedInterceptor
import com.finploit.android.data.api.FinanceCategoryApi
import com.finploit.android.data.api.FinanceApi
import com.finploit.android.data.api.FiscalApi
import com.finploit.android.data.api.GrocerySearchApi
import com.finploit.android.data.api.MealPlannerApi
import com.finploit.android.data.api.PantryApi
import com.finploit.android.data.api.ResponseUnwrapInterceptor
import com.finploit.android.data.api.GoalApi
import com.finploit.android.data.api.RecurringApi
import com.finploit.android.data.api.ShoppingApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LongTimeout

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor, unauthorizedInterceptor: UnauthorizedInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
                    else HttpLoggingInterceptor.Level.NONE
            if (BuildConfig.DEBUG) redactHeader("Authorization")
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(ResponseUnwrapInterceptor())
            .addInterceptor(unauthorizedInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @LongTimeout
    fun provideLongTimeoutOkHttpClient(authInterceptor: AuthInterceptor, unauthorizedInterceptor: UnauthorizedInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
                    else HttpLoggingInterceptor.Level.NONE
            if (BuildConfig.DEBUG) redactHeader("Authorization")
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(ResponseUnwrapInterceptor())
            .addInterceptor(unauthorizedInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @LongTimeout
    fun provideLongTimeoutRetrofit(@LongTimeout okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideCoupleApi(retrofit: Retrofit): CoupleApi =
        retrofit.create(CoupleApi::class.java)

    @Provides
    @Singleton
    fun provideBankAccountApi(retrofit: Retrofit): BankAccountApi =
        retrofit.create(BankAccountApi::class.java)

    @Provides
    @Singleton
    fun provideFinanceApi(retrofit: Retrofit): FinanceApi =
        retrofit.create(FinanceApi::class.java)

    @Provides
    @Singleton
    fun provideFiscalApi(retrofit: Retrofit): FiscalApi =
        retrofit.create(FiscalApi::class.java)

    @Provides
    @Singleton
    fun provideFinanceCategoryApi(retrofit: Retrofit): FinanceCategoryApi =
        retrofit.create(FinanceCategoryApi::class.java)

    @Provides
    @Singleton
    fun provideGoalApi(retrofit: Retrofit): GoalApi =
        retrofit.create(GoalApi::class.java)

    @Provides
    @Singleton
    fun provideShoppingApi(retrofit: Retrofit): ShoppingApi =
        retrofit.create(ShoppingApi::class.java)

    @Provides
    @Singleton
    fun provideRecurringApi(retrofit: Retrofit): RecurringApi =
        retrofit.create(RecurringApi::class.java)

    @Provides
    @Singleton
    fun provideAnalysisApi(@LongTimeout retrofit: Retrofit): AnalysisApi =
        retrofit.create(AnalysisApi::class.java)

    @Provides
    @Singleton
    fun provideMealPlannerApi(@LongTimeout retrofit: Retrofit): MealPlannerApi =
        retrofit.create(MealPlannerApi::class.java)

    @Provides
    @Singleton
    fun provideGrocerySearchApi(@LongTimeout retrofit: Retrofit): GrocerySearchApi =
        retrofit.create(GrocerySearchApi::class.java)

    @Provides
    @Singleton
    fun providePantryApi(retrofit: Retrofit): PantryApi =
        retrofit.create(PantryApi::class.java)
}
