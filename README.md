# GCP backed Gradle Remote Cache

An implementation of the Gradle Remote Cache that's backed by Google Cloud Storage buckets.

## Development

Set up the following environment variables for service account credentials.

```bash
# Gradle Cache Service Account Path
export ANDROIDX_GRADLE_SERVICE_ACCOUNT_PATH=$HOME/.gradle-cache/androidx-dev-prod-build-cache-writer.json
```
