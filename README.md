# 🦚 Kujaku

<p align="center">
  <img src="https://raw.githubusercontent.com/Space4414/Kujaku/main/app/src/main/res/drawable-nodpi/kujaku_logo.png" width="150" alt="Kujaku peacock logo"/>
</p>

<p align="center">
  <strong>A smooth, good-looking Android app for anime and manga, with sources built right in.</strong><br/>
  <sub>Built on Dantotsu, made for people who don't want to install extensions (and for older Android phones too).</sub>
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

## What is Kujaku?

**Kujaku** is an open-source Android app for tracking and reading anime and manga. It's built on top of [Dantotsu](https://github.com/rebelonion/Dantotsu), syncs with your AniList account, and comes with manga sources already built in, so you can start reading without hunting for extensions.

It's a personal project that I decided to share with the community, so expect an app that's made with care but still growing. This is another **vibe-coded** project of mine so nothing is guaranteed but I'll try.

## Why I made it

A few reasons, honestly:

- **The UI.** The main reason I built Kujaku is how it looks and feels. I wanted a smooth, clean experience, and Dantotsu's foundation gave me a great base to build that on.
- **Older Android phones.** Keiyoushi extensions stopped supporting Android 7 and now need Android 8+. I use a legacy Android device myself, and I didn't want to be locked out of reading manga just because my phone is old. Kujaku supports Android 5.0 and up. You can use the in-app sources to Android 5+ phones too not just Android 7+ devices.
- **No extension hassle.** I never liked installing extensions. Kujaku comes with [Kotatsu Parsers Redo](https://github.com/clquwu/kotatsu-parsers-redo) built in as a source, so you don't have to install anything extra to find manga.

## Why "Kujaku"?

Kujaku (孔雀) means *peacock* in Japanese. I saw a video of Malfoid casting a Patronus charm, and it turned out to be a peacock. I loved that idea so much that I decided to name an app after it. 🦚

## Features

- ✨ **Smooth, polished UI** that's easy on the eyes.
- 📚 **Built-in manga sources** through Kotatsu Parsers Redo. No extensions needed, and the sources keep getting updates as the upstream project updates.
- 🔌 **Extension support** for people who still want to use them (see [Known issues](#known-issues--roadmap)).
- 🔖 **AniList integration** to track what you read and watch.
- 👤 **MyAnimeList** and **Discord Rich Presence** support. Your Discord profile shows what you're reading as *Watching Kujaku*.
- ⬇️ **Downloads** for supported media.
- 🎨 **Themes and appearance customization.**
- ⚙️ **Reader, playback, and app settings** to set things up your way.
- 📱 **Works on Android 5.0 and up**, including older phones.

Feature availability may depend on external services, sources, and your device.

## Screenshots

<!-- Add your screenshots here. Example layout:

<p align="center">
  <img src="screenshots/home.png" width="22%" alt="Home"/>
  <img src="screenshots/reader.png" width="22%" alt="Reader"/>
  <img src="screenshots/details.png" width="22%" alt="Details"/>
  <img src="screenshots/settings.png" width="22%" alt="Settings"/>
</p>

-->

*Screenshots coming soon.*

## Known issues & roadmap

Kujaku is almost ready for its first stable release, but a few things are still not working properly, and I'd rather be upfront about them:

- ❌ **Anime streaming** doesn't work yet.
- ❌ **Installed manga extensions** can't fetch manga from their sources yet. The built-in Kotatsu Parsers Redo sources are not affected.

**Plan:** I'm fixing manga extensions first, then moving on to anime streaming. If you find other bugs, please [open an issue](https://github.com/Space4414/Kujaku/issues/new).

## Manga source integration

Kujaku integrates **[Kotatsu Parsers Redo](https://github.com/clquwu/kotatsu-parsers-redo)** as a built-in manga source. This is the parser layer that lets the app reach supported manga websites without needing any extension installed.

When Kotatsu Parsers Redo gets updates, Kujaku can pick them up too. Source availability still depends on the upstream project and on the individual websites, so a source can break at any time when a site changes.

## Get Kujaku

The first **stable release hasn't been published yet**. When it is, it will be available on the Releases page.

- **[GitHub Releases](https://github.com/Space4414/Kujaku/releases)**: stable releases (coming soon).
- **[GitHub Actions](https://github.com/Space4414/Kujaku/actions)**: development builds and workflow artifacts.

Development builds are for testing and may contain bugs or unfinished features. Always check the workflow result and artifact details before installing one.

## Also check out this : 
[Tsuki](https://github.com/TsukiAndroid/Tsuki),
It's another manga reading app of mine that was based on Yukimi and Futon.

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

Users are responsible for making sure their use of the app and any third-party sources follows applicable laws and service terms.

## Contributing

Bug reports, ideas, documentation fixes, and pull requests are all welcome.

- [Report a bug](https://github.com/Space4414/Kujaku/issues/new)
- [Browse open issues](https://github.com/Space4414/Kujaku/issues)
- [Browse pull requests](https://github.com/Space4414/Kujaku/pulls)

When reporting a bug, please include your Android version, the app build/version, the steps to reproduce it, and logs or screenshots if you can.

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

Kujaku wouldn't exist without the work of other open-source developers. Big thanks to:

- **[Dantotsu](https://github.com/rebelonion/Dantotsu)**: the original project Kujaku is based on.
- **[Kotatsu Parsers Redo](https://github.com/clquwu/kotatsu-parsers-redo)**: the built-in manga source/parser library.

Please visit their repositories for source code, licenses, and contributor information.

## License

Kujaku is distributed under the **Unabandon Public License (UPL)**. The UPL incorporates GPL-3.0 terms and adds a public-source availability requirement. Read [LICENSE.md](LICENSE.md) for the complete license text.

---

<p align="center">
  <strong>Made with persistence, curiosity, and 🦚.</strong><br/>
  <sub>Kujaku is an independent project and is not an official Dantotsu or Kotatsu product.</sub>
</p>
