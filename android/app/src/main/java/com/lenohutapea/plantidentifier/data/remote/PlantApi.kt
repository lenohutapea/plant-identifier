package com.lenohutapea.plantidentifier.data.remote

import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface PlantApi {
    @Multipart
    @POST("predict")
    suspend fun predict(@Part image: MultipartBody.Part): PredictResponse

    @GET("plants/{id}")
    suspend fun plant(@Path("id") id: String): PlantInfo
}
