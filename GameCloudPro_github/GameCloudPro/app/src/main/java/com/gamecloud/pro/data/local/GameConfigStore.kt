package com.gamecloud.pro.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.gamecloud.pro.domain.model.GameConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gameStore by preferencesDataStore("game_config")

class GameConfigStore(private val context: Context) {
    private fun key(packageName: String) =
        stringPreferencesKey("folder_${packageName.replace('.', '_')}")

    private fun autoKey(packageName: String) =
        booleanPreferencesKey("auto_${packageName.replace('.', '_')}")

    fun observe(packageName: String): Flow<GameConfig> =
        context.gameStore.data.map { prefs ->
            GameConfig(
                packageName = packageName,
                folderUri = prefs[key(packageName)],
                autoBackup = prefs[autoKey(packageName)] ?: true
            )
        }

    suspend fun save(config: GameConfig) {
        context.gameStore.edit { prefs ->
            if (config.folderUri == null) prefs.remove(key(config.packageName))
            else prefs[key(config.packageName)] = config.folderUri
            prefs[autoKey(config.packageName)] = config.autoBackup
        }
    }
}
