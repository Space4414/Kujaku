# Kujaku project notes

## Project guardrails

- Manga changes only. Do not change anime features or anime sources.
- Keep minSdk at 21 or lower; Android 7 support must remain. Never raise minSdk.
- Do not bump Kotlin, Android Gradle Plugin (AGP), or Gradle without telling the owner first.
- Never perform network or disk work on the main thread. Keep it on an appropriate coroutine dispatcher.
- The owner develops from a phone and cannot run Gradle locally. Verify builds through GitHub Actions.

## Kotatsu Integration Recon

Repository snapshot audited: Space4414/Kujaku, branch main, commit 1bd86ca6347ebbe116e305dd9fea0334e5d12b2d. Root AGENTS.md did not exist before this documentation change. This is a manga-side architecture and build inventory; no app or build files were changed.

### Current manga source architecture

- App parser abstraction: app/src/main/java/ani/dantotsu/parsers/MangaParser.kt defines MangaParser, MangaChapter, and MangaImage. MangaParser exposes manga chapter and page loading plus optional image transformation. MangaImage carries a FileUrl, transformation flag, and Tachiyomi Page.
- Source result and AniList matching: app/src/main/java/ani/dantotsu/parsers/BaseParser.kt defines BaseParser and ShowResponse (source result name/link/cover/names/count/extra plus SManga). BaseParser.autoSearch reuses a saved source result for that media/source when present; otherwise it searches with AniList media names and fuzzy matching, then saves the selected mapping keyed by source and media ID.
- Source orchestration: app/src/main/java/ani/dantotsu/parsers/BaseSources.kt defines MangaReadSources and its loadChaptersFromMedia/loadChapters flow. app/src/main/java/ani/dantotsu/parsers/MangaSources.kt defines MangaSources and HMangaSources. MangaSources consumes installed-extension state, wraps each manga extension source in DynamicMangaParser, preserves the user's pinned source ordering, and includes the downloaded/offline source. HMangaSources adds its own manga parser list.
- Extension-facing source API: app/src/main/java/eu/kanade/tachiyomi/source/MangaSource.kt defines MangaSource; CatalogueSource.kt adds catalogue/search operations; ConfigurableSource.kt exposes source-provided preferences; SourceFactory.kt supports sources created by a factory; online/HttpSource.kt implements HTTP manga detail/chapter/page/image operations; ParsedHttpSource.kt provides parsed HTML source behavior.
- Extension APK discovery/loading: app/src/main/java/eu/kanade/tachiyomi/extension/manga/MangaExtensionManager.kt owns installed/available manga extension state and install/update operations. app/src/main/java/eu/kanade/tachiyomi/extension/util/ExtensionLoader.kt scans installed packages under tachiyomi.extension, checks the supported manga extension library version (1.2–1.5), creates a PathClassLoader, reads source-class metadata, and instantiates MangaSource classes or SourceFactory.createSources(). MangaExtension and MangaLoadResult live under app/src/main/java/eu/kanade/tachiyomi/extension/manga/model/.
- Adapter to the app parser API: app/src/main/java/ani/dantotsu/parsers/AniyomiAdapter.kt defines DynamicMangaParser. It adapts extension search, manga details, chapter lists, and page lists to the app's MangaParser models and keeps the HttpSource associated with image data.
- Extension/source registry support also exists in app/src/main/java/eu/kanade/tachiyomi/source/manga/AndroidMangaSourceManager.kt and the tachiyomi.domain.source.manga.service.MangaSourceManager API. The main reading UI's displayed list is assembled by MangaSources.

### Manga models

- Source search result: ShowResponse in parsers/BaseParser.kt.
- Extension source models: app/src/main/java/eu/kanade/tachiyomi/source/model/SManga.kt and SMangaImpl.kt (title, URL, author/artist, description, status, thumbnail, etc.); SChapter.kt and SChapterImpl.kt (URL, name, chapter number, upload date, scanlator); Page.kt (page index/URL/image URL/status/progress); MangasPage.kt (search results and pagination state).
- App parser page/image models: MangaChapter and MangaImage in parsers/MangaParser.kt.
- App media/UI models: app/src/main/java/ani/dantotsu/media/manga/Manga.kt and MangaChapter.kt. The app chapter model carries the source SChapter, display metadata, reading progress, and loaded MangaImage list.
- URL plus request headers: app/src/main/java/ani/dantotsu/Network.kt defines FileUrl(url, headers), which is passed to Glide as a GlideUrl so source-specific headers are retained.

### Source list, chapter loading, reader, and downloads

