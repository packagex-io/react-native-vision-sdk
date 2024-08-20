package io.packagex.visionsdk.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

internal object DownloadUtils {

    private const val CHUNK_SIZE = 10240

    fun downloadFileSuspending(
        fileLink: String,
        downloadFile: File,
        progressListener: ((Float) -> Unit)? = null
    ) {

        var linkFileSize: Int

        if (downloadFile.exists().not()) {
            downloadFile.parentFile?.mkdirs()
            downloadFile.createNewFile()
        }

        URL(fileLink)
            .also { linkFileSize = it.openConnection().contentLength }
            .openStream().use { downloadStream ->
                FileOutputStream(downloadFile).use { fos ->
                    val data = ByteArray(CHUNK_SIZE)
                    var count: Int
                    var bytesDownloaded = 0
                    while (
                        downloadStream.read(data, 0, CHUNK_SIZE)
                            .also {
                                count = it
                                bytesDownloaded += count
                            } != -1
                    ) {
                        fos.write(data, 0, count)
                        progressListener?.invoke(bytesDownloaded.toFloat() / linkFileSize.toFloat())
                    }
                }
            }
    }
}