package com.gamecloud.pro.data.files

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.gamecloud.pro.domain.model.BackupFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class BackupScanner(private val context: Context) {
    suspend fun scan(treeUri: Uri, onProgress: (Int) -> Unit = {}): List<BackupFile> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: error("Kayıt klasörüne erişilemiyor.")
        val result = mutableListOf<BackupFile>()
        walk(root, "", result, onProgress)
        result
    }

    private fun walk(dir: DocumentFile, prefix: String, out: MutableList<BackupFile>, onProgress: (Int) -> Unit) {
        dir.listFiles().forEach { child ->
            val name = child.name ?: return@forEach
            val relative = if (prefix.isEmpty()) name else "$prefix/$name"
            when {
                child.isDirectory -> walk(child, relative, out, onProgress)
                child.isFile && child.canRead() -> {
                    out += BackupFile(
                        relativePath = relative,
                        sizeBytes = child.length(),
                        sha256 = sha256(child.uri)
                    )
                    onProgress(out.size)
                }
            }
        }
    }

    private fun sha256(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Dosya okunamadı." }
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
