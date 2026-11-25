package ani.dantotsu.parsers.kotatsu

import ani.dantotsu.Lazier
import ani.dantotsu.parsers.BaseParser
import ani.dantotsu.parsers.KotatsuMangaParser
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import org.koitharu.kotatsu.parsers.model.MangaParserSource

/** Keeps the large Kotatsu catalogue out of the source selector until the user opts in. */
object KotatsuSources {
    val all: List<MangaParserSource> by lazy {
        MangaParserSource.entries
            .filterNot { it.isBroken }
            .sortedWith(compareBy({ it.locale }, { it.title.lowercase() }))
    }

    private val byName: Map<String, MangaParserSource> by lazy {
        all.associateBy { it.name }
    }

    fun enabledNames(): List<String> =
        PrefManager.getNullableVal<List<String>>(PrefName.KotatsuEnabledSources, null) ?: emptyList()

    fun setEnabledNames(names: Collection<String>) {
        PrefManager.setVal(PrefName.KotatsuEnabledSources, names.toList())
    }

    fun enabledSources(): List<Lazier<BaseParser>> =
        enabledNames().mapNotNull { byName[it] }.map { source ->
            val locale = source.locale.ifBlank { "multi" }
            val name = "Kotatsu: " + source.title + " (" + locale + ")"
            Lazier<BaseParser>({ KotatsuMangaParser(source) }, name)
        }
}
