# GCP backed Gradle Remote Cache

An implementation of the Gradle Remote Cache that's backed by Google Cloud Storage buckets.

## Using the plugin

In your `settings.gradle.kts` file add the following

```kotlin
plugins {
    id("androidx.build.gradle.gcpbuildcache") version "1.0.0-alpha01"
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

## Development

Set up the following environment variables for service account credentials to run all the test.

```bash
# Gradle Cache Service Account Path
export GRADLE_CACHE_SERVICE_ACCOUNT_PATH=$HOME/.gradle-cache/androidx-dev-prod-build-cache-writer.json
```
