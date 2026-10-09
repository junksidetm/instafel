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
## [2026-10-09 15:46:00 IST] - Resource Attribute Linking Resolution & AAPT Compatibility
- **Action**: Resolved AAPT resource linking compilation failure in `:app` module by correcting tile background attribute references in the typography settings layout and adding declared theme attributes.
- **Components Modified**:
  - `app/src/main/res/layout/ifl_at_typography.xml`: Corrected preview CardView `app:cardBackgroundColor` references from undeclared `?ifl_attr_tile_background` to canonical theme attribute `?ifl_attr_tile_color`.
  - `app/src/main/res/values/attrs.xml`: Added explicit `<attr name="ifl_attr_tile_background" format="color" />` declaration as fallback alias.
  - `app/src/main/res/values/styles.xml`: Mapped `ifl_attr_tile_background` in `ifl_theme_light` and `ifl_theme_dark` styles for bulletproof backward and theme compatibility.
- **Status**: 100% (Completed)
## [2026-10-09 15:53:00 IST] - Patcher Core Kotlin Compilation Visibility Alignment
- **Action**: Resolved Kotlin compilation failure (`:patcher-core:compileKotlin`) caused by private visibility constraint on `projectDir` within `SmaliUtils`.
- **Components Modified**:
  - `patcher-core/src/main/kotlin/instafel/patcher/core/patches/FontEmojiPatch.kt`: Imported and referenced global `Env.PROJECT_DIR` for asset directory resolution.
  - `patcher-core/src/main/kotlin/instafel/patcher/core/utils/SmaliUtils.kt`: Relaxed `projectDir` property visibility from private to public `val projectDir: String` for idiomatic instance access.
- **Status**: 100% (Completed)
## [2026-10-09 16:08:00 IST] - Generation ID Safe Fallback, Synchronous CLI Thread Execution & Dual Variant Packaging
- **Action**: Fixed uninitialized `GENERATION_ID` exception during non-production builds, converted asynchronous CLI command thread spawns to synchronous joined execution, and restructured output artifact packaging to distribute both unclone and clone APK deliverables.
- **Components Modified**:
  - `patcher-core/src/main/kotlin/instafel/patcher/core/jobs/BuildProject.kt`: Pre-initialized `IFL_VERSION` and `GENERATION_ID` with timestamp-based epoch fallbacks before checking production mode flags, preventing reflection crashes in `generateBuildInfo`.
  - `patcher/src/main/kotlin/instafel/patcher/commands/BuildCmd.kt`: Synchronously joined background worker thread (`thread.join()`) to ensure complete APK signing and manifest generation before CLI exit.
  - `patcher/src/main/kotlin/instafel/patcher/commands/RunPatch.kt`: Synchronously joined patch worker thread.
  - `patcher/src/main/kotlin/instafel/patcher/commands/InitProject.kt`: Synchronously joined project init worker thread.
  - `.github/workflows/generate_instafel.yml`:
    - Updated `Package & Prepare Output Deliverables` to resolve APKs from `instagram/build/` dynamically.
    - Exported both Default (`instafel-v${VERSION}-arm64-v8a.apk`), Unclone, and Clone (`instafel-v${VERSION}-arm64-v8a-clone.apk`) signed variants into `release_apks/`.
    - Added `--clobber` support to `gh release create` for resilient GitHub Releases publication.
- **Status**: 100% (Completed)
## [2026-10-09 16:14:00 IST] - Executable Patcher ShadowJar Resolution Fix
- **Action**: Corrected CI patcher JAR selection logic in GitHub Actions workflow to specifically target the standalone executable shadowJar rather than the non-executable `ifl-patcher-core` library archive.
- **Components Modified**:
  - `.github/workflows/generate_instafel.yml`:
    - Updated `find` pattern to `ifl-patcher-*.jar ! -name "*-core*"` across build, decompilation, patch execution, and signing steps.
    - Exported `patcher_jar` environment variable to ensure deterministic invocation of the CLI JAR with valid manifest main attributes.
