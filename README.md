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
    isPush = true
    isEnabled = true
  }
}
```

`projectId` and `bucketName` are required. `isPush` and `isEnabled` defaults to
`true`.

Then also set `ANDROIDX_GRADLE_SERVICE_ACCOUNT_PATH` environment variable that
points to a file containing service account credentials in a json format.

## Development

Set up the following environment variables for service account credentials.

```bash
# Gradle Cache Service Account Path
export ANDROIDX_GRADLE_SERVICE_ACCOUNT_PATH=$HOME/.gradle-cache/androidx-dev-prod-build-cache-writer.json
```
