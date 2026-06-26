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

## Build status — Phase 16 (Medicine — schedules, doses, refills)

Medicines as a vertical over the one pipeline — and **no schema change**: a
medicine is an Item (`type=medicine`) whose dosage, daily times and stock live
in `details` (`MedicineDetails`), whose *next dose* reuses `reminderAt` (so it
notifies for free), and whose adherence reuses the **Phase 13 cadence engine** —
each dose taken writes a row in `habit_checkins`, so streaks are the same query
that powers Habits (§4: one atom, no duplication). A new "داروها" page in the
home pager lists each medicine with dosage, next-dose time, doses-remaining,
adherence streak, a one-tap "مصرف کردم", and a refill prompt when stock falls to
the threshold. Adding picks doses-per-day (۱–۴, mapped to sensible default
times). `MedicineSchedule.nextDose` (soonest remaining slot today, else tomorrow)
and the codec are covered by pure-JVM tests.

## Build status — Phase 15 (Calendar — device sync + ICS)

Calendars, both ways, reached from **Settings → تقویم**:

- **Device calendar** (`CalendarContract`, runtime READ/WRITE_CALENDAR): pick a
  writable calendar and mirror every item-with-a-reminder into it (create or
  update — the event id is stored on the Item as `calendarEventId`, DB v6, so
  re-syncing never duplicates). Upcoming device events are listed read-only and
  can be pulled into the Inbox with one tap.
- **ICS** (no permission, fully offline): export all due-dated items to a
  standard `.ics` (via SAF), and import an `.ics` back as Inbox items with
  reminders. A small dependency-free `IcsCodec` (RFC 5545: line folding, value
  escaping, UTC/floating/all-day dates) carries it, with pure-JVM unit tests.

Calendar access is **off until granted** and every call degrades to empty/null,
so the app is fully usable without it (the §12 spirit: integrations only add).
Lifecycle is handled: trashing, deleting, or clearing a reminder removes the
mirrored event and clears the link (via a `CalendarMirror` abstraction, so the
repo stays unit-testable); and ICS import **de-duplicates by UID** — our own
exports round-trip back onto the source item, and re-importing a file is a
no-op rather than a pile of copies.

## Build status — Phase 14 (Finance — expenses & installments)

Money as a vertical over the one pipeline. A finance entry is an Item
(`type=expense` for a one-off, `type=installment` for a plan); the amount lives
in the item's `details` JSON (`FinanceDetails`) while the **due date reuses
`reminderAt`** — so a finance item is searchable, openable in detail, trashable,
backed up, and reminded for free (§4: one atom, no duplication). A new
"هزینه‌ها" page in the home pager shows the total still owed, a list (remaining
amount, قسط n/total, Jalali due date, pay button) and an add sheet
(هزینه/قسطی toggle, amount, installment count/period, due presets). Paying an
installment rolls the due date forward one period (or clears it when settled).
Amounts are formatted as Toman with Persian digits.

## Build status — Phase 13 (Habits — first vertical)

Habits are Items (`type=habit`); daily completions live in a `habit_checkins`
table (DB v5) so streaks are real queries. A new "عادت‌ها" page in the home
pager lists habits with a check circle and streak; habits open in item detail
and are backed up like anything else.

## Build status — Phase 12 (hardening)

API key sealed with the Keystore (no plaintext, §11); Projects ordered by real
activity (`Project.updatedAt` bumped on triage); status/nav-bar icon contrast
follows the resolved theme; pure-JVM tests for `PersianNormalizer` and
`TagsCodec`. (Snackbar-Undo, Paging 3, and portable backup are noted as
follow-ups; deletions are already recoverable via Trash.)

## Build status — Phase 7 (signature & polish)

The Timeline signature realized: "today" is emphasized on the spine, rows
animate in. Long-press any item (Inbox / Timeline / Project) to send it to Trash.
Motion respects the system "remove animations" setting throughout.

## Build status — Phase 6 (trust & ownership)

A recoverable **Trash** for every deletion (soft-delete → restore to its prior
place → delete-forever / empty), and **encrypted export/import** via SAF
(AES-256-GCM with a Keystore key; whole-DB backup, import merges, nothing is
destroyed). Both reachable from Settings → داده‌ها.

## Build status — Phase 5 (optional AI assistant)

AI is **off by default** and reached only through a single `AIProvider`
(no-op until enabled). Provider-agnostic OpenAI-compatible client (base URL +
key + model). When enabled it only *adds suggestions* (§12): triage suggestions
in the sheet, ask-your-brain over FTS in Search, and voice transcription / image
OCR enrichment. Every call degrades to null — the app is fully usable with AI off.

## Build status — Phase 4 (capture-speed surfaces)

Capture → triage → projects, **FTS5 Persian search** (normalized index+query,
bm25 ranking, kept in sync by triggers), the **Timeline** showpiece (Jalali day
markers, pine spine), a swipeable home shell, splash screen, onboarding,
theme settings (system/light/dark), About, and double-tap-back to exit.

Capture surfaces: **share target** (text + image from any app), **photo** and
**voice** capture from the quick-add bar, and a **Glance home-screen widget**
for one-tap capture. All work with AI off.

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
