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
 */

package androidx.build.gradle.gcpbuildcache

import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class GcpStorageServiceTempFileTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun failedDownloadDeletesPartialTemporaryFile() {
        var createdPath: Path? = null
        val expectedFailure = IOException("synthetic download failure")

        val actualFailure = try {
            downloadToTemporaryFile(
                createTempFile = {
                    Files.createTempFile(temporaryFolder.root.toPath(), "cache", ".tmp")
                },
                download = { path ->
                    createdPath = path
                    Files.write(path, byteArrayOf(1, 2, 3))
                    throw expectedFailure
                }
            )
            null
        } catch (failure: IOException) {
            failure
        }

        assertSame(expectedFailure, actualFailure)
        val path = createdPath ?: throw AssertionError("download did not create a temporary file")
        assertFalse(Files.exists(path))
    }
}
