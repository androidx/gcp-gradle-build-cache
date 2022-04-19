/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.gradle.gcpbuildcache

import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.cloud.ServiceOptions
import com.google.cloud.http.HttpTransportOptions
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Sets up transport options and disables logging.
 */
internal class GcpTransportOptions(builder: Builder) : HttpTransportOptions(builder) {
    override fun getHttpRequestInitializer(serviceOptions: ServiceOptions<*, *>?): HttpRequestInitializer {
        // Delegate to the underlying initializer.
        val initializer = super.getHttpRequestInitializer(serviceOptions)
        return HttpRequestInitializer {
            it.isLoggingEnabled = false
            initializer.initialize(it)
        }
    }

    companion object {
        init {
            // Force log level to config
            val logger = Logger.getLogger(HttpTransport::class.java.name)
            logger.level = Level.SEVERE
        }
    }
}
