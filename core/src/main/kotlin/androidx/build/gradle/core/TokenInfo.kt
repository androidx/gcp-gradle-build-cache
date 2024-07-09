/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package androidx.build.gradle.core

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query

internal fun gson(config: GsonBuilder.() -> Unit = {}): Gson {
    val builder = GsonBuilder()
    config.invoke(builder)
    return builder.create()
}
interface TokenInfoService {
    @GET("/oauth2/v1/tokeninfo")
    fun tokenInfo(@Query("access_token") accessToken: String): Call<Unit>

    companion object {
        fun tokenService(): TokenInfoService {
            val httpClient = OkHttpClient
                .Builder()
                .addInterceptor(NetworkErrorInterceptor())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://www.googleapis.com")
                .addConverterFactory(GsonConverterFactory.create(gson()))
                .client(httpClient)
                .build()

            return retrofit.create()
        }
    }
}