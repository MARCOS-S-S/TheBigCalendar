package com.mss.thebigcalendar.data.model

enum class Language(
    val code: String,
    val displayName: String,
    val flag: String
) {
    SYSTEM("system", "Sistema", "🌐"),
    PORTUGUESE("pt", "Português", "🇧🇷"),
    ENGLISH("en", "English", "🇺🇸"),
    SPANISH("es", "Español", "🇪🇸"),
    FRENCH("fr", "Français", "🇫🇷"),
    GERMAN("de", "Deutsch", "🇩🇪"),
    ITALIAN("it", "Italiano", "🇮🇹"),
    JAPANESE("ja", "日本語", "🇯🇵"),
    RUSSIAN("ru", "Русский", "🇷🇺"),
    CHINESE_SIMPLIFIED("zh-rCN", "中文 (简体)", "🇨🇳");
    
    companion object {
        fun fromCode(code: String): Language {
            return values().find { it.code == code } ?: SYSTEM
        }
        
        fun getAvailableLanguages(): List<Language> {
            return values().toList()
        }
    }
}














