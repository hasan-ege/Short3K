package com.bozkurt.short3k

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {
    private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"

    // Projede mevcut olan diller
    private val SUPPORTED_LANGUAGES = listOf(
        "en", "tr", "de", "fr", "es", "ru", "ja", "zh", "it"
    )

    /**
     * Uygulama dili ayarlarını yapar. 
     * Android 13+ (minSdk 33) için AppCompatDelegate kullanmak en sağlıklı yoldur.
     */
    fun setLocale(language: String) {
        val localeList = LocaleListCompat.forLanguageTags(language)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /**
     * İlk açılışta cihaz diline göre dili ayarlar.
     */
    fun initialize(context: Context) {
        val preferences = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        
        if (!preferences.contains(SELECTED_LANGUAGE)) {
            val deviceLang = Locale.getDefault().language
            val initialLang = if (SUPPORTED_LANGUAGES.contains(deviceLang)) deviceLang else "en"
            
            // Tercihi kaydet
            preferences.edit().putString(SELECTED_LANGUAGE, initialLang).apply()
            
            // AppCompatDelegate ile dili ayarla (Bu işlem asenkrondur ve aktiviteyi yeniden başlatabilir)
            setLocale(initialLang)
        } else {
            // Zaten bir tercih varsa AppCompatDelegate onu otomatik hatırlar, 
            // ama yine de manuel set etmek garantiye alır.
            val lang = preferences.getString(SELECTED_LANGUAGE, "en") ?: "en"
            setLocale(lang)
        }
    }

    fun getSelectedLanguage(context: Context): String {
        return context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .getString(SELECTED_LANGUAGE, "en") ?: "en"
    }

    fun persistLanguage(context: Context, language: String) {
        context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .edit().putString(SELECTED_LANGUAGE, language).apply()
    }
}