- **Status**: 100% (Completed)
## [2026-10-09 16:27:00 IST] - Release Creation & Asset Upload Command Hardening
- **Action**: Corrected GitHub CLI release command syntax to conditionally execute `gh release create` for new tags or `gh release upload --clobber` for existing releases, eliminating invalid `--clobber` flag invocation on create.
- **Components Modified**:
  - `.github/workflows/generate_instafel.yml`: Added branch logic checking if tag exists via `gh release view` before deciding between `create` and `upload`.
- **Status**: 100% (Completed)
## [2026-10-09 16:30:00 IST] - Architecture Delegation: Upstream Ingestion via Alpha-Insta
- **Action**: Decoupled Instagram Alpha ingestion and bundle merging into dedicated repository `junksidetm/Alpha-Insta`. Instafel now consumes verified, standalone Alpha APK releases directly from `Alpha-Insta`, adding automated duplicate mod gating.
- **Components Modified**:
  - `.github/workflows/generate_instafel.yml`:
    - Replaced local APKMirror crawler and bundle merger with upstream release query from `junksidetm/Alpha-Insta`.
    - Added automated duplicate mod check against `junksidetm/instafel` releases; skips execution if `v${VERSION}` was already modded and published.
    - Added `gh release download` step streaming clean standalone APKs directly into Instafel patcher pipeline.
- **Status**: 100% (Completed)
## [2026-10-09 16:39:00 IST] - Documentation Overhaul & Ecosystem Transparency
- **Action**: Completely overhauled project `README.md` to comprehensively document the typography hierarchy engine, custom emoji integration, Alpha-Insta upstream ingest pipeline, multi-platform mirrors, and deliverable specifications.
- **Components Modified**:
  - `README.md`: Documented Apple SF Pro hierarchy, Google Sans Flex variable axis formulas, iOS 26.4 / Google 3D emoji suites, Mermaid architecture diagram, deliverable variant breakdown, and GitLab / Codeberg mirror synchronization.
- **Status**: 100% (Completed)
## [2026-10-09 16:47:00 IST] - Deliverable Standardization & Redundant Artifact Elimination
- **Action**: Standardized release deliverables on explicit `unclone` and `clone` naming conventions, eliminated redundant duplicate unclone artifact from CI packaging pipeline, and pruned duplicate asset from live release.
- **Components Modified**:
  - `.github/workflows/generate_instafel.yml`: Updated packaging logic to output exclusively `instafel-v${VERSION}-arm64-v8a-unclone.apk` and `instafel-v${VERSION}-arm64-v8a-clone.apk`, eliminating duplicate `instafel-v${VERSION}-arm64-v8a.apk` file.
  - `README.md`: Updated deliverables specification table to reflect the two clean variants.
  - GitHub Release `v451.0.0.0.70`: Pruned duplicate asset `instafel-v451.0.0.0.70-arm64-v8a.apk` and aligned release notes.
- **Status**: 100% (Completed)
## [2026-10-09 16:56:00 IST] - Privacy Protection: Scrubbing Email Exposure from Documentation
- **Action**: Completely scrubbed explicit email addresses from `README.md` mirror specifications to preserve privacy and prevent address harvesting.
- **Components Modified**:
  - `README.md`: Replaced raw emails in cryptographic signing section with privacy-preserving handle references (`@junksidetm` for GitHub, `@mrdarksidetm` for GitLab & Codeberg).
- **Status**: 100% (Completed)
## [2026-10-09 17:07:00 IST] - Local Core Enforcement & Upstream Fallback Bypass Resolution
- **Action**: Resolved issue where CI pipeline bypassed locally built patcher core and fell back to downloading outdated upstream core binary, which prevented custom typography and emoji settings from appearing in the Instagram app.
- **Components Modified**:
  - `patcher/src/main/kotlin/instafel/patcher/handlers/CoreHandler.kt`: Enhanced `checkDebugCoreJAR()` with dynamic resolution supporting `IFL_CORE_JAR` environment variable, `.output` directory scanning, and working directory discovery.
  - `patcher/src/main/kotlin/instafel/patcher/commands/CreateIflSourceZip.kt`: Synchronously joined background worker thread (`thread.join()`) during source extraction.
  - `.github/workflows/generate_instafel.yml`: Rebuilt `:patcher-core:build-jar` immediately after `updatePatcherSources` so embedded `ifl_sources` are packaged into the core JAR, staged the local core to working directory and `~/.local/share/ipatcher/core_data/core.jar`, and exported `IFL_CORE_JAR` across all build steps.
