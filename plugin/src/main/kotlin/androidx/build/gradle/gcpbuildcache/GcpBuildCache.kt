package androidx.build.gradle.gcpbuildcache

import org.gradle.caching.configuration.AbstractBuildCache

/**
 * Gradle Build Cache that uses GCP buckets as a backing for load and store
 * Gradle results.
 */
class GcpBuildCache : AbstractBuildCache() {
}