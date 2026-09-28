package com.gamecloud.pro.data.cloud

import android.content.Context
import android.net.Uri
import com.gamecloud.pro.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Cloud boundary. Firebase implementation can be plugged in without changing UI/domain code.
 *
 * Production Firebase path:
 * users/{uid}/games/{packageName}/backups/{backupId}/{relativePath}
 */
interface CloudBackupRepository {
    suspend fun upload(
        packageName: String,
        rootUri: Uri,
        manifest: BackupManifest
    ): BackupManifest

    suspend fun list(packageName: String): List<BackupManifest>
    suspend fun restore(packageName: String, backupId: String, destinationUri: Uri)
}

class FirebaseCloudBackupRepository(
    private val context: Context
) : CloudBackupRepository {
    override suspend fun upload(
        packageName: String,
        rootUri: Uri,
        manifest: BackupManifest
    ): BackupManifest = withContext(Dispatchers.IO) {
        // Intentionally isolated until Firebase project credentials are supplied.
        // File streaming belongs here; UI never talks directly to Firebase.
        error("Firebase yapılandırılmadı. app/google-services.json ekleyin.")
    }

    override suspend fun list(packageName: String): List<BackupManifest> {
        error("Firebase yapılandırılmadı.")
    }

    override suspend fun restore(packageName: String, backupId: String, destinationUri: Uri) {
        error("Firebase yapılandırılmadı.")
    }
}

fun newManifest(packageName: String, appVersion: String, files: List<BackupFile>) =
    BackupManifest(
        id = UUID.randomUUID().toString(),
        packageName = packageName,
        appVersion = appVersion,
        createdAtEpochMs = System.currentTimeMillis(),
        files = files,
        totalBytes = files.sumOf { it.sizeBytes }
    )
