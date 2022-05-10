# Release notes for GCP backed Gradle Remote Cache

## 1.0.0-alpha03

- Fixes issue [19](https://github.com/androidx/gcp-gradle-build-cache/issues/19).
    - Remove retry-ing of RPCs.
    - When writes fail, we fail silently and treat subsequent reads as a cache-miss.
    - Set the `chunkSize` for reads to equal the size of the `Blob`.
      This way, we only make 1 RPC per blob input stream. This is safe because
      the size of the objects in cache are not very large.