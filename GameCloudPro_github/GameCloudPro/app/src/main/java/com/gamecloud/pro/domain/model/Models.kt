package com.gamecloud.pro.domain.model

data class InstalledGame(
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long
)

data class GameConfig(
    val packageName: String,
    val folderUri: String? = null,
    val autoBackup: Boolean = true
)

data class BackupFile(
    val relativePath: String,
    val sizeBytes: Long,
    val sha256: String
)

data class BackupManifest(
    val id: String,
    val packageName: String,
    val appVersion: String,
    val createdAtEpochMs: Long,
    val files: List<BackupFile>,
    val totalBytes: Long
)

sealed interface BackupState {
    data object Idle : BackupState
    data class Running(val completed: Int, val total: Int) : BackupState
    data class Success(val manifest: BackupManifest) : BackupState
    data class Error(val message: String) : BackupState
}
