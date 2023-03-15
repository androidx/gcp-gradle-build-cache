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

package androidx.build.gradle.core

import org.gradle.caching.BuildCacheKey

fun BuildCacheKey.blobKey(): String {
    val slashes = """"/+""".toRegex()
    // Slashes are special when it comes to cache keys.
    // Under the hood, they are treated as a "folder/file" as long as there is
    // a single `/`.
    return hashCode.replace(slashes, "/")
}
