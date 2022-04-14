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

import org.gradle.api.file.RegularFileProperty
import org.gradle.caching.BuildCacheEntryReader
import org.gradle.caching.BuildCacheEntryWriter
import org.gradle.caching.BuildCacheKey
import org.gradle.caching.BuildCacheService
import java.io.ByteArrayOutputStream

/**
 * The service that responds to Gradle's request to load and store results for a given
 * [BuildCacheKey].
 *
 * @param projectId The Google Cloud Platform project id, that can be used for billing.
 * @param bucketName The name of the bucket that is used to store all the gradle cache entries.
 * This essentially becomes the root of all cache entries.
 */
internal class GcpBuildCacheService(
    private val projectId: String,
    private val bucketName: String,
    serviceAccountPath: RegularFileProperty,
    isPush: Boolean,
    isEnabled: Boolean,
    inTestMode: Boolean = false
) : BuildCacheService {

    private val storageService = if (inTestMode) {
        // Use an implementation backed by the File System when in test mode.
        FileSystemStorageService(projectId, bucketName, isPush, isEnabled)
    } else {
        GcpStorageService(projectId, bucketName, serviceAccountPath, isPush, isEnabled)
    }

    override fun close() {
        // Does nothing
    }

    override fun load(key: BuildCacheKey, reader: BuildCacheEntryReader): Boolean {
        val cacheKey = key.blobKey()
        val input = storageService.load(cacheKey) ?: return false
        reader.readFrom(input)
        return true
    }

    override fun store(key: BuildCacheKey, writer: BuildCacheEntryWriter) {
        val cacheKey = key.blobKey()
        val output = ByteArrayOutputStream()
        output.use {
            writer.writeTo(output)
        }
        storageService.store(cacheKey, output.toByteArray())
    }

    companion object {
        // Build Cache Key Helpers
        private val SLASHES = """"/+""".toRegex()

        internal fun BuildCacheKey.blobKey(): String {
            // Slashes are special when it comes to cache keys.
            // Under the hood, they are treated as a "folder/file" as long as there is
            // a single `/`.
            return hashCode.replace(SLASHES, "/")
        }
    }
}
