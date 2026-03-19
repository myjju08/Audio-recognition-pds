package com.earbrief.app.engine.tts

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(kotlin.io.path.ExperimentalPathApi::class)
@DisplayName("TtsDiskCache")
class TtsDiskCacheTest {

    private val tempDirs = mutableListOf<File>()

    @AfterEach
    fun tearDown() {
        tempDirs.forEach { dir ->
            dir.deleteRecursively()
        }
    }

    @Test
    fun `cache miss delegates to engine and stores mp3`() = runTest {
        val now = 1_000_000L
        val text = "hello"
        val expectedBytes = byteArrayOf(1, 2, 3, 4)
        val delegate = mockk<TtsEngine>()
        coEvery { delegate.synthesize(text) } returns Result.success(expectedBytes)

        val cacheDir = createTempDir()
        val cache = TtsDiskCache(
            delegate = delegate,
            cacheDir = cacheDir,
            nowProvider = { now }
        )

        val result = cache.synthesize(text)

        assertTrue(result.isSuccess)
        assertArrayEquals(expectedBytes, result.getOrThrow())
        coVerify(exactly = 1) { delegate.synthesize(text) }
        val cachedFile = File(cacheDir, "${md5(text)}.mp3")
        assertTrue(cachedFile.exists())
        assertArrayEquals(expectedBytes, cachedFile.readBytes())
    }

    @Test
    fun `cache hit returns cached bytes without delegate call`() = runTest {
        val now = 2_000_000L
        val text = "cached"
        val cachedBytes = byteArrayOf(9, 8, 7)
        val delegate = mockk<TtsEngine>()
        val cacheDir = createTempDir()
        val cachedFile = File(cacheDir, "${md5(text)}.mp3")
        cachedFile.writeBytes(cachedBytes)
        cachedFile.setLastModified(now - 1_000L)

        val cache = TtsDiskCache(
            delegate = delegate,
            cacheDir = cacheDir,
            nowProvider = { now }
        )

        val result = cache.synthesize(text)

        assertTrue(result.isSuccess)
        assertArrayEquals(cachedBytes, result.getOrThrow())
        coVerify(exactly = 0) { delegate.synthesize(any()) }
    }

    @Test
    fun `prune removes files older than ttl`() {
        val now = 200_000_000L
        val ttlMs = 24L * 60L * 60L * 1000L
        val cacheDir = createTempDir()
        val oldFile = File(cacheDir, "old.mp3")
        oldFile.writeBytes(byteArrayOf(1))
        oldFile.setLastModified(now - ttlMs - 1)

        val freshFile = File(cacheDir, "fresh.mp3")
        freshFile.writeBytes(byteArrayOf(2))
        freshFile.setLastModified(now - 10_000)

        TtsDiskCache(
            delegate = mockk(),
            cacheDir = cacheDir,
            ttlMs = ttlMs,
            nowProvider = { now }
        )

        assertFalse(oldFile.exists())
        assertTrue(freshFile.exists())
        assertEquals(1L, freshFile.length())
    }

    private fun createTempDir(): File {
        return Files.createTempDirectory("tts-cache-test").toFile().also { dir ->
            tempDirs += dir
        }
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
