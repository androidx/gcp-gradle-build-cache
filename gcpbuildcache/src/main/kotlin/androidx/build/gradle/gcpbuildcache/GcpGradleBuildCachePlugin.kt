package androidx.build.gradle.gcpbuildcache

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/**
 * A Gradle settings plugin that registers [GcpBuildCache]
 */
class GcpGradleBuildCachePlugin: Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.buildCache.registerBuildCacheService(
            GcpBuildCache::class.java,
            GcpBuildCacheServiceFactory::class.java
        )
    }
}