- MangaSources is initialized from App.kt using the installed manga extension flow. The manga detail screen chooses MangaSources or HMangaSources in MangaReadFragment.kt. MangaReadAdapter.kt displays available source names and selection/language controls; MangaSourceAdapter.kt plus SourceSearchDialogFragment.kt provide manual source search/override. MangaExtensionsFragment.kt and InstalledMangaExtensionsFragment.kt show available and installed manga extension packages.
- Chapter loading runs from media/manga/MediaDetailsViewModel.kt through MangaReadSources.loadChaptersFromMedia, which resolves the saved/automatic ShowResponse and calls the parser's chapter loader. MangaReadFragment.loadChapters dispatches this work on Dispatchers.IO. DynamicMangaParser uses the extension HttpSource for manga details/chapter lists and obtains Page objects for chapter images.
- Selecting a chapter in media/manga/MangaReadFragment.kt opens media/manga/mangareader/ChapterLoaderDialog.kt. The dialog calls MediaDetailsViewModel.loadMangaChapterImages on Dispatchers.IO, then launches MangaReaderActivity. MangaReaderActivity.kt presents pages through BaseImageAdapter.kt, ImageAdapter.kt, and DualPageAdapter.kt; ImageAdapter loads MangaImage.url, applies parser/image transformations, and uses the reader's Glide bitmap loader.
- Chapter downloads are implemented by download/manga/MangaDownloaderService.kt. MangaReadFragment.onMangaChapterDownloadClick asks DynamicMangaParser for ImageData entries (Page plus HttpSource), then enqueues a download task. The service runs page fetch/decode on Dispatchers.IO via media/manga/MangaCache.kt ImageData.fetchAndProcessImage, which calls HttpSource.getImage(page), retries failures, and saves decoded images as JPEGs through DocumentFile. Cover/banner metadata uses a separate HttpURLConnection helper on Dispatchers.IO.

### Image loading and HTTP client

- The app uses Glide 4.16.0, not Coil. app/src/main/java/ani/dantotsu/others/GlideApp.kt registers Glide's OkHttp URL loader with the app-level okHttpClient. app/src/main/java/ani/dantotsu/Network.kt initializes that client from eu/kanade/tachiyomi/network/NetworkHelper.kt. NetworkHelper.client is the shared OkHttp client used by manga HttpSource instances and Glide; the network helper configures its cookie jar/interceptors/cache and also exposes a longer-timeout download client.
- Reader requests preserve FileUrl headers using GlideUrl. MangaCache fetches page responses through HttpSource.getImage. Bitmap and download work is dispatched off the UI thread.

### Dependency injection and settings

- Dependency injection/service lookup uses Injekt (uy.kohesive.injekt:injekt-core:1.16.+), not Hilt. app/src/main/java/ani/dantotsu/App.kt initializes the global Injekt container and imports the app/preference modules; app and manga extension services retrieve dependencies through Injekt.get/injectLazy. NetworkHelper and MangaExtensionManager are obtained from that container.
- General settings are built as app screens: settings/SettingsActivity.kt supplies Settings data to SettingsAdapter.kt; settings/SettingsMangaActivity.kt builds manga settings; settings/ReaderSettingsActivity.kt builds reader settings. Manga extension preferences use settings/extensionprefs/MangaPreferenceFragmentCompat.kt, an AndroidX PreferenceFragmentCompat that asks a ConfigurableSource to populate its PreferenceScreen.
- AniList matching/linking is app-side, not provided by the source extension API: BaseParser.autoSearch uses the AniList Media's display names to find a source ShowResponse, then stores the chosen result per source/media ID. MediaDetailsViewModel.kt loads chapters using that mapping. Manual choice through MangaSourceAdapter overrides it. Media.kt provides mangaName(), choosing the AniList Romaji name for Japan-origin manga and the main name otherwise.

### Kujaku build baseline

- Android SDK: minSdk 21, targetSdk 35, compileSdk 35. Do not raise minSdk.
- Kotlin 2.1.0 (root build.gradle); AGP 8.7.3 (root build.gradle); Gradle 8.9 (gradle/wrapper/gradle-wrapper.properties).
- OkHttp 5.0.0-alpha.12 (app/build.gradle); Glide 4.16.0.
- Coroutines APIs are used, but no explicit kotlinx-coroutines version is declared in the project build files. There is no version catalog or dependency lockfile in the repository, so the exact resolved coroutines artifact version is not pinned by this snapshot.
- Core library desugaring is not enabled in app/build.gradle.
- No Gradle version catalog: dependencies/versions are declared in Groovy build.gradle files; settings.gradle.kts is the settings file.
- R8 configuration is in app/build.gradle release buildType, with app/proguard-rules.pro. app/build.gradle also references app/proguard-gson.pro, but that file is absent from the audited tree (GitHub contents lookup returned 404); check this before relying on a minified release build.

### GitHub Actions

