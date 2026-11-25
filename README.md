# 🦚 Kujaku

<p align="center">
  <img src="https://raw.githubusercontent.com/Space4414/Kujaku/main/app/src/main/res/drawable-nodpi/kujaku_logo.webp" width="150" alt="Kujaku peacock logo"/>
</p>

<p align="center">
  <strong>An open-source Android app for anime and manga.</strong><br/>
  <sub>Built on Dantotsu, with its own identity and an evolving manga-reading experience.</sub>
</p>

<p align="center">
  <a href="https://github.com/Space4414/Kujaku/actions/workflows/beta.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/Space4414/Kujaku/beta.yml?branch=main&style=for-the-badge&logo=github-actions&label=Build" alt="Build status"/>
  </a>
  <img src="https://img.shields.io/badge/Android-5.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 5.0 and newer"/>
  <img src="https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/License-UPL-8A2BE2?style=for-the-badge" alt="Unabandon Public License"/>
</p>

<p align="center">
  <a href="https://github.com/Space4414/Kujaku/stargazers"><img src="https://img.shields.io/github/stars/Space4414/Kujaku?style=flat&logo=github&color=yellow" alt="GitHub stars"/></a>
  <a href="https://github.com/Space4414/Kujaku/forks"><img src="https://img.shields.io/github/forks/Space4414/Kujaku?style=flat&logo=github&color=blue" alt="GitHub forks"/></a>
  <a href="https://github.com/Space4414/Kujaku/issues"><img src="https://img.shields.io/github/issues/Space4414/Kujaku?style=flat&color=orange" alt="Open issues"/></a>
  <a href="https://github.com/Space4414/Kujaku/pulls"><img src="https://img.shields.io/github/issues-pr/Space4414/Kujaku?style=flat&color=brightgreen" alt="Open pull requests"/></a>
  <img src="https://img.shields.io/github/last-commit/Space4414/Kujaku?style=flat&color=blueviolet" alt="Last commit"/>
  <img src="https://img.shields.io/github/contributors/Space4414/Kujaku?style=flat&color=informational" alt="Contributors"/>
</p>

---

## About

**Kujaku** is an independent, open-source Android project based on [Dantotsu](https://github.com/rebelonion/Dantotsu). It brings anime tracking and manga reading together, with support for external sources and integrations.

Kujaku is actively evolving. It is not a finished or perfectly stable product, and compatibility with third-party sources can change over time. The aim here is to be transparent about what the project is and where its content comes from.

## Features

- 📚 Manga reading through supported external sources.
- 📺 Anime tracking and related integrations.
- 🔌 Support for extensions and external integrations.
- 🔖 AniList account integration.
- 👤 MyAnimeList and Discord integrations.
- ⬇️ Downloads for supported media.
- 🎨 Themes and appearance customization.
- ⚙️ Reader, playback, and application settings.

Feature availability may depend on external services, extensions, and device compatibility.

## Manga source integration

Kujaku integrates **[Kotatsu Parsers Redo](https://github.com/clquwu/kotatsu-parsers-redo)** as a manga source/parser library. This integration provides the parser layer used to access supported manga websites from within the app.

The dependency is currently pinned to a specific upstream revision rather than tracking an unpinned moving branch. Source availability and compatibility remain dependent on the upstream project and the individual websites.

## Get Kujaku

- **[GitHub Actions](https://github.com/Space4414/Kujaku/actions)** — development builds and workflow artifacts.
- **[GitHub Releases](https://github.com/Space4414/Kujaku/releases)** — published releases, when available.

Development builds are primarily for testing and may contain bugs or incomplete features. Always check the workflow result and artifact details before installing a build.

## Build from source

Kujaku is an Android project built with Kotlin and Gradle.

### Requirements

- JDK 17
- Android SDK
- Android Studio (recommended)

### Build the F-Droid Alpha variant

```bash
git clone https://github.com/Space4414/Kujaku.git
cd Kujaku
chmod +x gradlew
./gradlew assembleFdroidAlpha
```

The generated APK is placed under:

```text
app/build/outputs/apk/
```

Other variants are configured in `app/build.gradle`. Google-flavor builds may require a matching Firebase configuration.

## Content, extensions, and responsibility

Kujaku is an application, not a content host. It does not host or distribute manga, anime, or other copyrighted media.

External websites, repositories, extensions, and add-ons are operated by third parties. Kujaku does not control their content, availability, accuracy, or security. Extensions may execute code on your device, so only use sources you trust.

Users are responsible for ensuring that their use of the application and any third-party sources complies with applicable laws and service terms.

## Contributing

Contributions, bug reports, documentation improvements, and thoughtful suggestions are welcome.

- [Report a bug](https://github.com/Space4414/Kujaku/issues/new)
- [Browse open issues](https://github.com/Space4414/Kujaku/issues)
- [Browse pull requests](https://github.com/Space4414/Kujaku/pulls)

When reporting a bug, include your Android version, app build/version, the steps to reproduce it, and relevant logs or screenshots when possible.

## Repository activity

<p align="center">
  <a href="https://star-history.com/#Space4414/Kujaku&Date">
    <img src="https://api.star-history.com/svg?repos=Space4414/Kujaku&type=Date" width="100%" alt="Kujaku star history"/>
  </a>
</p>

<p align="center">
  <img src="https://visitor-badge.laobi.icu/badge?page_id=Space4414.Kujaku&left_color=333333&right_color=8A2BE2" alt="Repository visitor counter"/>
</p>

## Credits

Kujaku stands on the work of other open-source developers and projects. Special thanks to:

- **[Dantotsu](https://github.com/rebelonion/Dantotsu)** — the original project Kujaku is based on.
- **[Kotatsu Parsers Redo](https://github.com/clquwu/kotatsu-parsers-redo)** — manga source/parser integration used by Kujaku.

Please visit the respective upstream repositories for their source code, licenses, and contributor information.

## License

Kujaku is distributed under the **Unabandon Public License (UPL)**. The UPL incorporates GPL-3.0 terms and adds a public-source availability requirement. Read [LICENSE.md](LICENSE.md) for the complete license text.

---

<p align="center">
  <strong>Made with persistence, curiosity, and 🦚.</strong><br/>
  <sub>Kujaku is an independent project and is not an official Dantotsu or Kotatsu product.</sub>
</p>
