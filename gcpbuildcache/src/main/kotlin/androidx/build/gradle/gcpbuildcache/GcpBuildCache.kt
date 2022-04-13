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
