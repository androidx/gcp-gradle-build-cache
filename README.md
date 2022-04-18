# GCP backed Gradle Remote Cache

An implementation of the Gradle Remote Cache that's backed by Google Cloud Storage buckets.

## Using the plugin

In your `settings.gradle.kts` file add the following

```kotlin
plugins {
    id("androidx.build.gradle.gcpbuildcache") version "1.0.0-alpha02"
}

import androidx.build.gradle.gcpbuildcache.GcpBuildCache
import androidx.build.gradle.gcpbuildcache.ExportedKeyGcpCredentials

buildCache {
    remote(GcpBuildCache::class) {
        projectId = "foo"
        bucketName = "bar"
        credentials = ExportedKeyGcpCredentials(File("path/to/credentials.json"))
        isPush = inCi
    }
}
```

- `projectId`, `bucketName` are required
- `credentials` defaults to `ApplicationDefaultGcpCredentials`, but can also be set to `ExportedKeyGcpCredentials`
- `isPush` defaults to `false`.

---

If you are using Groovy, then you should do the following:

```groovy
plugins {
    id("androidx.build.gradle.gcpbuildcache") version "1.0.0-alpha02"
}

import androidx.build.gradle.gcpbuildcache.GcpBuildCache
import androidx.build.gradle.gcpbuildcache.ExportedKeyGcpCredentials

buildCache {
    remote(GcpBuildCache) {
        projectId = "projectName"
        bucketName = "storageBucketName"
        credentials = new ExportedKeyGcpCredentials(new File("path/to/credentials.json"))
        push = inCi
    }
}
```
