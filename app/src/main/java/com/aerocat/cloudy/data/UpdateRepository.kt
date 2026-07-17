package com.aerocat.cloudy.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed interface DownloadState {
    data class Progress(val bytes: Long, val total: Long) : DownloadState {
        val fraction: Float get() = if (total > 0) bytes.toFloat() / total else 0f
    }
    data class Done(val file: File) : DownloadState
    data class Failed(val reason: String) : DownloadState
}

class UpdateRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {

    /** Fetch + parse the manifest from the user-configured Custom JSON URL. */
    suspend fun fetchManifest(url: String): Result<UpdateManifest> = runCatching {
        val request = Request.Builder().url(url).header("User-Agent", "Cloudy/8.6.4").build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val body = resp.body?.string() ?: error("Empty response")
            gson.fromJson(body, UpdateManifest::class.java)
                ?: error("Malformed JSON")
        }
    }

    /**
     * Streams the package to [dest], emitting progress, then verifies SHA-256.
     * A mismatched hash fails the download instead of handing a corrupt image to the flasher.
     */
    fun download(download: Download, dest: File): Flow<DownloadState> = flow {
        val request = Request.Builder().url(download.url).build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) { emit(DownloadState.Failed("HTTP ${resp.code}")); return@flow }
            val body = resp.body ?: run { emit(DownloadState.Failed("Empty body")); return@flow }
            val total = if (download.sizeBytes > 0) download.sizeBytes else body.contentLength()

            val digest = MessageDigest.getInstance("SHA-256")
            body.byteStream().use { input ->
                dest.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var read: Int
                    var written = 0L
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                        digest.update(buf, 0, read)
                        written += read
                        emit(DownloadState.Progress(written, total))
                    }
                }
            }
            val hex = digest.digest().joinToString("") { "%02x".format(it) }
            if (!hex.equals(download.sha256, ignoreCase = true)) {
                dest.delete()
                emit(DownloadState.Failed("Checksum mismatch — download rejected"))
            } else {
                emit(DownloadState.Done(dest))
            }
        }
    }.flowOn(Dispatchers.IO)
}
