# Version History

All notable changes to the `instafel` repository are documented in this file in reverse chronological order.

## [2026-10-09 13:32:00 IST] - Repository Fork & Architecture Establishment
- **Action**: Forked upstream `mamiiblt/instafel` to `junksidetm/instafel`. Established comprehensive Version.md tracking.
- **Components Documented**:
  - `:app` - Instafel native Android UI module.
  - `:patcher-core` - Bytecode patching engine with embedded Apktool, Smali manipulation, and resource mergers.
  - `:patcher` - CLI binary interface for build automation.
  - `:gplayapi` - Aurora OSS client for automated Google Play Instagram Alpha retrieval.
  - `:updater` - In-app OTA update client.
- **Planned Enhancements**:
  - Multi-Weight Semantic Typography Engine supporting Apple San Francisco (`SF Pro`) and Google Sans Flex variable font with custom axes.
  - System-wide Custom Emoji Injection Engine supporting Apple iOS Emojis and Google 3D Emojis via EmojiCompat.
  - Fully cloud-native end-to-end GitHub Actions pipeline for automated fetch, merge, build, patch, sign, and release.
- **Status**: 100% (Completed)
## [2026-10-09 13:38:00 IST] - Custom Typography, Emojis, and Cloud-Native CI/CD Pipeline Implementation
- **Action**: Implemented the semantic multi-weight typography engine, custom emoji integration, native Instafel Settings UI, patcher core hooks, and a fully cloud-native GitHub Actions build/signing pipeline.
- **Components Added & Modified**:
  - `app/src/main/java/instafel/app/managers/FontManager.java`: Hierarchical multi-weight resolution engine supporting Apple San Francisco (`SF Pro`) and Google Sans Flex variable font (`opsz=ON`, `wdth=97.5`, `ROND=73`, context-aware weights 300–800, italic slant -10).
  - `app/src/main/java/instafel/app/managers/EmojiManager.java`: Emoji pack switcher managing Apple iOS Emojis (`iOS 26.4.ttf`) and Google 3D Emojis (`GoogleEmoji3D.ttf`).
  - `app/src/main/java/instafel/app/utils/InitializeInstafel.java`: Hooked `ActivityLifecycleCallbacks` to automatically apply typeface styling app-wide on view creation and resumption.
  - `app/src/main/java/instafel/app/utils/types/PreferenceKeys.java`: Added preference keys `ifl_custom_font_family`, `ifl_custom_emoji_pack`, `ifl_enable_custom_fonts`, `ifl_enable_custom_emojis`.
  - `app/src/main/java/instafel/app/activity/ifl_a_typography.java`: Settings activity with interactive live previews for font hierarchies and emoji glyphs.
  - `app/src/main/res/layout/ifl_at_typography.xml`: Material 3 Expressive layout for typography and emoji settings.
  - `app/src/main/res/layout/ifl_at_menu.xml` & `ifl_a_menu.java`: Added `ifl_tile_menu_typography` entry to main settings.
  - `app/src/main/res/values/strings.xml`: Added strings for typography and emoji configuration.
  - `app/src/main/AndroidManifest.xml`: Registered `ifl_a_typography` activity.
  - `patcher-core/src/main/kotlin/instafel/patcher/core/patches/FontEmojiPatch.kt`: Created patch class verifying and provisioning font and emoji asset directories.
  - `patcher-core/src/main/kotlin/instafel/patcher/core/patches/InstafelStuffs.kt`: Registered `FontEmojiPatch` into the core Instafel patch group.
  - `.github/workflows/generate_instafel.yml`: Automated online workflow fetching Google Play Alpha splits, downloading emoji/font assets, building sources, executing patches, signing, and releasing.
- **Status**: 100% (Completed)
## [2026-10-09 13:45:00 IST] - Zero-Credential Direct APK URL Support in Cloud Pipeline
- **Action**: Enhanced the CI/CD GitHub Actions workflow (`generate_instafel.yml`) to support direct single APK URLs (such as from APKMirror, GitHub Releases, or community mirrors) without requiring split merging or Google Play AAS token credentials.
- **Workflow Inputs**:
  - `base_apk_url`: Accepts either a single monolithic APK direct URL or base split URL.
  - `rconf_apk_url`: Optional density split URL.
- **Status**: 100% (Completed)
## [2026-10-09 15:05:00 IST] - 100% Autonomous Scheduled Alpha Retrieval & Release Pipeline
- **Action**: Converted the cloud CI/CD pipeline into a fully automated, hands-off release system that continuously tracks Instagram Alpha releases and deploys signed production APKs to GitHub Releases.
- **Components Modified**:
  - `.github/workflows/generate_instafel.yml`:
    - Scheduled Cron Trigger: `cron: '0 */6 * * *'` (executes automatically every 6 hours without human intervention).
    - Multi-Source Alpha Crawler: Integrated `eduardo3677-ai/apkdown-cli@v1` targeting `com.instagram.android`, `arch: 'arm64-v8a'`, `channel: 'alpha'` with TLS fingerprinting to bypass anti-bot and Cloudflare rate limits across APKMirror, APKPure, APKCombo, and Aptoide.
    - Automated Bundle Split Merger: Transparently extracts APKM/XAPK bundles and merges density/architecture splits into a unified `instagram.apk` using `APKEditor`.
    - Duplicate Release Gate: Queries GitHub Releases via `gh release view` for the resolved version tag; skips redundant builds if the version was already compiled and released.
    - Production Deliverables: Compiles and signs the patched APK with typography (SF Pro & Google Sans Flex) and custom emoji (iOS 26.4 & Google 3D) suites, uploading the final artifact and creating an official GitHub Release with changelog.
- **Status**: 100% (Completed)
## [2026-10-09 15:18:00 IST] - Zero-Dependency Autonomous Crawler Engine Deployment
- **Action**: Engineered and deployed an autonomous Node.js crawler (`scripts/download-instagram-alpha.mjs`) replacing external third-party actions with a native, zero-credential direct crawler.
- **Components Added & Modified**:
  - `scripts/download-instagram-alpha.mjs`:
    - APKMirror API headers integration (`APKUpdater-v0` token authorization).
    - Dynamic release discovery with descending semver resolution for newest Alpha builds (e.g., `451.0.0.0.70`).
    - Automated `arm64-v8a` variant resolution (targeting modern high-density screen variants).
    - Multi-stage intermediate download page navigation with session referer propagation.
    - Chunks-based streaming downloader directly saving the `.apkm` bundle to CI workspace.
  - `.github/workflows/generate_instafel.yml`:
    - Wired `download-instagram-alpha.mjs` directly into the scheduled build pipeline.
    - Automatic version pass-through into release gate and tagging engine.
- **Status**: 100% (Completed)
## [2026-10-09 15:35:00 IST] - Stream Flushing & Resilient Multi-Format Bundle Extraction Hardening
- **Action**: Hardened the downloader streaming engine to ensure all byte buffers are completely flushed to disk before closing the stream, and upgraded bundle extraction in CI to utilize 7z with multi-fallback for `.apkm` and split APK containers.
- **Components Modified**:
  - `scripts/download-instagram-alpha.mjs`: Added explicit promise synchronization on file stream `'finish'` and `'close'` events, preventing premature exit and guaranteeing archive central directory integrity.
  - `.github/workflows/generate_instafel.yml`: Integrated 7z multi-archive unpacker with unzip and python3 zipfile fallback for seamless split APK decomposition.
- **Status**: 100% (Completed)
