package com.lenohutapea.plantidentifier.di

import android.content.Context
import com.lenohutapea.plantidentifier.BuildConfig
import com.lenohutapea.plantidentifier.data.DiagnosisRepository
import com.lenohutapea.plantidentifier.data.ServerSettings
import com.lenohutapea.plantidentifier.data.local.AppDatabase
import com.lenohutapea.plantidentifier.data.remote.BaseUrlInterceptor
import com.lenohutapea.plantidentifier.data.remote.PlantApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Manual dependency injection: one instance of each dependency for the whole app. */
class AppContainer(context: Context) {
    val settings = ServerSettings(context.applicationContext)

    /** Plain client, also used by the Settings screen to test an address before saving it. */
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: PlantApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(httpClient.newBuilder().addInterceptor(BaseUrlInterceptor(settings)).build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PlantApi::class.java)

    private val database = AppDatabase.build(context)

    val repository = DiagnosisRepository(context.applicationContext, api, database.diagnosisDao())
}
