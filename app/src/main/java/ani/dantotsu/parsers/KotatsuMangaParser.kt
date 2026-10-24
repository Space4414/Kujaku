package ani.dantotsu.parsers

import ani.dantotsu.App
import ani.dantotsu.FileUrl
import ani.dantotsu.media.manga.ImageData
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.koitharu.kotatsu.parsers.model.ContentRating as KotatsuContentRating
import org.koitharu.kotatsu.parsers.model.ContentType as KotatsuContentType
import org.koitharu.kotatsu.parsers.model.Manga as KotatsuManga
import org.koitharu.kotatsu.parsers.model.MangaChapter as KotatsuChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaState as KotatsuState
import org.koitharu.kotatsu.parsers.model.MangaTag as KotatsuTag
import java.util.Locale

/** Adapts one Kotatsu parser to Kujaku's existing manga-source contract. */
class KotatsuMangaParser(private val kotatsuSource: MangaParserSource) : MangaParser() {
    private val loaderContext: KotatsuMangaLoaderContext by lazy {
        val context = App.instance?.applicationContext
            ?: throw IllegalStateException("Application context is not available")
        KotatsuMangaLoaderContext(context)
    }

    private val kotatsuParser by lazy { loaderContext.newParserInstance(kotatsuSource) }
    private val downloadClient: OkHttpClient by lazy {
        loaderContext.httpClient.newBuilder().addInterceptor(kotatsuParser).build()
    }

    override val name: String = "Kotatsu: " + kotatsuSource.title + " (" + kotatsuSource.locale.ifBlank { "multi" } + ")"
    override val saveName: String = "Kotatsu_" + kotatsuSource.name
    override val hostUrl: String get() = kotatsuParser.domain
    override val isNSFW: Boolean = kotatsuSource.contentType == KotatsuContentType.HENTAI
    override val language: String = kotatsuSource.locale
        .takeIf { it.isNotBlank() }
        ?.let { Locale.forLanguageTag(it).displayLanguage }
        ?: "Multiple"

