package es.ulpgc.pamn.dominio.ui.text

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources

// Un texto de la interfaz que se traduce (docs/tareas/ajustes.md, fase 3, decisión U). En vez de la frase
// ya escrita, dice qué texto es (su id en strings.xml) y sus datos; la frase sale al pintarlo, en el idioma
// de la app. Así las funciones de textos siguen siendo puras y se prueban en el ordenador sin Android.
sealed interface UiText {

    // Un texto de strings.xml; `args` rellenan sus %1$s, %2$d… (pueden ser otros UiText).
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    // Un plural de strings.xml: la forma se elige por `count` («1 pregunta» / «3 preguntas»).
    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = listOf(count)) : UiText

    // Lo que no se traduce: lo que escribe el autor de un curso (decisión R) o lo que ya viene escrito.
    data class Raw(val text: String) : UiText

    // Factory methods: UiText.of(R.string.paso, 2, 5) se lee mejor que Res(R.string.paso, listOf(2, 5)).
    companion object {
        fun of(@StringRes id: Int, vararg args: Any): UiText = Res(id, args.toList())

        fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): UiText =
            Plural(id, count, if (args.isEmpty()) listOf(count) else args.toList())

        fun raw(text: String): UiText = Raw(text)
    }
}

// Puerto: quien convierte un id en su frase. En la app, los Resources de Android (en el idioma elegido);
// en los tests del ordenador, uno falso.
interface StringResolver {
    fun string(id: Int, args: List<Any>): String

    fun plural(id: Int, count: Int, args: List<Any>): String
}

// La frase. Los datos que son UiText se traducen primero (p. ej. «Entregada el %1$s» con una fecha).
fun UiText.resolve(resolver: StringResolver): String = when (this) {
    is UiText.Res -> resolver.string(id, args.resolved(resolver))
    is UiText.Plural -> resolver.plural(id, count, args.resolved(resolver))
    is UiText.Raw -> text
}

private fun List<Any>.resolved(resolver: StringResolver): List<Any> = map { if (it is UiText) it.resolve(resolver) else it }

// El adaptador de Android: getString y getQuantityString, en el idioma de esos Resources.
class ResourcesResolver(private val resources: Resources) : StringResolver {
    override fun string(id: Int, args: List<Any>): String = resources.getString(id, *args.toTypedArray())

    override fun plural(id: Int, count: Int, args: List<Any>): String =
        resources.getQuantityString(id, count, *args.toTypedArray())
}

// Para pintarlo en Compose. LocalResources (como stringResource): si cambia el idioma, se vuelve a pintar.
@Composable
fun UiText.asString(): String = resolve(ResourcesResolver(LocalResources.current))
