/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.gradle.gcpbuildcache

import androidx.build.gradle.core.Credentials
import java.io.File

/**
 * [ApplicationDefaultGcpCredentials] or [ExportedKeyGcpCredentials] to use
 * to authenticate to Google Cloud Platform.
 */
sealed interface GcpCredentials : Credentials

/**
 * Use Application Default to authenticate to Google Cloud Platform.
 */
object ApplicationDefaultGcpCredentials : GcpCredentials

/**
 * Use Service Account to authenticate to Google Cloud Platform.
 *
 * @param credentials a block which returns the exported service account credentials payload.
 */
class ExportedKeyGcpCredentials(val credentials: () -> String) : GcpCredentials {
    /**
     * Builds an [ExportedKeyGcpCredentials] from a file containing the exported service account keys.
     */
    constructor(file: File) : this({ file.readText() })
}
