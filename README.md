# GCP backed Gradle Remote Cache

An implementation of the Gradle Remote Cache that's backed by Google Cloud Storage buckets.

## Using the plugin

In your `settings.gradle.kts` file add the following

```kotlin
plugins {
    id("androidx.build.gradle.gcpbuildcache") version "1.0.0-alpha01"
}

buildCache {
    remote(androidx.build.gradle.gcpbuildcache.GcpBuildCache::class) {
        projectId = "foo"
        bucketName = "bar"
        serviceAccountPath = File("path/to/credentials.json")
        isPush = inCi
  }
}
```

- `projectId`, `bucketName`, and `serviceAccountPath` are required
- `isPush` defaults to `false`.

## Development

Set up the following environment variables for service account credentials to run all the test.

```bash
# Gradle Cache Service Account Path
export GRADLE_CACHE_SERVICE_ACCOUNT_PATH=$HOME/.gradle-cache/androidx-dev-prod-build-cache-writer.json
```
