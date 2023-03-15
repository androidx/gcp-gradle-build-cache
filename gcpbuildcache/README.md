# GCP backed Gradle Remote Cache

An implementation of the Gradle Remote Cache that's backed by Google Cloud Storage buckets.

## Using the plugin

In your `settings.gradle.kts` file add the following

```kotlin
plugins {
    id("androidx.build.gradle.gcpbuildcache") version "1.0.0-beta01"
}

import androidx.build.gradle.gcpbuildcache.GcpBuildCache
import androidx.build.gradle.gcpbuildcache.GcpBuildCacheServiceFactory
import androidx.build.gradle.gcpbuildcache.ExportedKeyGcpCredentials

buildCache {
    registerBuildCacheService(GcpBuildCache::class, GcpBuildCacheServiceFactory::class)
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
    id("androidx.build.gradle.gcpbuildcache") version "1.0.0-beta01"
}

import androidx.build.gradle.gcpbuildcache.GcpBuildCache
import androidx.build.gradle.gcpbuildcache.GcpBuildCacheServiceFactory
import androidx.build.gradle.gcpbuildcache.ExportedKeyGcpCredentials

buildCache {
    registerBuildCacheService(GcpBuildCache, GcpBuildCacheServiceFactory)
    remote(GcpBuildCache) {
        projectId = "projectName"
        bucketName = "storageBucketName"
        credentials = new ExportedKeyGcpCredentials(new File("path/to/credentials.json"))
        push = inCi
    }
}
```

## Setting up Google Cloud Platform project

1. [Install `gcloud` CLI on your machine](https://cloud.google.com/sdk/docs/install)
2. Create a GCP project `YOUR-GCP-PROJECT` and [set up billing](https://cloud.google.com/billing/docs/how-to/manage-billing-account#create_a_new_billing_account).
3. Create a Google Cloud Storage bucket
```bash
gsutil mb –p YOUR-GCP-PROJECT gs://YOUR-BUCKET-NAME
```
4. Create IAM roles for read and read/write
```bash
gcloud iam roles create CacheReadWrite --project=YOUR-GCP-PROJECT --title=CacheReadWrite --description="Have access to read and write to remote Gradle cache" --permissions=storage.buckets.get,storage.objects.create,storage.objects.delete,storage.objects.get,storage.objects.getIamPolicy,storage.objects.list
gcloud iam roles create CacheRead --project=YOUR-GCP-PROJECT --title=CacheRead --description="Have access to read from remote Gradle cache" --permissions=storage.buckets.get,storage.objects.get,storage.objects.getIamPolicy,storage.objects.list
```
5. Create IAM Service Accounts
```bash
gcloud iam service-accounts create cache-read-write  --project=YOUR-GCP-PROJECT
gcloud iam service-accounts create cache-read  --project=YOUR-GCP-PROJECT
```
6. Grant the service account roles that we just created
```bash
gcloud projects add-iam-policy-binding YOUR-GCP-PROJECT --member=serviceAccount:cache-read@YOUR-GCP-PROJECT.iam.gserviceaccount.com --role=projects/YOUR-GCP-PROJECT/roles/CacheRead
gcloud projects add-iam-policy-binding YOUR-GCP-PROJECT --member=serviceAccount:cache-read-write@YOUR-GCP-PROJECT.iam.gserviceaccount.com --role=projects/YOUR-GCP-PROJECT/roles/CacheReadWrite
```
7. Use `YOUR-GCP-PROJECT` and `YOUR-BUCKET-NAME` in the plugin configuration with exported service account credentials.
