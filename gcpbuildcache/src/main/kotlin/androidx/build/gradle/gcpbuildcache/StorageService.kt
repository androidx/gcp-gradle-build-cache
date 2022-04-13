package androidx.build.gradle.gcpbuildcache

import java.io.Closeable
import java.io.InputStream

interface StorageService : Closeable {
    /**
     * The name of the project.
     */
    val projectId: String

    /**
     * The name of the root bucket where the cache is going to be stored.
     */
    val bucketName: String

    /**
     * `true` if the underlying storage service supports writes and deletes.
     */
    val isPush: Boolean

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
}
