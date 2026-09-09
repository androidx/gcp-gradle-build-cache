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

package androidx.build.gradle.core

import org.gradle.api.provider.Property
import org.gradle.caching.configuration.AbstractBuildCache

/**
 * Gradle Build Cache that uses a cloud storage provider as a backing to load and store Gradle cache results.
 */
abstract class RemoteGradleBuildCache : AbstractBuildCache() {

    /**
     * Runtime switch that determines whether the build attempts to upload entries
     * to the remote build cache.
     *
     * Unlike [org.gradle.caching.configuration.BuildCache.isPush], this flag is **not**
     * part of the Gradle Configuration Cache model’s fingerprint and is only used at execution
     * time.
     *
     * Keeping `push = true` in `settings.gradle[.kts]` and gating uploads with this property allows you to
     * toggle pushing without invalidating the configuration cache
     *
     */
    abstract val runtimePush: Property<Boolean>

    /**
     * The name of the bucket that is used to store all the gradle cache entries.
     * This essentially becomes the root of all cache entries.
     */
    lateinit var bucketName: String

    /**
     * The type of credentials to use to connect to authenticate to your project instance.
     */
    abstract val credentials: Credentials
}
