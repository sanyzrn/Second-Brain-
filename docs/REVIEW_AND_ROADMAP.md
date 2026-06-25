# Second Brain — Full Review & Forward Roadmap

A from-scratch audit of the codebase (Phases 0–7) plus a re-phased execution plan
for everything deferred. Severity: 🔴 bug · 🟠 gap · 🟡 polish · 🟢 improvement.

> Standing constraint: the project is built and reviewed but **not compiled in
> the web sandbox** (Google Maven blocked). Findings are from source review and
> the JVM unit tests. The riskiest unverified code is the Glance widget and the
> OpenAI client (both degrade safely to no-op).

---

## 1. Architecture & build
- 🟢 Clean modular graph (13 modules): `:app`, `core:{designsystem,data,database,security,ai}`, `feature:{inbox,project,timeline,search,settings,onboarding}`. MVI/MVVM + Hilt, `Flow`-driven. Solid.
- 🟡 `typeLabelFa` is duplicated in 3 files (timeline/search/project). Extract to `core:designsystem` util.
- 🟢 No CI yet. Add a GitHub Actions workflow (assembleDebug + unit tests) for an env that allows Google Maven, so regressions are caught automatically.
- 🟢 Launcher icon is a plain vector drawable, not an adaptive icon (no `mipmap-anydpi-v26`). Fine on 24+, but an adaptive icon is more correct on modern launchers.

## 2. Data layer (Room / SQLCipher / FTS / backup)
- 🟢 FTS5 fresh-install bug already fixed via the DB callback; normalization shared on index+query.
- 🔴 `Project.updatedAt` is never bumped when items are triaged into/out of a project, but the Projects list is `ORDER BY updatedAt DESC`. Result: ordering is effectively creation-time, not activity-time. Bump it on triage/edit.
- 🟡 `FtsSchema.backfillMissing` runs an `INSERT … WHERE id NOT IN (SELECT …)` on **every** app open. Harmless now; at scale do it once (guard with a flag, or only when `count(items_fts) < count(items)`).
- 🟠 No Room **migration tests** and no FTS round-trip tests. These are exactly the fragile parts. `PersianNormalizer.normalize` is pure JVM and should have unit tests now; migrations need instrumented tests.
- 🟢 Backup import is a merge (upsert) — good (never destroys). Consider a "replace vs merge" choice and a manifest/version check with a friendly error on mismatch.

## 3. Security
- 🔴 **AI API key is stored in plaintext DataStore** (`ai_api_key`). The DB is encrypted, but this personal secret is not — contradicts §11. Encrypt it with `KeystoreCipher` before persisting (store base64 of `iv||ciphertext`).
- 🟢 DB passphrase (Keystore-sealed) and backup cipher are sound.
- 🟢 Consider an optional app lock (biometric/PIN) for "privacy is part of the identity."

## 4. Repositories
- 🟢 `ItemRepository` is the single source of truth; trash/restore/links/search all centralized and unit-tested.
- 🟡 `restore()` infers prior status (`type != null → triaged else inbox`). Correct for current states, but if "archived" is ever used it won't round-trip. Consider storing prior status explicitly when trashing.
- 🟢 No pagination — `observeTimeline`/`observeInbox`/search load all rows. Fine for now; adopt Paging 3 before large datasets.

## 5. Dependency injection
- 🟢 Graph is correct: `AIProvider`→`DefaultAIProvider` (settings-driven), repositories `@Binds`, DAOs/`KeystoreCipher` provided. No god modules.

## 6. Design system
- 🔴 `SbText` has **no `maxLines`/`overflow`**. List rows (inbox/timeline/project/search) render full content — a long note breaks the editorial rhythm and hurts scroll performance. Add `maxLines`/`overflow` params; use ellipsis in lists, full text in detail.
- 🟡 Status-bar icon contrast isn't controlled. With an in-app theme override (e.g., forced Light while the system is Dark), status/nav icons can be wrong-contrast. Drive `WindowInsetsControllerCompat.isAppearanceLightStatusBars` from the resolved `dark` flag in `MainActivity`.
- 🟢 Strong, consistent primitives (SbText/Card/Chip/Switch/TextField/IconButton). Good foundation.

## 7. Capture (Inbox)
- 🟢 Fast quick-add; mic/camera swap; spatial entrance; haptics. On-spec.
- 🟠 Voice/photo capture stores a blob but the row shows only a text label ("تصویر"/"یادداشت صوتی") — **no thumbnail, no playback**.
- 🟡 Long-press-to-trash has no confirmation and only a Toast (not an **Undo**). Recoverable via Trash, but immediate Undo is the expected pattern.
- 🟡 Recording has no max-duration or level meter; navigating away discards silently.

## 8. Triage & Projects
- 🟠 **Re-triage is impossible**: the triage sheet only opens from the Inbox. Once an item is typed, you can't change its type/project/tags. Needs the item-detail screen.
- 🟠 Tags are stored and settable but there's **no tag browse/filter** anywhere.
- 🟢 Project hub tabs (derived by filtering) are correct and duplication-free.

