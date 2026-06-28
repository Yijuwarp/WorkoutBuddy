# Dark, energetic UI redesign

## Problem Statement

WorkoutBuddy's UI currently feels flat and generic — a single light Material 3 theme with an ad-hoc set of accent colors (gold, rose, amber, slate, youtube-red) that don't read as a cohesive visual identity. Nothing about the screens signals "workout app" specifically; the same layout could belong to a to-do list or a banking app. During an active workout, the experience should feel motivating and energetic, not like a generic forms app.

## Solution

Give the app a bold, energetic, dark-only visual identity, rolled out incrementally starting with the screen used during an active workout (`WorkoutScreen`), then propagated to the remaining screens (Log, Profile, Onboarding). The redesign covers a new dark color palette, punchier treatment of key numeric stats, and restyled/extended animations on the highest-value interaction moments (set completion, PR celebration, rest-timer countdown, workout summary).

## User Stories

1. As a user starting a workout, I want the active screen to feel energetic and motivating, so that I'm more engaged during my session.
2. As a user training in a dim room or at night, I want a dark UI, so that the screen isn't uncomfortably bright.
3. As a user completing a set, I want clear, punchy visual/motion feedback, so that I feel a sense of progress and momentum.
4. As a user who breaks a personal record, I want an energetic celebration moment, so that the achievement feels rewarding.
5. As a user resting between sets, I want the rest-timer countdown to be visually engaging, so that waiting doesn't feel like dead time.
6. As a user finishing a workout, I want an energetic, visually rewarding summary screen, so that completing the workout feels like an accomplishment.
7. As a user glancing at my stats mid-workout (timer, calories, intensity), I want the key numbers to be bold and legible at a glance, so that I can read them quickly without breaking focus.
8. As a user who values consistency, I want the new visual language to extend to the Log, Profile, and Onboarding screens after WorkoutScreen, so that the app feels cohesive rather than having one redesigned screen and several legacy ones.
9. As a user with an existing mental model of status colors (PR/gold, success/green, danger/red, warning/amber), I want those semantic colors preserved (just restyled for dark backgrounds), so that I don't have to relearn what colors mean.
10. As a user, I want gold (PR) and amber (warning) to be visually distinguishable from each other on the new palette, so that I don't confuse a celebration with a warning.
11. As a user, I want existing interactions (button press feedback, etc.) to continue working as before, so that the redesign doesn't regress existing polish.

## Implementation Decisions

- **Theme scope:** Dark-only. No light/dark toggle and no system-following behavior in this pass. The existing light theme (`WorkoutBuddyTheme` / `LightColorScheme`) is replaced, not made conditional.
- **Rollout order:** Incremental, screen by screen. Order: `WorkoutScreen` first, then `LogScreen`, `ProfileScreen`, `OnboardingScreen`. Shared components (`UIComponents`, `EquipmentPicker`, `WorkoutIntensityDial`, `WavyFloatingNumbers`) get updated as they're encountered by each screen's pass rather than in one separate sweep.
- **Color palette (neutrals):**
  - Background: `#0B0E14`
  - Surface (cards): `#161B26`
  - Surface elevated (modals/active state): `#1E2533`
  - Border/divider: `#2A3142`
  - Text primary: `#F1F5F9`
  - Text muted: `#8A94A6`
- **Color palette (accents):**
  - Primary (brightened from existing `#2563EB`): `#3B82F6`
  - Secondary/glow accent: `#60A5FA`
  - PR gold (changed from `#D97706` to avoid collision with warning amber): `#FBBF24`
  - Success green (brightened from `#16A34A`): `#22C55E`
  - Danger red: unchanged, `#EF4444`
  - Warning amber: unchanged, `#F59E0B`
- **Typography:** No new font, no change to `Typography`/`Type.kt` type scale. Key numeric displays (workout timer, calorie count, intensity %) get increased weight/size at the call site rather than via a global type-scale change.
- **Motion:** No new animation infrastructure. Restyle/extend the existing primitives (`WavyFloatingNumbersContainer`, `pressScale`, `recordBrokenCelebration` state in `WorkoutViewModel`/`WorkoutScreen`) for the new palette and for the four target flows, in priority order:
  1. Set completion
  2. PR celebration
  3. Rest-timer countdown
  4. Workout summary
- **Out-of-scope component shapes/spacing decisions** (corner radius, elevation values, spacing scale) are left to be resolved during implementation per-screen, not pre-specified here.

## Testing Decisions

- This is a purely visual/UI redesign with no behavioral/business-logic changes — color values, type styles, and animation timing are not meaningful targets for automated tests.
- Existing test coverage (`WorkoutCalculationsTest`, `MainScreenTest`) should continue to pass unmodified; the redesign must not change any computed values (intensity, calories, durations) or `WorkoutViewModel` state semantics, only how they're rendered.
- Verification of this PRD is primarily visual: build the app and review each restyled screen and animation manually (per repo convention of running the app to confirm UI changes, since type-checking/build success doesn't verify visual correctness).
- If `MainScreenTest` or other Compose UI tests assert on specific colors or text styles tied to the old light theme, those assertions need to be updated to match the new dark theme rather than relaxed or removed.

## Out of Scope

- Light mode / theme toggle support.
- Custom/display fonts.
- New animation systems or libraries beyond what already exists in the codebase.
- Redesigning Log, Profile, and Onboarding screens in this initial pass (tracked as the next incremental steps, not part of this PRD's delivery).
- Changing any workout-tracking business logic, data model, or calculations.

## Further Notes

- This PRD originated from a `/grilling`-style design conversation, not a written brief — decisions above reflect explicit user confirmations made during that conversation, not assumptions.
- Future PRDs should cover the Log/Profile/Onboarding passes once the WorkoutScreen redesign is validated, since palette/motion choices here are intended to be reused, not re-derived.
