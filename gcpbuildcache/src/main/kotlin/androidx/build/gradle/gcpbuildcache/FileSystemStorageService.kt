package androidx.build.gradle.gcpbuildcache

import java.io.File
import java.io.InputStream
import java.nio.file.Files

/**
 * An implementation of the [StorageService] that is backed by a file system.
 */
class FileSystemStorageService(
    override val projectId: String,
    override val bucketName: String,
    override val isPush: Boolean
) : StorageService {

    private val location = Files.createTempDirectory("tmp$projectId$bucketName").toFile()

    override fun load(cacheKey: String): InputStream? {
        val file = File(location, cacheKey)
        return if (file.exists() && file.isFile) {
            file.inputStream()
        } else {
            null
        }
    }

    override fun store(cacheKey: String, contents: ByteArray): Boolean {
        if (!isPush) {
            return false
        }

        val file = File(location, cacheKey)
        val output = file.outputStream()
        output.use {
            output.write(contents)
        }
        return true
    }

    override fun delete(cacheKey: String): Boolean {
        if (!isPush) {
            return false
        }

        val file = File(location, cacheKey)
        return file.delete()
    }

    override fun close() {
        location.deleteRecursively()
    }

    companion object {
        private fun File.deleteRecursively() {
            val files = listFiles()
            for (file in files) {
                if (file.isFile) {
                    file.delete()
                }
                if (file.isDirectory) {
                    file.deleteRecursively()
                }
            }
            delete()
        }
    }

}