## 9. Timeline
- 🟢 The signature reads well: Jalali day markers, pine spine, "today" emphasis, animate-in.
- 🟡 Tapping a Timeline row does nothing but shows a ripple (`combinedClickable(onClick = {})`) — misleading. Should open item detail.
- 🟢 Group headers are per-day only; month/year separators would help long histories.

## 10. Search & AI
- 🟢 FTS bareword-prefix query fixed; ask-your-brain grounds answers in FTS hits; triage suggestions are suggest-only (§12). NoOp default; degrades to null. On-spec.
- 🟠 Ask-mode retrieval is a naive prefix match on the question's words — misses paraphrases. Improve retrieval (expand terms; later: embeddings).
- 🟠 No search **filters** (type/project/date) and results aren't openable (no detail screen).
- 🟡 `suggestReminder` exists in the provider but has **no surface** (no reminders feature).
- 🟢 OpenAI client is unverified (can't compile); transcription multipart is the most likely to need a per-provider tweak.

## 11. Navigation & shell
- 🟢 Swipe shell + pushed routes + double-back are clean.
- 🟡 `combinedClickable(onClick = {})` ripple (see §9) appears on Timeline/Project rows.
- 🟢 Widget opens the app only — no inbox count, no direct capture, no preview image.

## 12. Trust & ownership
- 🟢 Recoverable Trash (restore to prior place) + encrypted SAF export/import. On-spec.
- 🟠 Backup is **device-bound** (Keystore key) — can't restore on a new device. Offer an optional password-based portable export.
- 🟢 No "auto-backup reminder" / scheduled backup. Nice-to-have.

## 13. Cross-cutting
- 🟠 **Accessibility**: long-press actions lack `onLongClickLabel`; some touch targets are small; TalkBack flow through triage/RTL unverified. Audit needed.
- 🟠 **Testing**: only repository unit tests. Add `PersianNormalizer` tests, ViewModel tests (Turbine), migration/FTS instrumented tests, and a couple of Compose UI tests.
- 🟡 **Perf**: load-all lists; multi-flow `combine` in InboxViewModel is fine but watch recomposition at scale.
- 🟢 **i18n/RTL**: strong. Consider extracting hardcoded Persian strings into `strings.xml` for future localization and consistency.
- 🟢 **Error reporting**: no logging/telemetry (correct for privacy), but an opt-in local error log would aid debugging.

---

# Re-phased execution plan (Phase 8+)

The deferred verticals (Finance, Medicine, Habits, Calendar) are deliberately
sequenced **after** the horizontal capabilities they all reuse. Every vertical is
just a typed `Item` (`type` + `details` JSON + optional `item_links`) surfaced
through the existing Inbox → triage → project → timeline → search → trash →
backup pipeline, so the verticals stay thin.

### Phase 8 — Item detail & editing  *(foundational; unblocks everything)*
Open any item (from Timeline/Project/Search/Inbox) into a detail screen: read
full content, edit text, change type/project/tags (re-triage), see metadata,
trash/restore in context. Fixes the no-op taps and the re-triage gap.

### Phase 9 — Connections UI (§5)  *(realizes the core value)*
From detail: link to another item (picker over FTS), view backlinks & outgoing,
remove links. A lightweight "connections" panel. The data layer already exists.

### Phase 10 — Rich blobs
Image thumbnails + full-screen viewer; audio playback for voice notes; show
capture source. (OCR/transcription already enrich the text.)

### Phase 11 — Reminders & notifications  *(prerequisite for several verticals)*
Surface `suggestReminder`; schedule local notifications (WorkManager/AlarmManager);
an "امروز / پیش‌رو" view. Reminders are a reusable capability for Finance, Medicine,
Calendar.

### Phase 12 — Hardening & polish
Snackbar-Undo for trash; `SbText` maxLines; encrypt the API key; status-bar
contrast; bump `Project.updatedAt`; Paging 3; accessibility pass; tests
(normalizer/VM/migration); optional password-based portable backup; CI.

### Phase 13 — Habits  *(first vertical — simplest, drives daily use, §17)*
Recurring check-ins, streaks, a daily habit strip. `type=habit`, schedule in
`details`. Reuses reminders.

### Phase 14 — Finance / installments
Costs, due dates, installment plans, totals. `type=expense|installment`. Due
dates use reminders; "connections" links a cost to its project/doc.

### Phase 15 — Calendar sync
Read/write the device calendar (or ICS import/export) for items with reminders.
Depends on Phase 11.

### Phase 16 — Medicine
Medication schedules, dosages, refills. Reuses reminders + the habit cadence
engine from Phase 13.

### Phase 17 — Goals & weekly review
Goals that link to projects/items; a weekly review surface. Ties the system
together (§18, §19).

### Deferred R&D (own design pass each)
On-device AI provider (same `AIProvider`); embedding/vector semantic search
(upgrades Phase 9/10 retrieval); end-to-end encrypted multi-device sync (the
biggest; needs a conflict model and key exchange).
