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

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/**
 * A Gradle settings plugin that registers [S3BuildCache]
 */
class S3GradleBuildCachePlugin : Plugin<Settings> {

    override fun apply(settings: Settings) {
        settings.buildCache.registerBuildCacheService(
            S3BuildCache::class.java,
            S3BuildCacheServiceFactory::class.java
        )
    }
}
