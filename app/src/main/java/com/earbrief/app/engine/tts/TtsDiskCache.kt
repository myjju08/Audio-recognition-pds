package com.earbrief.app.engine.tts

import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TtsDiskCache(
    private val delegate: TtsEngine,
    private val cacheDir: File,
    private val maxCacheBytes: Long = 50L * 1024L * 1024L,
    private val ttlMs: Long = 24L * 60L * 60L * 1000L,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) : TtsEngine {

    private val lock = Mutex()

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        pruneCache()
    }

    override suspend fun synthesize(text: String): Result<ByteArray> {
        return lock.withLock {
            val file = cacheFileForText(text)
            val now = nowProvider()

            if (file.exists()) {
                val ageMs = now - file.lastModified()
                if (ageMs <= ttlMs) {
                    return@withLock runCatching { file.readBytes() }
                }
                file.delete()
            }

            delegate.synthesize(text).onSuccess { bytes ->
                runCatching {
                    file.writeBytes(bytes)
                    file.setLastModified(now)
                }
                pruneCache()
            }
        }
    }

    override fun release() {
        delegate.release()
    }

    private fun cacheFileForText(text: String): File {
        return File(cacheDir, "${md5(text)}.mp3")
    }

    private fun pruneCache() {
        val now = nowProvider()
        val files = cacheDir.listFiles()?.filter { it.isFile && it.extension == "mp3" }.orEmpty().toMutableList()

        files.filter { now - it.lastModified() > ttlMs }.forEach { expired ->
            expired.delete()
        }

        val activeFiles = cacheDir.listFiles()?.filter { it.isFile && it.extension == "mp3" }.orEmpty()
            .sortedBy { it.lastModified() }
            .toMutableList()

        var totalSize = activeFiles.sumOf { it.length() }
        while (totalSize > maxCacheBytes && activeFiles.isNotEmpty()) {
            val oldest = activeFiles.removeFirst()
            totalSize -= oldest.length()
            oldest.delete()
        }
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
