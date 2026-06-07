# Today

A focused, Things3-style to-do app for the **Mudita Kompakt** (e-ink Android), built **100% with
Mudita Mindful Design (MMD)** components and obeying the MMD e-ink "bible" — pure black/white,
visible controls only, no animation, scrollbars on every scroll region.

Package: `com.mosquishe.today`

## Features

- **Today** — only to-dos due today (scheduled for today, overdue carried forward, or with a
  due/overdue deadline). The screen you open into.
- **Upcoming** — future-scheduled to-dos in a scrolling list grouped by date.
- **Anytime** — active to-dos with no date.
- **Logbook** — completed to-dos with the date they were finished.
- A to-do has a **title, notes, a checklist, multiple tags, a scheduled date, an optional deadline,
  and an optional repeat** (daily / weekly / monthly).
- **Checklist auto-complete** — when every checklist item is ticked the to-do completes itself
  (toggleable in Settings).
- **Tags** — create, assign multiple per to-do, filter any view by tag.
- **"Day starts at"** setting — choose when a new day's to-dos begin appearing (default 03:00).
- **Repeating** to-dos spawn the next occurrence when completed.
- **Search** the Logbook by title.
- **Undo delete** — deleting a to-do shows an MMD snackbar that restores it (with checklist + tags).
- Monochrome adaptive **app icon** (rounded-square + checkmark, matching the in-app FAB).

## Tech stack (lean, small bundle)

- Kotlin 1.9.22, AGP 8.3.0, Jetpack Compose (UI 1.7.3, compiler 1.5.10), `com.mudita:MMD:1.0.0`.
- Room 2.6.1 (kapt) for persistence; Preferences DataStore for settings.
- Compose Navigation; **manual DI** (no Hilt) via `AppContainer`.
- `minSdk 28`, `targetSdk 35`, arm64-only. Release uses R8 + resource shrinking.

## Architecture

```
domain/      Pure Kotlin, unit-tested: logical-today, view predicates, recurrence, auto-complete.
data/local   Room entities (Task, ChecklistItem, Tag, TaskTagCrossRef), DAOs, converters.
data/settings SettingsStore (DataStore).
data/repo    TaskRepository — single gateway; view filtering + recurrence + auto-complete live here.
di/          AppContainer (manual DI) + viewModelCreator helper.
ui/          theme, nav (AppShell + NavHost), one generic list screen, detail, settings, common.
```

The view rules live once in `domain/DateLogic.kt` and are reused by the repository, which filters
`TaskDao.observeAll()` in memory (personal-scale data) so the rules stay in one tested place.

## Build & run

```bash
# Debug build + install on the emulator/device
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Unit tests (domain logic)
./gradlew test

# Small release bundle (R8 + resource shrink; currently debug-signed for testing —
# swap in a real keystore before publishing)
./gradlew :app:assembleRelease
```

## E-ink compliance

All scrollables use `LazyColumnMMD`/`LazyRowMMD` (built-in scrollbar). Completion is a tap on
`CheckboxMMD`; delete is an overflow menu — no swipe gestures. No animations (nav transitions are
`None`). Colors come only from `eInkColorScheme`. The only non-MMD UI primitives are `Icon`/
`IconButton` and layout containers, as the MMD guidelines permit.

## License

[Apache-2.0](LICENSE) © 2026 Yoran van Driel.

The Mudita MMD component library (`com.mudita:MMD`) is a **separate, proprietary** dependency owned
by Mudita and is **not** covered by this license (see [NOTICE](NOTICE)).
