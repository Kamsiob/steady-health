package com.kamsiob.steadyhealth.backup

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** A zip in memory, put together the way the export puts the real one together. */
fun zipOf(vararg files: Pair<String, String>): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
        files.forEach { (name, text) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(text.toByteArray())
            zip.closeEntry()
        }
    }
    return out.toByteArray()
}
