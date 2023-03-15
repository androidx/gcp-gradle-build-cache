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

package androidx.build.gradle.s3buildcache

import org.gradle.caching.BuildCacheService
import org.gradle.caching.BuildCacheServiceFactory

/**
 * Factory used by Gradle to create S3BuildCache instances.
 */
class S3BuildCacheServiceFactory : BuildCacheServiceFactory<S3BuildCache> {

    override fun createBuildCacheService(
        buildCache: S3BuildCache,
        describer: BuildCacheServiceFactory.Describer
    ): BuildCacheService {
        describer
            .type("AWS-S3-backed")
            .config("region", buildCache.region)
            .config("bucketName", buildCache.bucketName)
            .config("reducedRedundancy", "${buildCache.reducedRedundancy}")
            .config("isPushSupported", "${buildCache.isPush}")
            .config("isEnabled", "${buildCache.isEnabled}")
            .config("credentialsType", "${buildCache.credentials}")

        val service = S3BuildCacheService(
            region = buildCache.region,
            bucketName = buildCache.bucketName,
            isPush = buildCache.isPush,
            isEnabled = buildCache.isEnabled,
            reducedRedundancy = buildCache.reducedRedundancy,
            credentials = buildCache.credentials
        )
        service.validateConfiguration()
        return service
    }
}
