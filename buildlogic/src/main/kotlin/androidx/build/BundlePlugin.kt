/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package androidx.build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata

class BundlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val bundleInside = project.configurations.create("bundleInside") {
            it.isTransitive = false
            it.isCanBeResolved = true
        }
        project.afterEvaluate {
            project.configurations.getByName("compileOnly").extendsFrom(bundleInside)
            project.configurations.getByName("testImplementation").extendsFrom(bundleInside)
            val jarsToBundle = bundleInside.incoming.artifactView {  }.files
            project.tasks.named("jar").configure { jarTask ->
                jarTask as Jar
                jarTask.from(project.provider { jarsToBundle.map { if (it.isDirectory) { it } else { project.zipTree(it) } } })
            }
            project.tasks.withType(PluginUnderTestMetadata::class.java).configureEach {
                it.pluginClasspath.from(jarsToBundle)
            }
        }
    }
}