    override suspend fun search(query: String): List<ShowResponse> = try {
        withContext(Dispatchers.IO) {
            val order = kotatsuParser.availableSortOrders.first()
            kotatsuParser.getList(0, order, MangaListFilter(query = query)).map(::toShowResponse)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Logger.log(error)
        emptyList()
    }

    override suspend fun loadChapters(
        mangaLink: String,
        extra: Map<String, String>?,
        sManga: SManga,
    ): List<MangaChapter> = try {
        withContext(Dispatchers.IO) {
            val details = kotatsuParser.getDetails(toKotatsuManga(mangaLink, extra, sManga))
            updateSManga(sManga, details)
            details.chapters.orEmpty().map(::toKujakuChapter)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Logger.log(error)
        emptyList()
    }

    override suspend fun loadImages(chapterLink: String, sChapter: SChapter): List<MangaImage> = try {
        withContext(Dispatchers.IO) {
            val headers = kotatsuParser.getRequestHeaders()
            kotatsuParser.getPages(sChapter.toKotatsuChapter()).mapIndexed { index, page ->
                val imageUrl = kotatsuParser.getPageUrl(page)
                MangaImage(
                    url = FileUrl(imageUrl, headers.toKujakuHeaders()),
                    page = Page(index, page.url, imageUrl),
                )
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Logger.log(error)
        emptyList()
    }

    override suspend fun imageList(sChapter: SChapter): List<ImageData> = try {
        withContext(Dispatchers.IO) {
            val headers = kotatsuParser.getRequestHeaders()
            kotatsuParser.getPages(sChapter.toKotatsuChapter()).mapIndexed { index, page ->
                val imageUrl = kotatsuParser.getPageUrl(page)
                ImageData.forKotatsu(
                    page = Page(index, page.url, imageUrl),
                    imageUrl = imageUrl,
                    headers = headers,
                    client = downloadClient,
                )
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Logger.log(error)
        emptyList()
    }

    private fun toShowResponse(manga: KotatsuManga): ShowResponse {
        val sManga = SManga.create().apply {
            title = manga.title
            url = manga.url
            author = manga.authors.firstOrNull()
            artist = manga.authors.drop(1).takeIf { it.isNotEmpty() }?.joinToString(", ")
            description = manga.description
            genre = manga.tags.joinToString(", ") { it.title }
            status = manga.state.toSMangaStatus()
            thumbnail_url = manga.coverUrl ?: manga.largeCoverUrl
            initialized = true
        }
        val headers = kotatsuParser.getRequestHeaders().toKujakuHeaders()
        val extra = mutableMapOf(
            EXTRA_ID to manga.id.toString(),
            EXTRA_PUBLIC_URL to manga.publicUrl,
            EXTRA_ALT_TITLES to manga.altTitles.joinToString("\n"),
        )
        return ShowResponse(
            name = manga.title,
            link = manga.url,
            coverUrl = FileUrl(manga.coverUrl ?: manga.largeCoverUrl ?: defaultImage, headers),
            otherNames = manga.altTitles.toList(),
            total = null,
            extra = extra,
            sManga = sManga,
        )
    }

    private fun toKotatsuManga(link: String, extra: Map<String, String>?, manga: SManga): KotatsuManga {
        val url = manga.url.ifBlank { link }
        val publicUrl = extra?.get(EXTRA_PUBLIC_URL)?.takeIf { it.isNotBlank() } ?: absoluteUrl(url)
        val authors = listOfNotNull(manga.author, manga.artist)
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        val tags = manga.getGenres().orEmpty().map {
            KotatsuTag(title = it, key = it, source = kotatsuParser.source)
        }.toSet()
        return KotatsuManga(
            id = extra?.get(EXTRA_ID)?.toLongOrNull() ?: url.hashCode().toLong(),
            title = manga.title,
            altTitles = extra?.get(EXTRA_ALT_TITLES)
                ?.split("\n")
                ?.filter { it.isNotBlank() }
                ?.toSet()
                .orEmpty(),
            url = url,
            publicUrl = publicUrl,
            rating = 0f,
            contentRating = if (isNSFW) KotatsuContentRating.ADULT else null,
            coverUrl = manga.thumbnail_url,
            tags = tags,
            state = manga.status.toKotatsuState(),
            authors = authors,
            description = manga.description,
            source = kotatsuParser.source,
        )
    }

    private fun updateSManga(target: SManga, details: KotatsuManga) {
        target.title = details.title
        target.url = details.url
        target.author = details.authors.firstOrNull()
        target.artist = details.authors.drop(1).takeIf { it.isNotEmpty() }?.joinToString(", ")
        target.description = details.description
        target.genre = details.tags.joinToString(", ") { it.title }
        target.status = details.state.toSMangaStatus()
        target.thumbnail_url = details.coverUrl ?: details.largeCoverUrl ?: target.thumbnail_url
        target.initialized = true
    }

    private fun toKujakuChapter(chapter: KotatsuChapter): MangaChapter {
        val scanlatorParts = listOfNotNull(
            chapter.scanlator?.takeIf { it.isNotBlank() },
            chapter.branch?.takeIf { it.isNotBlank() },
        ).distinct()
        val scanlator = scanlatorParts.joinToString(" / ").takeIf { it.isNotEmpty() }
        val chapterTitle = chapter.title?.takeIf { it.isNotBlank() }
            ?: chapter.numberString()
            ?: "Chapter"
        val sChapter = SChapter.create().apply {
            url = chapter.url
            name = chapterTitle
            chapter_number = chapter.number
            date_upload = chapter.uploadDate
            this.scanlator = scanlator
        }
        return MangaChapter(
            number = chapter.numberString() ?: chapter.title?.takeIf { it.isNotBlank() } ?: "0",
            link = chapter.url,
            title = chapter.title,
            description = null,
            scanlator = scanlator ?: "Unknown",
            sChapter = sChapter,
            date = chapter.uploadDate,
        )
    }

    private fun SChapter.toKotatsuChapter(): KotatsuChapter = KotatsuChapter(
        id = url.hashCode().toLong(),
        title = name,
        number = chapter_number,
        volume = 0,
        url = url,
        scanlator = scanlator,
        uploadDate = date_upload,
        branch = null,
        source = kotatsuParser.source,
    )

    private fun absoluteUrl(url: String): String {
        if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            return url
        }
        val base = kotatsuParser.domain.let {
            if (it.startsWith("http://", true) || it.startsWith("https://", true)) it else "https://" + it
        }
        return runCatching { base.toHttpUrl().resolve(url)?.toString() }.getOrNull() ?: url
    }

    private fun Headers.toKujakuHeaders(): Map<String, String> =
        names().associateWith { header -> values(header).joinToString(", ") }

    private fun Int.toKotatsuState(): KotatsuState? = when (this) {
        SManga.ONGOING -> KotatsuState.ONGOING
        SManga.COMPLETED, SManga.PUBLISHING_FINISHED -> KotatsuState.FINISHED
        SManga.CANCELLED -> KotatsuState.ABANDONED
        SManga.ON_HIATUS -> KotatsuState.PAUSED
        SManga.LICENSED -> KotatsuState.RESTRICTED
        else -> null
    }

    private fun KotatsuState?.toSMangaStatus(): Int = when (this) {
        KotatsuState.ONGOING -> SManga.ONGOING
        KotatsuState.FINISHED -> SManga.COMPLETED
        KotatsuState.ABANDONED -> SManga.CANCELLED
        KotatsuState.PAUSED -> SManga.ON_HIATUS
        KotatsuState.UPCOMING -> SManga.UNKNOWN
        KotatsuState.RESTRICTED -> SManga.LICENSED
        null -> SManga.UNKNOWN
    }

    private companion object {
        const val EXTRA_ID = "kotatsu.id"
        const val EXTRA_PUBLIC_URL = "kotatsu.public_url"
        const val EXTRA_ALT_TITLES = "kotatsu.alt_titles"
    }
}