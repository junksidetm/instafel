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
