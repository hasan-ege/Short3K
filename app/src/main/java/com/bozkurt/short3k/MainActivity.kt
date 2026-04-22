package com.bozkurt.short3k

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnRefresh: Button
    private lateinit var btnGenerateAll: Button
    private lateinit var btnOpenSettings: Button
    private lateinit var emptyStateLayout: View
    private lateinit var tvAppTitle: TextView
    private lateinit var searchView: SearchView
    private val gamesList = mutableListOf<GameModel>()
    private lateinit var adapter: GameAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // İlk açılışta dili cihaz diline göre ayarla (eğer henüz seçilmemişse)
        LocaleHelper.initialize(this)
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        btnRefresh = findViewById<Button>(R.id.btnRefresh)
        btnGenerateAll = findViewById<Button>(R.id.btnGenerateAll)
        btnOpenSettings = findViewById<Button>(R.id.btnOpenSettings)
        emptyStateLayout = findViewById<View>(R.id.emptyStateLayout)
        tvAppTitle = findViewById<TextView>(R.id.tvAppTitle)
        searchView = findViewById<SearchView>(R.id.searchView)

        adapter = GameAdapter(gamesList, 
            onItemClick = { game: GameModel -> handleGameClick(game) },
            onDeleteClick = { game: GameModel -> deleteShortcutFile(game) }
        )
        recyclerView.adapter = adapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })

        btnRefresh.setOnClickListener {
            getSavedFolderUri("VITA3K_URI")?.let { loadGamesFromUri(it) }
            Toast.makeText(this, getString(R.string.library_refreshed), Toast.LENGTH_SHORT).show()
        }
        btnGenerateAll.setOnClickListener {
            generateAllShortcuts()
        }
        btnOpenSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btnOpenSettingsEmpty).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        getSavedFolderUri("VITA3K_URI")?.let {
            loadGamesFromUri(it)
        } ?: run {
            showEmptyState(true)
        }
    }

    private fun showEmptyState(show: Boolean) {
        emptyStateLayout.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun saveFolderUri(uri: Uri, key: String) {
        val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(key, uri.toString())
            apply()
        }
    }

    private fun getSavedFolderUri(key: String): Uri? {
        val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val uriString = sharedPref.getString(key, null)
        return if (uriString != null) Uri.parse(uriString) else null
    }

    private fun loadGamesFromUri(uri: Uri) {
        gamesList.clear()
        showEmptyState(false)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rootDir = DocumentFile.fromTreeUri(this@MainActivity, uri)
                if (rootDir == null || !rootDir.isDirectory) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Invalid Folder", Toast.LENGTH_SHORT).show()
                        showEmptyState(true)
                    }
                    return@launch
                }

                val gameDirs = rootDir.listFiles()
                val foundCodes = mutableSetOf<String>()
                
                gameDirs.forEach { gameDir ->
                    if (gameDir.isDirectory) {
                        val code = gameDir.name ?: "Unknown"
                        var name = "Unknown Game"
                        var version = "1.00"
                        var iconUri: String? = null
                        var backgroundUri: String? = null
                        
                        val sceSysDir = gameDir.findFile("sce_sys")
                        if (sceSysDir != null && sceSysDir.isDirectory) {
                            val paramSfo = sceSysDir.findFile("param.sfo")
                            if (paramSfo != null) {
                                contentResolver.openInputStream(paramSfo.uri)?.use { stream ->
                                    val sfoData = SfoParser.parseSfo(stream)
                                    name = sfoData["TITLE"] ?: name
                                    version = sfoData["APP_VER"] ?: version
                                }
                            }
                            
                            val iconFile = sceSysDir.findFile("icon0.png")
                            if (iconFile != null) iconUri = iconFile.uri.toString()

                            val bgFile = sceSysDir.findFile("pic0.png")
                            if (bgFile != null) backgroundUri = bgFile.uri.toString()
                        }
                        
                        if (code.length >= 7) {
                            val game = GameModel(name, code, version, iconUri, backgroundUri)
                            
                            // Check for shortcut
                            getSavedFolderUri("SHORTCUT_URI")?.let { scUri ->
                                val scDir = DocumentFile.fromTreeUri(this@MainActivity, scUri)
                                val scFile = scDir?.findFile("${name}.psvita")
                                game.hasShortcut = scFile != null
                            }
                            
                            gamesList.add(game)
                            foundCodes.add(code)
                        }
                    }
                }

                // Check for orphans (Shortcuts without games)
                getSavedFolderUri("SHORTCUT_URI")?.let { scUri ->
                    val scDir = DocumentFile.fromTreeUri(this@MainActivity, scUri)
                    scDir?.listFiles()?.forEach { file ->
                        if (file.name?.endsWith(".psvita") == true) {
                            val gameName = file.name!!.removeSuffix(".psvita")
                            var titleId = ""
                            try {
                                contentResolver.openInputStream(file.uri)?.use { stream ->
                                    titleId = stream.bufferedReader().readText().trim()
                                }
                            } catch (e: Exception) {}

                            if (titleId.isNotEmpty() && !foundCodes.contains(titleId)) {
                                // Missing game but has shortcut
                                gamesList.add(GameModel(
                                    name = gameName,
                                    code = titleId,
                                    version = "MISSING",
                                    iconUri = null,
                                    backgroundUri = null,
                                    hasShortcut = true
                                ))
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    adapter.updateList(gamesList)
                    if (gamesList.isEmpty()) {
                        showEmptyState(true)
                        tvAppTitle.text = "SHORT3K | ${getString(R.string.library)} (0)"
                    } else {
                        tvAppTitle.text = "SHORT3K | ${getString(R.string.library)} (${gamesList.size})"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { showEmptyState(true) }
            }
        }
    }

    private fun handleGameClick(game: GameModel) {
        if (game.name.contains("(Missing)")) {
            Toast.makeText(this, getString(R.string.missing_warning), Toast.LENGTH_LONG).show()
            return
        }

        val scUri = getSavedFolderUri("SHORTCUT_URI")
        if (scUri == null) {
            Toast.makeText(this, getString(R.string.select_folder_first), Toast.LENGTH_SHORT).show()
            return
        }

        if (game.hasShortcut) {
            Toast.makeText(this, "${getString(R.string.already_exists)} ${game.name}", Toast.LENGTH_SHORT).show()
        } else {
            // Create shortcut
            createShortcutFile(game, scUri)
        }
    }

    private fun createShortcutFile(game: GameModel, folderUri: Uri) {
        try {
            val rootDir = DocumentFile.fromTreeUri(this, folderUri)
            val fileName = "${game.name}.psvita"
            val file = rootDir?.createFile("application/octet-stream", fileName)
            file?.uri?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(game.code.toByteArray())
                }
                Toast.makeText(this, getString(R.string.shortcut_created), Toast.LENGTH_SHORT).show()
                game.hasShortcut = true
                adapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error creating shortcut: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun generateAllShortcuts() {
        val scUri = getSavedFolderUri("SHORTCUT_URI")
        if (scUri == null) {
            Toast.makeText(this, getString(R.string.select_folder_first), Toast.LENGTH_SHORT).show()
            return
        }

        var createdCount = 0
        gamesList.forEach { game ->
            if (!game.hasShortcut && !game.name.contains("(Missing)")) {
                try {
                    val rootDir = DocumentFile.fromTreeUri(this, scUri)
                    val fileName = "${game.name}.psvita"
                    val file = rootDir?.createFile("application/octet-stream", fileName)
                    file?.uri?.let { uri ->
                        contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(game.code.toByteArray())
                        }
                        game.hasShortcut = true
                        createdCount++
                    }
                } catch (e: Exception) {}
            }
        }
        adapter.notifyDataSetChanged()
        Toast.makeText(this, getString(R.string.gen_count, createdCount), Toast.LENGTH_SHORT).show()
    }

    private fun deleteShortcutFile(game: GameModel, silent: Boolean = false) {
        val scUri = getSavedFolderUri("SHORTCUT_URI") ?: return
        try {
            val scDir = DocumentFile.fromTreeUri(this, scUri)
            // Fix: Use the clean name without (Missing)
            val fileName = "${game.name}.psvita"
            val scFile = scDir?.findFile(fileName)
            if (scFile != null && scFile.delete()) {
                if (game.version == "MISSING") {
                    gamesList.remove(game)
                } else {
                    game.hasShortcut = false
                }
                adapter.notifyDataSetChanged()
                if (!silent) Toast.makeText(this, "Shortcut deleted", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            if (!silent) Toast.makeText(this, "Error deleting: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchGame(game: GameModel) {
        try {
            val intent = Intent().apply {
                component = ComponentName("org.vita3k.emulator", "org.vita3k.emulator.Emulator")
                action = "LAUNCH_${game.code}"
                putExtra("AppStartParameters", arrayOf("-r", game.code))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Vita3K not found or error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
