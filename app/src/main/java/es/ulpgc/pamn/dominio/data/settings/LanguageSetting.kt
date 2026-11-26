package es.ulpgc.pamn.dominio.data.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import es.ulpgc.pamn.dominio.settings.AppLanguage

// El idioma de la app (docs/tareas/ajustes.md, fase 3, decisiones O y V). Puerto pequeño para probar Ajustes
// en el ordenador con uno falso. No se guarda en SettingsStore: lo recuerda el propio Android.
interface LanguageSetting {

    // En Android 10–12 no se puede elegir por app: se sigue al sistema.
    val canChooseLanguage: Boolean

    fun current(): AppLanguage

    // Al cambiarlo, Android vuelve a crear la pantalla en el idioma nuevo.
    fun choose(language: AppLanguage)
}

// Con LocaleManager (Android 13+), sin dependencias. Necesita android:localeConfig en el Manifest
// (res/xml/locales_config.xml): así el idioma de la app también sale en los ajustes del sistema.
class AndroidLanguageSetting(private val context: Context) : LanguageSetting {

    override val canChooseLanguage: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun current(): AppLanguage {
        if (!canChooseLanguage) return AppLanguage.SYSTEM
        return AppLanguage.fromTags(localeManager().applicationLocales.toLanguageTags())
    }

    override fun choose(language: AppLanguage) {
        if (!canChooseLanguage) return
        // Vacía = el del sistema.
        localeManager().applicationLocales = language.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
    }

    private fun localeManager(): LocaleManager = context.getSystemService(LocaleManager::class.java)
}
