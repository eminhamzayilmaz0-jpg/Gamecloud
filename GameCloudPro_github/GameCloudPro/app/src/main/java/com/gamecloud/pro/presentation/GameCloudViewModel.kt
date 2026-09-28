package com.gamecloud.pro.presentation

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamecloud.pro.GameCloudApplication
import com.gamecloud.pro.data.cloud.newManifest
import com.gamecloud.pro.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val games: List<InstalledGame> = emptyList(),
    val selected: InstalledGame? = null,
    val config: GameConfig? = null,
    val backupState: BackupState = BackupState.Idle,
    val message: String = "Hazır"
)

class GameCloudViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as GameCloudApplication).container

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var configJob: Job? = null

    init { refreshGames() }

    fun refreshGames() {
        viewModelScope.launch {
            val games = container.gameDiscovery.getGames()
            _state.update { it.copy(games = games) }
        }
    }

    fun selectGame(game: InstalledGame) {
        _state.update {
            it.copy(
                selected = game,
                config = null,
                backupState = BackupState.Idle,
                message = "${game.name} seçildi."
            )
        }
        configJob?.cancel()
        configJob = viewModelScope.launch {
            container.gameStore.observe(game.packageName).collect { config ->
                _state.update { it.copy(config = config) }
            }
        }
    }

    fun clearSelection() {
        configJob?.cancel()
        configJob = null
        _state.update {
            it.copy(selected = null, config = null, backupState = BackupState.Idle, message = "Hazır")
        }
    }

    fun setFolder(uri: Uri) {
        val game = _state.value.selected ?: return
        viewModelScope.launch {
            runCatching {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                getApplication<Application>().contentResolver.takePersistableUriPermission(uri, flags)
                container.gameStore.save(GameConfig(game.packageName, uri.toString(), true))
            }.onSuccess {
                _state.update { s -> s.copy(message = "Kayıt klasörü bağlandı.") }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                val msg = "Klasör izni alınamadı: ${e.message ?: "bilinmeyen hata"}"
                _state.update { s -> s.copy(message = msg) }
            }
        }
    }

    fun backup() {
        val game = _state.value.selected ?: return
        val folder = _state.value.config?.folderUri ?: run {
            _state.update { it.copy(message = "Önce kayıt klasörünü seçin.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(backupState = BackupState.Running(0, 0), message = "Yedek hazırlanıyor…") }
            runCatching {
                val treeUri = Uri.parse(folder)
                val files = container.scanner.scan(treeUri) { done ->
                    _state.update { s -> s.copy(backupState = BackupState.Running(done, 0)) }
                }
                val manifest = newManifest(game.packageName, game.versionName, files)
                container.cloud.upload(game.packageName, treeUri, manifest)
            }.onSuccess { manifest ->
                _state.update { s -> s.copy(backupState = BackupState.Success(manifest), message = "Bulut yedeği hazır.") }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                val msg = e.message ?: "Bilinmeyen hata"
                _state.update { s -> s.copy(backupState = BackupState.Error(msg), message = msg) }
            }
        }
    }
}
