package com.example.walkingpark.retrofit2

import com.example.walkingpark.MainActivity
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object InstanceParkApi {

    private const val BASE_URL = "http://api.data.go.kr/openapi/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(UnsafeOkHttpClient.unsafeOkHttpClient().build())
            .build()
    }

    val api:PublicApiController by lazy {
        retrofit.create(PublicApiController::class.java)
    }

}