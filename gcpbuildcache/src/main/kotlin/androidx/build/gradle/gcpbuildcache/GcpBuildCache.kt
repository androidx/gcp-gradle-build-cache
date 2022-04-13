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

import org.gradle.caching.configuration.AbstractBuildCache

/**
 * Gradle Build Cache that uses GCP buckets as a backing for load and store
 * Gradle results.
 */
open class GcpBuildCache(
) : AbstractBuildCache() {
    /**
     * The Google Cloud Platform project id, that can be used for billing.
     */
    lateinit var projectId: String

    /**
     * The name of the bucket that is used to store all the gradle cache entries.
     * This essentially becomes the root of all cache entries.
     */
    lateinit var bucketName: String
}
