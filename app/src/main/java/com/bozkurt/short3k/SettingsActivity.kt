package com.bozkurt.short3k

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SettingsActivity : AppCompatActivity() {

    private val languages = listOf(
        "English" to "en",
        "Türkçe" to "tr",
        "Deutsch" to "de",
        "Français" to "fr",
        "Español" to "es",
        "Русский" to "ru",
        "日本語" to "ja",
        "中文" to "zh",
        "Italiano" to "it",
        "Português" to "pt",
        "العربية" to "ar",
        "हिन्दी" to "hi",
        "Nederlands" to "nl",
        "Polski" to "pl",
        "Ελληνικά" to "el",
        "Svenska" to "sv",
        "Dansk" to "da",
        "Suomi" to "fi",
        "Norsk" to "no",
        "한국어" to "ko"
    )

    private lateinit var tvVitaPathValue: TextView
    private lateinit var tvShortcutPathValue: TextView

    private val vitaFolderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            saveFolderUri(it, "VITA3K_URI")
            updatePathDisplays()
            Toast.makeText(this, getString(R.string.library_refreshed), Toast.LENGTH_SHORT).show()
        }
    }

    private val shortcutFolderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            saveFolderUri(it, "SHORTCUT_URI")
            updatePathDisplays()
            Toast.makeText(this, getString(R.string.library_refreshed), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tvVitaPathValue = findViewById(R.id.tvVitaPathValue)
        tvShortcutPathValue = findViewById(R.id.tvShortcutPathValue)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnSetVitaPath).setOnClickListener { vitaFolderPicker.launch(null) }
        findViewById<MaterialButton>(R.id.btnSetShortcutPath).setOnClickListener { shortcutFolderPicker.launch(null) }

        setupLanguageSpinner()
        updatePathDisplays()
    }

    private fun updatePathDisplays() {
        val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        
        val vitaUri = sharedPref.getString("VITA3K_URI", null)
        if (vitaUri != null) {
            val path = Uri.parse(vitaUri).path ?: vitaUri
            val cleanPath = path.substringAfterLast(":")
            tvVitaPathValue.text = getString(R.string.path_display, cleanPath)
            tvVitaPathValue.alpha = 1.0f
        } else {
            tvVitaPathValue.text = "${getString(R.string.not_selected)}\n${getString(R.string.vita_path_example)}"
            tvVitaPathValue.alpha = 0.5f
        }

        val shortcutUri = sharedPref.getString("SHORTCUT_URI", null)
        if (shortcutUri != null) {
            val path = Uri.parse(shortcutUri).path ?: shortcutUri
            val cleanPath = path.substringAfterLast(":")
            tvShortcutPathValue.text = getString(R.string.path_display, cleanPath)
            tvShortcutPathValue.alpha = 1.0f
        } else {
            tvShortcutPathValue.text = "${getString(R.string.not_selected)}\n${getString(R.string.shortcut_path_example)}"
            tvShortcutPathValue.alpha = 0.5f
        }
    }

    private fun setupLanguageSpinner() {
        val spinner = findViewById<Spinner>(R.id.spinnerLanguage)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages.map { it.first })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val currentLang = LocaleHelper.getSelectedLanguage(this)
        val currentIndex = languages.indexOfFirst { it.second == currentLang }.coerceAtLeast(0)
        spinner.setSelection(currentIndex)

        spinner.post {
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedLang = languages[position].second
                    if (selectedLang != currentLang) {
                        LocaleHelper.persistLanguage(this@SettingsActivity, selectedLang)
                        LocaleHelper.setLocale(selectedLang)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
    }

    private fun saveFolderUri(uri: Uri, key: String) {
        val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(key, uri.toString())
            apply()
        }
    }
}
