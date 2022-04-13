package androidx.build.gradle.gcpbuildcache

import org.gradle.caching.BuildCacheService
import org.gradle.caching.BuildCacheServiceFactory

class GcpBuildCacheServiceFactory : BuildCacheServiceFactory<GcpBuildCache> {
    override fun createBuildCacheService(
        buildCache: GcpBuildCache,
        describer: BuildCacheServiceFactory.Describer,
    ): BuildCacheService {
        describer
            .type("GCP-backed")
            .config("projectId", buildCache.projectId)
            .config("bucketName", buildCache.bucketName)

        return GcpBuildCacheService(buildCache.projectId, buildCache.bucketName)
    }
}
