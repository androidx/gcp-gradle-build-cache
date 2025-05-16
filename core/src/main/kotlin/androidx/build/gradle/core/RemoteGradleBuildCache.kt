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

import org.gradle.caching.configuration.AbstractBuildCache

/**
 * Gradle Build Cache that uses a cloud storage provider as a backing to load and store Gradle cache results.
 */
abstract class RemoteGradleBuildCache : AbstractBuildCache() {

    /**
     * The name of the bucket that is used to store all the gradle cache entries.
     * This essentially becomes the root of all cache entries.
     */
    lateinit var bucketName: String

    /**
     * The prefix to use when storing cache entries in the bucket.
     * It becomes new root for all cache entries.
     * If not specified, the cache entries will be stored at the root of the bucket.
     */
    lateinit var prefix: String

    /**
     * The type of credentials to use to connect to authenticate to your project instance.
     */
    abstract val credentials: Credentials
}
