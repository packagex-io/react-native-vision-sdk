package io.packagex.visionsdk.utils

import java.io.File
import java.util.zip.ZipFile

internal object ZipUtils {
    /**
     * Extract a zip file into any directory
     *
     * @param zipFile src zip file
     * @param extractTo directory to extract into.
     * There will be new folder with the zip's name inside [extractTo] directory.
     * @param extractHere no extra folder will be created and will be extracted
     * directly inside [extractTo] folder.
     *
     * @return the extracted directory i.e, [extractTo] folder if [extractHere] is `true`
     * and [extractTo]\zipFile\ folder otherwise.
     */
    fun extractZipFile(
        zipFile: File,
        extractTo: File,
        extractHere: Boolean = false,
    ): File? {
        return try {
            val outputDir = if (extractHere) {
                extractTo
            } else {
                File(extractTo, zipFile.nameWithoutExtension)
            }

            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        if (entry.isDirectory) {
                            val d = File(outputDir, entry.name)
                            if (!d.exists()) d.mkdirs()
                        } else {
                            val f = File(outputDir, entry.name)
                            if (f.parentFile?.exists() != true)  f.parentFile?.mkdirs()

                            f.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }

            extractTo
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}