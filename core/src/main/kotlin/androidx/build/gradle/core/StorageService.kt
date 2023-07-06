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

import java.io.Closeable
import java.io.InputStream

interface StorageService : Closeable {
    /**
     * The name of the root bucket where the cache is going to be stored.
     */
    val bucketName: String

    /**
     * `true` if the underlying storage service supports writes and deletes.
     */
    val isPush: Boolean

    /**
     * If `true`, use the underlying storage service.
     */
    val isEnabled: Boolean

    /**
     * Loads an entity from Storage.
     * @param cacheKey is the unique key that can identify a resource that needs to be loaded.
     * @return an [InputStream] if there is a storage-hit. `null` if it's a storage-miss.
     */
    fun load(cacheKey: String): InputStream?

    /**
     * Stores an entity into the storage.
     * @param cacheKey is the unique key that can identify a resource that needs to be stored.
     */
    fun store(cacheKey: String, contents: ByteArray): Boolean

    /**
     * Removes an entity from storage.
     * @param cacheKey is the unique key that can identify a resource that needs to be removed.
     */
    fun delete(cacheKey: String): Boolean

    /**
     * Checks of the current configuration is valid. Throws an exception if the state is bad.
     */
    fun validateConfiguration()
}
