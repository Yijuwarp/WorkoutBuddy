# Workout Buddy — Play Store Submission Checklist

## ✅ Already done

- [x] **Package name** finalized: `com.venkatchait.workoutbuddy` (permanent, can't change after first publish)
- [x] **Unused INTERNET permission removed** (app has no network usage — YouTube links open externally)
- [x] **Release keystore generated**: `keystore/workoutbuddy-release.jks`, credentials in `keystore/keystore.properties` (both gitignored)
- [x] **Signed release AAB built**: `app/build/outputs/bundle/release/app-release.aab`
- [x] **512×512 hi-res icon**: `play_store_assets/icon_512.png`
- [x] **1024×500 feature graphic**: `play_store_assets/feature_graphic_1024x500.png`
- [x] **Store listing copy drafted**: `play_store_assets/store_listing.md`
- [x] **Privacy policy drafted and hosted**: live at https://yijuwarp.github.io/WorkoutBuddy/privacy-policy.html

## ⚠️ Critical — back up the keystore NOW

`keystore/workoutbuddy-release.jks` and `keystore/keystore.properties` are **gitignored on purpose** (never commit signing credentials), which also means **nothing else is backing them up**. If you lose this file, you can never publish an update to this app again under the same listing — you'd have to launch as a brand new app and lose all reviews/installs/history.

**Copy `keystore/workoutbuddy-release.jks` and `keystore/keystore.properties` to at least one other secure location right now** — a password manager, encrypted cloud storage, or a USB drive in a drawer. Do this before you forget.

## 🔲 What only you can do (Play Console)

1. **Host the privacy policy.** Fill in your name/email in `privacy_policy.md`, publish it somewhere public (GitHub Pages, Google Sites, Notion public page), and get the URL.

2. **Create the app in Play Console** (play.google.com/console):
   - App name: Workout Buddy
   - Default language, app/games type: App, Free
   - Package name will be set automatically from the AAB you upload

3. **App content section** (Play Console → Policy → App content):
   - Privacy policy URL (from step 1)
   - Ads: No ads
   - Content rating questionnaire — answer honestly; this is a fitness tracker with no violence/mature content, so it should land in the **Everyone** rating. The questionnaire will ask about things like violence, sexual content, gambling, user-generated content (none of these apply) — just answer "No" throughout.
   - Target audience: 13+ is reasonable for a fitness app (avoids extra requirements for child-directed apps); adjust if you want a different audience
   - Data safety form: see below
   - Government apps, financial features, health apps declarations: this isn't a medical app — it doesn't diagnose or treat anything, so you can typically answer "No" to the health-specific compliance questions, but read Google's current health-app policy if asked, since policies change.

4. **Data safety form** (Play Console → Policy → App content → Data safety):
   - Since the app collects **zero data** (no network permission, no analytics, no ads SDK, everything stored locally only): select **"No data collected"** when prompted, or if forced to itemize, every category should be marked as not collected/not shared.

5. **Store listing** (Play Console → Grow → Store presence → Main store listing):
   - Paste short description + full description from `store_listing.md`
   - Upload `icon_512.png` as the app icon
   - Upload `feature_graphic_1024x500.png` as the feature graphic
   - Upload **phone screenshots** — see below, you'll need at least 2 (Play requires JPEG/PNG, 16:9 or 9:16, between 320px and 3840px on each side)

6. **Upload the AAB** (Play Console → Release → Production, or Internal testing first if you want to test before going live):
   - Upload `app/build/outputs/bundle/release/app-release.aab`
   - Add release notes (e.g. "Initial release")

7. **Countries/pricing**: choose which countries to release in, confirm Free

8. **Review and submit** for review. First-time app reviews typically take a few hours to a few days.

## 🔲 Screenshots — still needed

I wasn't able to capture these because the device wasn't connected when I got to this step. Easiest options:
- **You take them**: open the app on your phone, screenshot 4-8 key screens (onboarding, active workout, exercise detail, profile/rank badge, log/calendar) using your phone's normal screenshot gesture, then AirDrop/transfer them to your PC.
- **Or reconnect wireless ADB** and ask me to grab them — I'll need Settings → Developer options → Wireless debugging turned on again, same as before.

Play Store requires **at least 2 screenshots**, recommends 4-8, sized between 320px–3840px per side at a 16:9 or 9:16 ratio (a normal phone screenshot will satisfy this).

## Note on app signing in Play Console

Google Play uses "Play App Signing" by default for new apps — when you upload your AAB, Google re-signs it with their own key for distribution, using your upload key (the one we just generated) only to verify it's really you uploading. This is automatic and you don't need to configure anything extra; just don't lose the upload keystore regardless.
