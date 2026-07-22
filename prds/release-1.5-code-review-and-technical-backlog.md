# WorkoutBuddy — Review Findings (release-1.5)

Survey of the codebase at `release-1.5` (worktree checkout, commit `4cc0b6a`). Ordered roughly by risk/impact.

## 1. Data-loss risk: destructive migration fallback
`WorkoutDatabase.kt:105` chains `.fallbackToDestructiveMigration()` alongside 5 hand-written explicit migrations (schema v15→v20, `WorkoutDatabase.kt:30-95`). Any install that jumps a version Room doesn't have an explicit `Migration` for — a bug in a future migration, or a very old install skipping several versions — **silently wipes all user data** (workout history, PRs) instead of failing loudly. `exportSchema = false` (line 21) also throws away Room's schema-history JSON, so there's no scaffolding for migration tests.
- **Fix direction**: remove the destructive fallback once migrations are trusted, add Room migration tests, turn `exportSchema` back on.

## 2. Exact-alarm permission — Play policy + incomplete onboarding
`AndroidManifest.xml` declares `USE_EXACT_ALARM` unconditionally for API 33+. This permission is policy-gated by Google Play to apps whose *core function* is alarms/clocks/timers — WorkoutBuddy is a fitness tracker, so shipping this as-is risks Play Console rejection or a mandatory policy declaration.
Separately, `OnboardingPermissionsStep` (`OnboardingScreen.kt:383-458`) requests `POST_NOTIFICATIONS` and battery-optimization exemption, but **never surfaces an exact-alarm request/explanation flow** (no `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` anywhere in the codebase). This is presumably exactly what the open `fix-exact-alarm-permission` and `onboarding-exact-alarm` branches are meant to address.
- Both scheduling call sites (`DailyReminderScheduler.kt:47-60`, `WorkoutViewModel.kt:2644-2687`) already fall back to inexact alarms gracefully and wrap scheduling in `try/catch (SecurityException)` — the fallback logic itself is fine, just duplicated between the two classes.
- **Fix direction**: decide whether exact alarms are actually necessary for a daily reminder (likely not); if not, drop `USE_EXACT_ALARM` and rely on inexact scheduling + `setAndAllowWhileIdle`. If exact timing is required, add the missing onboarding permission flow and expect a Play policy questionnaire.

## 3. Near-zero real test coverage
- Unit tests (`WorkoutCalculationsTest`, `WorkoutProgressionTest`, `WorkoutSelectionTest`) only cover pure calculation/selection logic (~400 lines total).
- The one instrumented test, `MainScreenTest.kt`, is unmodified Android Studio template boilerplate (`FAKE_DATA = listOf("Sample1"...)`, asserts `"Hello $it!"`) — it tests nothing about this app.
- **No coverage at all** for: `WorkoutViewModel.kt` (2,760 lines — timers, PR detection, workout generation, adaptive weight logic), `WorkoutRepository`/DAO, Room migrations, or notification/alarm scheduling.
- **Fix direction**: prioritize ViewModel unit tests (it's the core business logic) and a Room migration test suite given the destructive-fallback risk above; delete or replace the boilerplate instrumented test.

## 4. God classes
- `WorkoutViewModel.kt` — 2,760 lines, single ViewModel for the entire app (timers, PR detection, workout generation, calorie/step estimation, profile/onboarding, notification scheduling all in one class).
- `UIComponents.kt` — 3,042 lines, a shared grab-bag of composables.
- No DI framework (Hilt/Koin) — dependencies are wired manually via `WorkoutApplication` lazy singletons + a hand-rolled `ViewModelProvider.Factory` in `MainActivity.kt:22-30`. Workable at this size but makes the ViewModel hard to split or test in isolation.
- **Fix direction**: split ViewModel by feature area (workout session, profile/onboarding, notifications) even without introducing a DI framework; break `UIComponents.kt` into per-feature component files.

## 5. No crash reporting / silent error handling
No Crashlytics/Sentry/Bugsnag anywhere in the build. Of the 7 try/catch blocks in the app, most (`Haptics.kt:32-33,54-55`, `TimerExpiredReceiver.kt:82-83`) just call `e.printStackTrace()`, which is invisible in a release build. (Two catches — `OnboardingScreen.kt:397` and `WorkoutViewModel.kt:2680` — are deliberate, targeted, and fine.)
- **Fix direction**: add a lightweight crash reporter before the next Play release, or at minimum route these catches through `Log.e`.

## 6. No i18n
`res/values/strings.xml` contains exactly one string (`app_name`). All UI copy — screen titles, buttons, dialogs, validation messages — is hardcoded directly in Kotlin composables. The app cannot be localized without a large extraction effort.
- **Fix direction**: not urgent unless localization is on the roadmap, but worth deciding on now rather than after copy grows further.

## 7. Release build not minified
`isMinifyEnabled = false` (`build.gradle.kts:41`) despite ProGuard rule files being wired up — release APK/AAB ships unshrunk and unobfuscated.
- **Fix direction**: enable minification + shrinking before the next Play Store release, then smoke-test for ProGuard-related breakage (reflection, Room, Compose).

## 8. Stale docs
`README.md` and `docs/ARCHITECTURE.md` report DB schema v10 and versionName 1.0 — actual current state is schema v20, versionCode 5, versionName 1.5. `docs/POLISH_RECOMMENDATIONS.md` is a genuinely useful, up-to-date backlog of sound/animation polish items and can be treated as an existing "nice to have" list.
- **Fix direction**: quick pass to update version numbers in README/ARCHITECTURE next time either is touched.

## 9. Minor / inconsistent accessibility
`contentDescription` usage is inconsistent across screens (`UIComponents.kt` has 30 uses; `BodyScreen.kt` has 1; `EquipmentScreen.kt` has 2). No accessibility tests exist to catch regressions. Not urgent, but worth an audit pass.

---

### Suggested priority order
1. Fix the destructive-migration data-loss risk (#1) — highest blast radius, cheap to at least add tests for.
2. Resolve the exact-alarm permission question (#2) — blocks Play policy compliance; likely what the two open branches are already tackling.
3. Add ViewModel + migration tests (#3) before making structural changes, so refactors are covered.
4. Everything else (#4–#9) is lower urgency / can be scheduled opportunistically.
