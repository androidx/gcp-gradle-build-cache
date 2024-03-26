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

package androidx.build.gradle.s3buildcache

import androidx.build.gradle.core.RemoteGradleBuildCache

/**
 * Gradle Build Cache that uses AWS S3 buckets as a backing for load and store Gradle results.
 */
abstract class S3BuildCache : RemoteGradleBuildCache() {

    /**
     * The AWS region the S3 bucket is located in.
     */
    lateinit var region: String

    /**
     * Whether to use reduced redundancy.
     * When using S3 Express One Zone, set to false
     * @see <a href="https://aws.amazon.com/s3/reduced-redundancy/">Reduced Redundancy</a>
     * */
    var reducedRedundancy: Boolean = true

    /**
     * The type of credentials to use to connect to AWS.
     */
    override var credentials: S3Credentials = DefaultS3Credentials
}
