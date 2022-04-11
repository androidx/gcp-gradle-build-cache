package androidx.build.gradle.gcpbuildcache

import com.google.cloud.storage.Bucket
import com.google.cloud.storage.StorageOptions
import org.gradle.caching.BuildCacheService
import org.gradle.caching.BuildCacheServiceFactory

class GcpBuildCacheServiceFactory : BuildCacheServiceFactory<GcpBuildCache> {
  override fun createBuildCacheService(
    buildCache: GcpBuildCache,
    describer: BuildCacheServiceFactory.Describer,
  ): BuildCacheService {
    describer
      .type("GCP-backed")
      .config("bucketName", buildCache.bucketName)

    return GcpBuildCacheService(getStorageBucket(buildCache.bucketName))
  }

  private fun getStorageBucket(bucketName: String): Bucket {
    return StorageOptions.getDefaultInstance().service.get(bucketName)
  }
}