- .github/workflows/beta.yml — push builds (except main, l10n_dev_crowdin, and custom-download-location) and v*.*.* tags; ignores README-only changes. Runs ./gradlew assembleGoogleAlpha and uploads the APK artifact. Discord/Telegram publishing is guarded to run only in the upstream rebelonion/Dantotsu repository. This is the only workflow that builds the app; it does not run a test suite or define workflow_dispatch. It explicitly ignores main, so a documentation commit directly to main will not trigger this build.
- .github/workflows/bug_greetings.yml — automated greeting on newly opened bug issues.
- .github/workflows/feature_greetings.yml — automated greeting on newly opened feature-request issues.
- .github/workflows/extension-issue-handling.yml — handles extension-related issue creation/labels with repository issue automation.
- .github/workflows/pr_greetings.yml — greeting automation for pull requests, with owner/Weblate exclusions.
- Before this recon branch was created, the GitHub Actions API reported zero runs for the repository. A non-main documentation branch is required to exercise the existing APK build without changing workflow files.

### Tsuki reference implementation

- Space4414/Tsuki was not accessible (GitHub returned 404). The public TsukiAndroid/Tsuki repository was accessible on branch devel; inspected HEAD was 89b8d9cf6bb53303f340505f46c2ad2c1c8ac5b7. Findings below are from that repository, not the unavailable Space4414 fork.
- Exact dependency in app/build.gradle: com.github.clquwu:kotatsu-parsers-redo:dc223c3d80. The version is declared in gradle/libs.versions.toml as dc223c3d80. GitHub resolves that short pin to full parser commit dc223c3d8098415b29cf1acfedc401a88d01c9fe (commit message: Update KdtScans.kt).
- app/src/main/kotlin/io/github/landwarderer/futon/core/parser/MangaLoaderContextImpl.kt implements the library MangaLoaderContext. It receives a @MangaHttpClient OkHttpClient and MutableCookieJar via Hilt, supplies source config, locale/default user agent and WebView hooks, and implements redrawImageResponse by decoding a mutable bitmap, wrapping it in BitmapWrapper, invoking the parser-provided redraw callback, and encoding the result. The scrambling/descrambling algorithm itself is supplied by the parser callback/library source, not a separate app-wide algorithm.
- core/network/NetworkModule.kt provides @BaseHttpClient and @MangaHttpClient. The base client wires the cookie jar and network facilities including CloudFlareInterceptor and RateLimitInterceptor; the manga client adds the cache/common-header interceptors. core/network/CloudFlareInterceptor.kt calls Kotatsu CloudFlareHelper to classify challenge responses. Browser cookie clearance is separately handled by app/src/main/kotlin/io/github/landwarderer/futon/browser/cloudflare/CloudflareBypassManager.kt, which uses the browser/WebView cookie path for cf_clearance.
- core/prefs/SourceSettings.kt implements MangaSourceConfig, storing parser ConfigKey values in source-specific SharedPreferences. core/image/MangaSourceHeaderInterceptor.kt handles Coil 3 image requests by attaching the manga-source marker or custom-source referer/browser user-agent. core/parser/BitmapWrapper.kt adapts Android Bitmap to the Kotatsu bitmap abstraction.
- Tsuki comparison baseline: Kotlin 2.2.10, AGP 8.13.2, Gradle wrapper 9.0.0, OkHttp 5.2.1, coroutines 1.10.2, Coil 3, and core library desugaring enabled. These differ from Kujaku and are not a reason to upgrade Kujaku automatically.

### Integration risks to resolve before implementation

1. Kotatsu's MangaLoaderContext is a different abstraction from Kujaku's MangaParser/DynamicMangaParser and Tachi extension types. A manga-only adapter will need an explicit mapping for search results, SManga/SChapter/Page, FileUrl headers, source identity/config, and reader images; replacing anime or extension behavior is out of scope.
2. Keep minSdk 21. Tsuki enables core library desugaring while Kujaku does not; check the pinned parser's Android/runtime requirements on Android 7 through CI before choosing whether a desugaring change is required.
3. Dependency baselines differ materially: Kujaku Kotlin 2.1.0 / AGP 8.7.3 / Gradle 8.9 / OkHttp 5.0.0-alpha.12 versus Tsuki Kotlin 2.2.10 / AGP 8.13.2 / Gradle 9.0.0 / OkHttp 5.2.1. Do not bump Kotlin, AGP, or Gradle without owner approval; validate dependency compatibility in CI.
4. Coroutines' resolved Kujaku version is currently unknown from checked-in declarations. Resolve through CI dependency reporting before diagnosing a binary/API conflict; do not guess a version.
5. Check the missing app/proguard-gson.pro reference before minified release validation. The current beta workflow builds Google Alpha, not a minified release, so a passing beta build would not validate this path.
6. The existing beta workflow excludes main. Preserve that fact when planning documentation or implementation verification; no workflow change is authorized by this recon.
