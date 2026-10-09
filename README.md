<h1 align="center">Instafel 🚀</h1>

<p align="center">
  <b>Advanced Instagram Alpha Customization Suite & Typography Engine</b><br/>
  <i>Seamlessly injecting multi-weight Apple San Francisco / Google Sans Flex typography, iOS 26.4 / Google 3D emojis, AMOLED styling, and developer tools into Instagram Alpha releases.</i>
</p>

<p align="center">
  <a href="https://github.com/junksidetm/instafel/releases"><img src="https://img.shields.io/github/v/release/junksidetm/instafel?style=flat-square&color=blue&label=Latest%20Release" alt="Latest Release"></a>
  <a href="https://github.com/junksidetm/Alpha-Insta"><img src="https://img.shields.io/badge/Upstream%20Ingest-Alpha--Insta-orange?style=flat-square" alt="Alpha-Insta"></a>
  <a href="https://gitlab.com/mrdarksidetm/instafel"><img src="https://img.shields.io/badge/Mirror-GitLab-fc6d26?style=flat-square&logo=gitlab" alt="GitLab Mirror"></a>
  <a href="https://codeberg.org/mrdarksidetm/instafel"><img src="https://img.shields.io/badge/Mirror-Codeberg-2185d0?style=flat-square&logo=codeberg" alt="Codeberg Mirror"></a>
  <img src="https://img.shields.io/badge/Architecture-arm64--v8a-success?style=flat-square" alt="Architecture">
</p>

---

## 🌟 Highlights & New Features

### 1. 🔤 Hierarchical Typography Engine
Instafel dynamically replaces standard fonts across all UI elements with calibrated font weight hierarchies:
- **Apple San Francisco (`SF Pro`)**:
  - **Display Bold & Black**: Headlines, profile handles, and screen titles.
  - **Text Regular & Medium**: Body copy, comments, captions, and DM threads.
  - **Italic**: Calibrated slant for secondary captions and timestamps.
- **Google Sans Flex (Variable Font)**:
  - **Variable Axes Integration**:
    - Optical Sizing (`opsz`): `ON`
    - Width (`wdth`): `97.5`
    - Roundness (`ROND`): `73`
    - Contextual Weights: Dynamic `300` to `800` mapping
    - Italic Slant (`slnt`): `-10`

### 2. 🎨 Custom Emoji Suites
- **Apple iOS Emojis (iOS 26.4)**: Render authentic Apple emojis across direct messages, comments, reels, and stories.
- **Google 3D Emojis**: Crisp, modern fluent/3D vector emoji glyphs sourced directly from [junksidetm/Google-Emoji-3D](https://github.com/junksidetm/Google-Emoji-3D).

### 3. ⚙️ Dedicated Instafel Settings Menu
Access all mod settings directly inside the app under **Instafel Settings → Typography & Emojis**:
- Real-time switch for Custom Font Hierarchy
- Font Family Picker (Apple SF Pro vs. Google Sans Flex vs. Default)
- Live Typography Hierarchy Preview card
- Real-time switch for Custom Emojis
- Emoji Pack Picker (Apple iOS 26.4 vs. Google 3D vs. System Default)
- Live Emoji Glyph Preview card

### 4. 🛡️ Core Patch Suite
- **Developer Options**: Unlocks internal Instagram experiment flags and developer tools.
- **Ad Removal**: Filters sponsored posts, story advertisements, and suggested clips.
- **AMOLED Pure Black**: Forces dark surfaces to deep `#000000` for OLED battery savings.
- **Snooze Warning Bypass**: Silences timeout and snooze alerts.
- **Clone Variant**: Standalone package identifier enabling side-by-side installation with official Instagram.
- **Channel Customization**: Personalized build and channel naming.

---

## 🏗️ System Architecture

Instafel employs a decoupled, 100% autonomous online build pipeline:

```mermaid
flowchart LR
    A["APKMirror"] -->|"Daily Crawl (03:00 UTC)"| B["Alpha-Insta<br/>(junksidetm/Alpha-Insta)"]
    B -->|"Merge Splits into Clean APK"| C["Alpha-Insta Release<br/>(v451.x arm64-v8a)"]
    C -->|"Scheduled Trigger & Mod Gate"| D["Instafel Pipeline<br/>(junksidetm/instafel)"]
    E["Assets<br/>(SF Pro / GSF / iOS / Google 3D)"] --> D
    D -->|"Decompile, Inject & Reassemble"| F["ifl-patcher Engine"]
    F -->|"Production APK Signing"| G["GitHub Releases<br/>(Unclone + Clone APKs)"]
```

1. **Upstream Ingest ([Alpha-Insta](https://github.com/junksidetm/Alpha-Insta))**:
   - Monitored daily at `03:00 UTC` for new Instagram Alpha releases.
   - Bypasses Cloudflare anti-bot verification via authentic `APKUpdater-v0` token headers.
   - Extracts APKM bundles and merges multi-APK splits into a clean standalone base APK using `APKEditor`.
2. **Downstream Modding ([Instafel](https://github.com/junksidetm/instafel))**:
   - Gated check: Checks `Alpha-Insta` releases against already modded versions. If no new Alpha was published that day, the workflow terminates within 5 seconds to conserve resources.
   - Downloads the pre-merged standalone APK directly from `Alpha-Insta`.
   - Injects custom fonts and emoji smali hooks.
   - Rebuilds and production-signs both **Unclone** and **Clone** APK variants.
   - Publishes official releases directly on GitHub Releases.

---

## 📦 Production Deliverables

Every release on [GitHub Releases](https://github.com/junksidetm/instafel/releases) provides:

| Deliverable | Package Target | Description |
|---|---|---|
| `instafel-v<VERSION>-arm64-v8a-unclone.apk` | `com.instagram.android` | Unclone variant. Directly replaces official Instagram. |
| `instafel-v<VERSION>-arm64-v8a-clone.apk` | `com.instafel.android` | Standalone clone variant. Installs alongside official Instagram. |
| `build_info.json` | Metadata | Cryptographic MD5 hashes, commit IDs, and generation timestamps. |

---

## 🌐 Multi-Platform Mirror Ecosystem

Following our decentralized multi-platform policy, Instafel maintains active functional parity across all official hosts:

- **GitHub (Primary)**: [`https://github.com/junksidetm/instafel`](https://github.com/junksidetm/instafel)
- **GitLab (Mirror)**: [`https://gitlab.com/mrdarksidetm/instafel`](https://gitlab.com/mrdarksidetm/instafel)
- **Codeberg (Mirror)**: [`https://codeberg.org/mrdarksidetm/instafel`](https://codeberg.org/mrdarksidetm/instafel)

Every commit across all three mirrors is cryptographically signed with verified SSH keys:
- GitHub: `junksidetm` (`331540275+junksidetm@users.noreply.github.com`)
- GitLab / Codeberg: `mrdarksidetm` (`ajukr99901@gmail.com`)

---

## ⚠️ Disclaimer

Instafel is **not affiliated with, maintained by, or endorsed by Meta or Instagram**. This project is open-source and intended solely for personal educational and research purposes. Commercial distribution or unauthorized resale is strictly prohibited.

---

<p align="center">
  <i>Forked and extended with ❤️ by <a href="https://github.com/junksidetm">junksidetm</a></i><br/>
  <i>Original base created by <a href="https://github.com/mamiiblt">mamiiblt</a></i>
</p>
