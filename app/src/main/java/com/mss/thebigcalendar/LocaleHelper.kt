package com.mss.thebigcalendar

import android.content.Context
import android.content.ContextWrapper
import android.os.LocaleList
import java.util.Locale

class LocaleHelper(base: Context) : ContextWrapper(base) {

    companion object {

        fun onAttach(context: Context): ContextWrapper {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val language = prefs.getString("selected_language", "system") ?: "system"
            val locale = getLocale(language)

            val configuration = context.resources.configuration
            configuration.setLocale(locale)
            
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            configuration.setLocales(localeList)
            
            return LocaleHelper(context.createConfigurationContext(configuration))
        }

        private fun getLocale(language: String): Locale {
            return when (language) {
                "pt" -> Locale("pt", "BR")
                "en" -> Locale.ENGLISH
                "es" -> Locale("es", "ES")
                "fr" -> Locale.FRENCH
                "de" -> Locale.GERMAN
                "it" -> Locale.ITALIAN
                "ja" -> Locale.JAPANESE
                "ru" -> Locale("ru", "RU")
                "zh-rCN" -> Locale.SIMPLIFIED_CHINESE
                else -> Locale.getDefault()
            }
        }
    }
}