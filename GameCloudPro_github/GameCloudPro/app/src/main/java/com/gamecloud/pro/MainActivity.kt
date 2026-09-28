package com.gamecloud.pro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamecloud.pro.domain.model.*
import com.gamecloud.pro.presentation.GameCloudViewModel
import com.gamecloud.pro.presentation.UiState

class MainActivity : ComponentActivity() {
    private var folderCallback: ((android.net.Uri) -> Unit)? = null
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) folderCallback?.invoke(uri)
        folderCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: GameCloudViewModel = viewModel()
            val state by vm.state.collectAsState()
            GameCloudTheme {
                GameCloudApp(
                    state = state,
                    onSelect = vm::selectGame,
                    onFolder = { folderCallback = vm::setFolder; picker.launch(null) },
                    onBackup = vm::backup,
                    onBack = vm::clearSelection
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameCloudApp(
    state: UiState,
    onSelect: (InstalledGame) -> Unit,
    onFolder: () -> Unit,
    onBackup: () -> Unit,
    onBack: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("GameCloud", fontWeight = FontWeight.Bold) },
                actions = { IconButton({}) { Icon(Icons.Default.Cloud, null) } }
            )
        },
        bottomBar = {
            NavigationBar {
                listOf("Oyunlar" to Icons.Default.Gamepad, "Bulut" to Icons.Default.Cloud, "Ayarlar" to Icons.Default.Settings)
                    .forEachIndexed { i, (label, icon) ->
                        NavigationBarItem(tab == i, { tab = i }, icon = { Icon(icon, null) }, label = { Text(label) })
                    }
            }
        }
    ) { p ->
        when (tab) {
            0 -> Games(state, p, onSelect, onFolder, onBackup, onBack)
            1 -> Cloud(state, p)
            2 -> Settings(p)
        }
    }
}

@Composable
private fun Games(
    state: UiState, padding: PaddingValues,
    onSelect: (InstalledGame) -> Unit, onFolder: () -> Unit, onBackup: () -> Unit, onBack: () -> Unit
) {
    BackHandler(enabled = state.selected != null, onBack = onBack)
    Column(Modifier.fillMaxSize().padding(padding).padding(18.dp)) {
        Text("Oyunların", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("${state.games.size} başlatılabilir uygulama bulundu.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))

        if (state.selected == null) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(state.games, key = { it.packageName }) { game ->
                    Card(Modifier.fillMaxWidth().clickable { onSelect(game) }) {
                        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SportsEsports, null)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(game.name, fontWeight = FontWeight.SemiBold)
                                Text("v${game.versionName}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        } else {
            TextButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null)
                Spacer(Modifier.width(6.dp))
                Text("Oyun listesi")
            }
            Text("Seçilen oyun", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(state.selected.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(state.selected.packageName, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (state.config?.folderUri != null) "✓ Kayıt klasörü bağlı" else "Kayıt klasörü bağlı değil",
                        color = if (state.config?.folderUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onFolder, Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FolderOpen, null)
                        Spacer(Modifier.width(7.dp))
                        Text("Kayıt klasörünü bağla")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onBackup, Modifier.fillMaxWidth(), enabled = state.config?.folderUri != null) {
                        Icon(Icons.Default.CloudUpload, null)
                        Spacer(Modifier.width(7.dp))
                        Text("Buluta yedekle")
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(state.message, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun Cloud(state: UiState, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(18.dp)) {
        Text("Bulut", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(state.selected?.name ?: "Oyun seçilmedi", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                when (val b = state.backupState) {
                    BackupState.Idle -> Text("Henüz bu oturumda yedekleme yapılmadı.")
                    is BackupState.Running -> {
                        Text("Yedek hazırlanıyor… ${b.completed} dosya işlendi")
                        Spacer(Modifier.height(8.dp))
                        if (b.total > 0) LinearProgressIndicator(progress = { b.completed / b.total.toFloat() }, modifier = Modifier.fillMaxWidth())
                        else LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    is BackupState.Success -> Text("${b.manifest.files.size} dosya • ${b.manifest.totalBytes} byte")
                    is BackupState.Error -> Text(b.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun Settings(padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(18.dp)) {
        Text("Ayarlar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("GameCloud Pro", fontWeight = FontWeight.Bold)
                Text("Sürüm 3.0")
                Spacer(Modifier.height(12.dp))
                Text("Bulut hesabı ve Firebase ayarları bir sonraki üretim adımında etkinleştirilecek.")
            }
        }
    }
}

@Composable
fun GameCloudTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(), content = content)
}
