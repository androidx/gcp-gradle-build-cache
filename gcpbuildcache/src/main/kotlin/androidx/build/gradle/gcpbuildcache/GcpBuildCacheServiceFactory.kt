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

import org.gradle.caching.BuildCacheService
import org.gradle.caching.BuildCacheServiceFactory

/**
 * Factory used by Gradle to create GcpBuildCache instances.
 */
class GcpBuildCacheServiceFactory : BuildCacheServiceFactory<GcpBuildCache> {
    override fun createBuildCacheService(
        buildCache: GcpBuildCache,
        describer: BuildCacheServiceFactory.Describer,
    ): BuildCacheService {
        describer
            .type("GCP-backed")
            .config("projectId", buildCache.projectId)
            .config("bucketName", buildCache.bucketName)
            .config("prefix", buildCache.prefix)
            .config("isPushSupported", "${buildCache.isPush}")
            .config("isEnabled", "${buildCache.isEnabled}")
            .config(
                "usingExportedKeyCredentials",
                "${buildCache.credentials is ExportedKeyGcpCredentials}"
            )

        val service = GcpBuildCacheService(
            projectId = buildCache.projectId,
            bucketName = buildCache.bucketName,
            prefix = buildCache.prefix,
            gcpCredentials = buildCache.credentials,
            messageOnAuthenticationFailure = buildCache.messageOnAuthenticationFailure,
            isPush = buildCache.isPush,
            isEnabled = buildCache.isEnabled
        )
        service.validateConfiguration()
        return service
    }
}
