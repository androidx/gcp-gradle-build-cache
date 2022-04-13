package androidx.build.gradle.gcpbuildcache

import org.gradle.caching.configuration.AbstractBuildCache

/**
 * Gradle Build Cache that uses GCP buckets as a backing for load and store
 * Gradle results.
 *
 * @param projectId The Google Cloud Platform project id, that can be used for billing.
 * @param bucketName The name of the bucket that is used to store all the gradle cache entries.
 * This essentially becomes the root of all cache entries.
 */
open class GcpBuildCache(
) : AbstractBuildCache() {
  lateinit var projectId: String
  lateinit var bucketName: String
}