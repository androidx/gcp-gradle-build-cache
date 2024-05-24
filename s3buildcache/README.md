# AWS S3 backed Gradle Remote Cache

An implementation of the Gradle Remote Cache that's backed by AWS S3 buckets.

## Using the plugin

In your `settings.gradle(.kts)` file add the following

```kotlin
plugins {
  id("androidx.build.gradle.s3buildcache") version "1.0.0-alpha06"
}

import androidx.build.gradle.s3buildcache.S3BuildCache
import androidx.build.gradle.s3buildcache.S3BuildCacheServiceFactory
import androidx.build.gradle.s3buildcache.ExportedS3Credentials

buildCache {
  registerBuildCacheService(S3BuildCache::class, S3BuildCacheServiceFactory::class)
  remote(S3BuildCache::class) {
    region = "s3-bucket-region"
    bucketName = "s3-bucket-name"
    credentials = ExportedS3Credentials("your-aws-access-key-id", "your-aws-secret-key")
    isPush = System.getenv().containsKey("CI")
  }
}
```

- `region`, `bucketName` are required.
- `credentials` defaults to `DefaultS3Credentials`, but can also be set to `ExportedS3Credentials`, `ProfileS3Credentials`, or `SpecificCredentialsProvider`.
- `isPush` defaults to `false`.

---

If you are using Groovy, then you should do the following:

```groovy
plugins {
  id("androidx.build.gradle.s3buildcache") version "1.0.0-alpha06"
}

import androidx.build.gradle.s3buildcache.ExportedS3Credentials
import androidx.build.gradle.s3buildcache.S3BuildCache
import androidx.build.gradle.s3buildcache.S3BuildCacheServiceFactory

buildCache {
  registerBuildCacheService(S3BuildCache, S3BuildCacheServiceFactory)
  remote(S3BuildCache) {
    region = "s3-bucket-region"
    bucketName = "s3-bucket-name"
    credentials = new ExportedS3Credentials("your-aws-access-key-id", "your-aws-secret-key")
    push = System.getenv().containsKey("CI")
  }
}
```
