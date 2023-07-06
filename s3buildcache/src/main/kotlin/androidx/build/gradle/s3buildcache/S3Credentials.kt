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

import androidx.build.gradle.core.Credentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider

/**
 * [DefaultS3Credentials] or [ExportedS3Credentials] to use to authenticate to AWS.
 */
sealed interface S3Credentials : Credentials

/**
 * Use DefaultCredentialsProvider to authenticate to AWS.
 */
object DefaultS3Credentials : S3Credentials

/**
 * Use a specific credentials provider
 * @see <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html">Using credentials</a>
 * */
class SpecificCredentialsProvider(val provider: AwsCredentialsProvider) : S3Credentials

/**
 * Use provided keys to authenticate to AWS.
 */
class ExportedS3Credentials(val awsAccessKeyId: String, val awsSecretKey: String) : S3Credentials
