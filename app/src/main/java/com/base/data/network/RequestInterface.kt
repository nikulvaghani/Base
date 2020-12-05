package com.base.data.network


import com.base.BuildConfig
import com.base.data.pref.SharedPref
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Created by Nikul on 14-08-2020.
 */
interface RequestInterface {

//    @POST("sendOTP")
//    fun sendOTP(@Body body: HashMap<String, Any>): Observable<OTP>

    companion object {
        const val DOMAIN = "www.domain.com"
        const val API_BASE_URL = "http://$DOMAIN/API/"
        private const val TIME_OUT = 60L
        private const val DEVICE_TYPE = "android" // Android, iOS

        @Volatile
        private var INSTANCE: RequestInterface? = null

        @Volatile
        private var INSTANCE_WITHOUT_ENCRYPT: RequestInterface? = null

        fun getInstance(): RequestInterface {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: create().also {
                    INSTANCE = it
                }
            }
        }

        private fun create(): RequestInterface {
            val httpClient = OkHttpClient.Builder()
            httpClient.connectTimeout(TIME_OUT, TimeUnit.SECONDS)
            httpClient.readTimeout(TIME_OUT, TimeUnit.SECONDS)
            httpClient.writeTimeout(TIME_OUT, TimeUnit.SECONDS)


            httpClient.addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .method(original.method, original.body)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", SharedPref.authToken)
                    .header("device-type", DEVICE_TYPE)
                chain.proceed(requestBuilder.build())
            }

            if (BuildConfig.DEBUG) {
                val logging =
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                httpClient.addInterceptor(logging)
            }

            return Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .client(httpClient.build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder().setLenient().create()
                    )
                )
                .addConverterFactory(ScalarsConverterFactory.create())
                .build().create(RequestInterface::class.java)
        }
    }
}