- **Status**: 100% (Completed)
## [2026-10-09 18:27:00 IST] - Direct Asset Loading, Global Reflection Font Override, Emoji Transformation Spans & Local Asset Bundling
- **Action**: Resolved issue where toggling custom fonts and emojis in Instafel settings produced no visible changes in Instagram. Implemented direct `AssetManager` font loaders, reflection-based `Typeface.sSystemFontMap` override, dynamic `EmojiTransformationMethod` spans, decor view layout listeners, and pre-bundled local typography assets.
- **Root Cause Analysis**:
  - **Asset Location Disconnect:** Assets packaged into APK's `assets/` directory were queried exclusively via `context.getFilesDir() + "/fonts"` and `context.getFilesDir() + "/emojis"`. Because Android does not automatically unpack APK assets into internal storage, `fontFile.exists()` returned `false` 100% of the time.
  - **Emoji Application Void:** `EmojiManager.java` had zero view-binding logic or text transformations in the app; it was only queried once by the settings preview.
  - **Dynamic View Traversal:** `FontManager.applyToActivity` ran a single one-shot traversal on `Activity` resume. Instagram's dynamic feeds, reels, and stories (Litho / RecyclerView) bypass static decor view traversals.
- **Components Modified**:
  - `app/src/main/java/instafel/app/managers/FontManager.java`:
    - Implemented dual-source loading: attempts direct loading from `context.getAssets()` first, with fallback to `context.getFilesDir()`.
    - Added background asset extraction unpacking bundled assets to `files/fonts/`.
    - Implemented reflection-based system font override modifying `Typeface.sSystemFontMap` and static fields (`DEFAULT`, `DEFAULT_BOLD`, `SANS_SERIF`, `SERIF`) mapping `"sans-serif"`, `"roboto"`, `"Instagram Sans"`, and custom fallbacks.
    - Added throttled `ViewTreeObserver.OnGlobalLayoutListener` to decor view to continuously apply typography to dynamic and recycled views.
  - `app/src/main/java/instafel/app/managers/EmojiManager.java`:
    - Implemented direct asset loading and background extraction for `iOS_26.4.ttf` and `GoogleEmoji3D.ttf`.
    - Implemented `EmojiTransformationMethod` and `EmojiTypefaceSpan` using Unicode emoji regex (detecting pictographs, skin tone modifiers, variation selectors, flags, and ZWJ combinations) to apply custom emoji typefaces exclusively to emoji spans.
    - Added `applyToTextView(TextView)` with `EditText` watcher.
  - `app/src/main/java/instafel/app/activity/ifl_a_typography.java`:
    - Invalidate caches (`FontManager.clearCache()`, `EmojiManager.clearCache()`) and reapply system overrides immediately upon switch/dialog selection.
    - Applied `EmojiManager.applyToTextView(previewEmojis)` to live preview.
  - `assets_bundle/`:
    - Pre-bundled 30 Apple SF Pro weights (`assets_bundle/fonts/sf_pro/`), Google Sans Flex variable font (`assets_bundle/fonts/GoogleSansFlex.ttf`), and Apple iOS 26.4 emojis (`assets_bundle/emojis/iOS_26.4.ttf`) locally in repository.
  - `.github/workflows/generate_instafel.yml`:
    - Updated asset preparation step to consume pre-bundled local font/emoji assets and fetch fresh `GoogleEmoji3D.ttf` from latest release of `junksidetm/Google-Emoji-3D`.
  - `.gitignore`:
    - Ignored `assets_bundle/emojis/GoogleEmoji3D.ttf` to keep dynamically fetched release binary out of git history while tracking all bundled fonts and iOS emojis.
- **Status**: 100% (Completed)

## [2026-10-09 19:28:00 IST] - Multi-Platform Mirror CI/CD Integration
- **Action**: Added GitLab CI pipeline and Forgejo Actions workflow for autonomous engine validation and build replication on mirror platforms.
- **Files Added**:
  - `.gitlab-ci.yml`
  - `.forgejo/workflows/generate_instafel.yml`
  - `Version.md`
- **Status**: 100% (Completed & Synced)
