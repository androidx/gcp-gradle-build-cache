# Release notes for GCP backed Gradle Remote Cache

## 1.0.0-alpha06

- Warn when a user incorrectly configures GCP bucket to be used for the cache.

## 1.0.0-alpha05

- Downloads `Blob`s to an intermediate `Buffer` or a `File` depending on the size of the blob.
- The underlying `FileHandleInputStream` gets cleaned up automatically after the `InputStream` is closed.
- This way we can avoid flakes from the build cache, that is caused by intermittent `StorageException`s 
- Retry on fetches given they are being fetched to an intermediate location.

## 1.0.0-alpha04

- Handles exceptions when fetching `BlobInfo`s and `ReadChannel`s from the storage service.

## 1.0.0-alpha03

- Fixes issue [19](https://github.com/androidx/gcp-gradle-build-cache/issues/19).
    - Remove retry-ing of RPCs.
    - When writes fail, we fail silently and treat subsequent reads as a cache-miss.
    - Set the `chunkSize` for reads to equal the size of the `Blob`.
      This way, we only make 1 RPC per blob input stream. This is safe because
      the size of the objects in cache are not very large.