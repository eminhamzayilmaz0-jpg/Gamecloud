package com.gamecloud.pro

import android.content.Context
import com.gamecloud.pro.data.cloud.FirebaseCloudBackupRepository
import com.gamecloud.pro.data.files.BackupScanner
import com.gamecloud.pro.data.local.GameConfigStore
import com.gamecloud.pro.data.local.GameDiscoveryRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val gameStore = GameConfigStore(appContext)
    val gameDiscovery = GameDiscoveryRepository(appContext)
    val scanner = BackupScanner(appContext)
    val cloud = FirebaseCloudBackupRepository(appContext)
}
