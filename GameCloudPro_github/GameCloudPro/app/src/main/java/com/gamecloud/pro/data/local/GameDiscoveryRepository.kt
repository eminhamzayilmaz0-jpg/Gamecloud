package com.gamecloud.pro.data.local

import android.content.Context
import android.content.pm.PackageManager
import com.gamecloud.pro.domain.model.InstalledGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameDiscoveryRepository(private val context: Context) {
    suspend fun getGames(): List<InstalledGame> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .mapNotNull {
                runCatching {
                    val info = pm.getPackageInfo(it.packageName, 0)
                    InstalledGame(
                        name = pm.getApplicationLabel(it).toString(),
                        packageName = it.packageName,
                        versionName = info.versionName ?: "-",
                        versionCode = if (android.os.Build.VERSION.SDK_INT >= 28)
                            info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
                    )
                }.getOrNull()
            }
            .sortedBy { it.name.lowercase() }
            .toList()
    }
}
