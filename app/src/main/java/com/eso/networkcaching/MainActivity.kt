package com.eso.networkcaching

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

   // lateinit var testView:TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //testView = findViewById(R.id.updatedAtTv)
        loadGithubAccount()

    }

    private fun loadGithubAccount() {

        // Create a cache object
        val cacheSize = 10 * 1024 * 1024 // 10 MB
        val httpCacheDirectory = File(cacheDir, "http-cache")
        val cache = Cache(httpCacheDirectory, cacheSize.toLong())

        // create a network cache interceptor, setting the max age to 1 minute
        val networkCacheInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())

            var cacheControl = CacheControl.Builder()
                .maxAge(1, TimeUnit.MINUTES)
                .build()

            response.newBuilder()
                .header("Cache-Control", cacheControl.toString())
                .build()
        }

        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY


        // Create the httpClient, configure it
        // with cache, network cache interceptor and logging interceptor
        val httpClient = OkHttpClient.Builder()
            .cache(cache)
            .addNetworkInterceptor(networkCacheInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        // Create the Retrofit with the httpClient
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(httpClient)
            .build()

        // Build the gitHubApi with Retrofit and do the network request
        val githubApi = retrofit.create(GithubApi::class.java)
        githubApi.getGithubAccountObservable("google")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableObserver<Response<GithubAccount>>() {
                override fun onNext(response: Response<GithubAccount>) {

                   // testView.text = response.raw().cacheResponse!!.message
                    if (response.raw().cacheResponse != null) {

                        Log.d("Network", "response came from cache"+response.body().toString())
                    }

                    if (response.raw().networkResponse != null) {
                        Log.d("Network", "response came from server")
                    }

                    Toast.makeText(applicationContext, response.body().toString(), Toast.LENGTH_SHORT).show()
                }

                override fun onComplete() {}

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                }
            })
    }
}