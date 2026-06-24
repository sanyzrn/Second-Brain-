# مغز دوم — Second Brain

A *Personal Life Operating System* for Android. One place to **capture**,
**connect**, and **retrieve** everything important in a life — so the mind is
freed from storage and left for thinking.

> کمتر به یاد آور، بیشتر زندگی کن. — *Remember less, live more.*

This is a native Android app built fresh against a fixed product constitution,
a strict engineering spine, and a deliberately non-generic, RTL-native,
Persian-first design.

---

## Design direction (chosen by the art director)

**Pine Editorial** — warm-paper light primary, with the **Timeline** as the one
signature element. Accent is **Deep Pine `#1F6F5C`**, spent only on the Timeline
spine and the single primary action. Persian voice is **Yekan Bakh**; data is set
in **Space Mono** (the "instrument" voice). RTL is the host layout, not a mirror.

## Engineering spine

- **Kotlin + Jetpack Compose**, min SDK 24, target/compile SDK 35.
- **Identity:** `ir.dbsgraphic.secondbrain`, project `SecondBrain`.
- **Modular** (MVI/MVVM, **Hilt** DI):
  - `:app` — RTL shell, entry points.
  - `:core:designsystem` — tokens, type (Yekan Bakh / Space Mono), primitives.
  - `:core:security` — Keystore-sealed SQLCipher key (never hardcoded).
  - `:core:database` — **Room over SQLCipher**, the single source of truth.
- **Offline-first**, no Firebase / no third-party cloud.
- Custom design system on Compose Foundation; Material used only as a substrate.

## Build status — Phase 3 (Resurface) + polish

Capture → triage → projects, **FTS5 Persian search** (normalized index+query,
bm25 ranking, kept in sync by triggers), the **Timeline** showpiece (Jalali day
markers, pine spine), a swipeable home shell, splash screen, onboarding,
theme settings (system/light/dark), About, and double-tap-back to exit.

> **Replace the DBS logo:** `feature/settings/src/main/res/drawable/dbs_logo.xml`
> is a placeholder (the real `Dbs_logo_single.webp` couldn't be fetched in the
> sandbox). Drop the real file in as `dbs_logo.webp` at that path and delete the
> `.xml` placeholder.

> **FTS5:** search relies on SQLCipher being built with FTS5 (the modern
> `net.zetetic:sqlcipher-android` builds enable it).

> **Note on building in the Claude Code web environment:** this session's egress
> policy blocks `dl.google.com`, which hosts the Android SDK *and* the Google
> Maven repository (AGP, AndroidX, Compose, Room, Hilt). A Gradle build cannot
> resolve Android dependencies here. Build and run locally (Android Studio, or
> `./gradlew :app:assembleDebug`) or in an environment whose network policy
> allows Google's hosts. The project is otherwise complete and self-contained;
> fonts are bundled in `core/designsystem/src/main/res/font`.

## Build locally

```bash
./gradlew :app:assembleDebug      # build the debug APK
./gradlew :app:installDebug       # install on a connected device/emulator
```
