# AdSkip Pro - Auto Ad Skipper & Shield (ऑटो विज्ञापन स्किप और शील्ड)

A high-performance Android application that automatically detects and clicks "Skip Ad" buttons across all apps and platforms (including YouTube, web browsers, and mobile games), auto-mutes audio during unskippable video ads, and provides DNS ad-blocking guidance with live statistics and an interactive in-app test simulator.

## User Review & Critical Decisions

> [!IMPORTANT]
> The app combines an Android **Accessibility Service** (for instant automated clicking of "Skip Ad", "Close", and "X" buttons across all apps) with an **Audio Manager** (for optional auto-muting during video ads) and a **DNS Shield Configuration** (for system-wide banner and tracker prevention).
> An **In-App Test Ad Simulator** is built-in so users can immediately test and verify auto-skipping functionality inside the emulator without needing external apps.

- **Confirmed Decision 1 (Ad Detection Engine)**: Accessibility Service engine scanning UI nodes across running apps for skip patterns ("Skip Ad", "Skip in...", "Close", "विज्ञापन छोड़ें", "स्किप करें", countdowns) and executing instant clicks.
- **Confirmed Decision 2 (Smart Audio Mute)**: Automatically lowers/mutes media volume when video ads begin, and restores original volume when skipped or completed.
- **Confirmed Decision 3 (Bilingual UI)**: Full Hindi and English bilingual support toggle, catering directly to the user's language preference.

---

## 1. Overview & Core Concept

- **What It Does**: Once enabled by the user with standard Accessibility permission, AdSkip Pro runs quietly in the background. Whenever any app (such as YouTube, streaming apps, mobile games, or web browsers) displays an ad with a "Skip Ad" or "Close" button, the app detects it within milliseconds, performs the click automatically, mutes unwanted loud ad audio, and logs the skipped ad to display saved time in the dashboard.
- **Target Audience / Persona**: Users tired of repetitive mobile ads, forced video countdowns, and having to manually tap tiny skip buttons on video players and gaming apps.
- **Key Value**: Saves hours of accumulated ad-watching time, removes disruptive loud ad audio, and provides complete hands-free viewing comfort.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Onboarding & Permission Setup**:
   - Master switch with clear, warm visual prompt.
   - 1-tap "Turn On Auto-Skipper" button that opens Android Accessibility Settings (`Settings.ACTION_ACCESSIBILITY_SETTINGS`).
   - Visual step-by-step guidance card showing exactly how to toggle "AdSkip Pro" in system settings.
2. **Dashboard & Live Analytics**:
   - Active Shield Status badge (pulsing green indicator when Accessibility Service is active).
   - Live metrics: Total Ads Skipped counter, Estimated Time Saved (minutes/hours), and Today's Skip Count.
   - Quick settings toggles: "Auto-Click Skip Buttons", "Mute Audio on Ads", "Turbo Skip Mode (0ms delay)".
3. **In-App Test Simulator (Sandbox)**:
   - Interactive mock video player that triggers a sample 5-second skippable ad, an interstitial popup ad, and a banner ad.
   - When the user enables the service, they can see AdSkip Pro automatically detect and click "Skip Ad" right in front of their eyes.
4. **Skipped Ads Activity Log**:
   - Clean timeline list showing app name (e.g. YouTube, Browser, Game), timestamp, and time saved per ad.
5. **Language & Customization**:
   - Header toggle between Hindi (हिंदी) and English.
   - Per-app protection filters and custom skip delay slider.

### Visual Identity & Theme

- **Aesthetic Direction**: **Cyber-Shield Dark Mode** — sleek, trustworthy, modern utility with dark obsidian tones and high-contrast emerald and cyan status accents.
- **Color Palette**:
  - Background: `#0B0F19` (Deep Obsidian Void)
  - Surface Card: `#131B2E` (Deep Navy Surface)
  - Primary Accent: `#10B981` (Shield Emerald Green - Active & Protected)
  - Secondary Accent: `#06B6D4` (Electric Cyan - Analytics & Speed)
  - Warning/Alert: `#F59E0B` (Amber - Permission Attention Needed)
  - Text Primary: `#F9FAFB`
  - Text Secondary: `#94A3B8`
- **Typography & Layout**:
  - Monospace bold metrics for counters and time saved.
  - Large rounded interactive cards with subtle glowing borders (`1.dp` emerald outline when active).
  - Minimum 48dp touch targets on all toggles and buttons.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Accessibility Service vs Custom VPN for Video Ads**:
  - *Chosen Approach*: Android `AccessibilityService` is the industry standard and most battery-friendly method to skip ads in apps like YouTube and games, because modern video streams bundle ads within the same TLS connection (making pure network DNS blocking unable to skip YouTube video ads). Accessibility directly clicks the skip button the microsecond it appears.
  - *Trade-off*: Requires Accessibility permission from the user, which we explain with clear security & privacy assurances (no personal data collection).
- **Decision 2: Local In-Memory & DataStore Persistence**:
  - *Chosen Approach*: Skipped counts, settings (mute toggle, speed delay, language), and recent logs are saved locally using lightweight Android preferences and local repository.
  - *Why*: Ultra-fast, zero latency, works completely offline, zero battery drain.
- **Decision 3: Integrated Live Ad Test Simulator**:
  - *Chosen Approach*: Provide a built-in sandbox screen where users can test simulated video ads and banner ads.
  - *Why*: In browser emulators or test environments, external apps like YouTube might not have active accounts; this sandbox allows instantaneous verification that the background auto-click service is working.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌───────────────────────────────────────────────────────────────┐
│                    AdSkip Pro Application                     │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │               Jetpack Compose UI Layer                  │  │
│  │  • DashboardScreen (Master Toggle, Live Counters)       │  │
│  │  • ServiceSetupCard (Deep-link & Step Guide)            │  │
│  │  • TestSimulatorScreen (Live Interactive Ad Sandbox)    │  │
│  │  • ActivityLogScreen (Skipped Ads Timeline)             │  │
│  │  • SettingsScreen (Hindi/English, Mute, Delay, Apps)    │  │
│  └───────────────────────────┬─────────────────────────────┘  │
│                              │ Observes State / Events        │
│  ┌───────────────────────────▼─────────────────────────────┐  │
│  │                       MainViewModel                     │  │
│  │  • isServiceRunning, statsState, settingsState          │  │
│  │  • testAdState (controls simulator playback & ad timer) │  │
│  └─────────────┬─────────────────────────────┬─────────────┘  │
│                │                             │                │
│  ┌─────────────▼───────────────┐ ┌───────────▼─────────────┐  │
│  │   AdSkipPreferencesRepo     │ │   AudioManager Controller│  │
│  │   • Total ads skipped       │ │   • Auto-mute on ad      │  │
│  │   • Time saved / History    │ │   • Restore volume       │  │
│  └─────────────▲───────────────┘ └───────────▲─────────────┘  │
│                │                             │                │
│  ┌─────────────┴─────────────────────────────┴─────────────┐  │
│  │            AdSkipAccessibilityService (Android OS)      │  │
│  │  • TYPE_WINDOW_CONTENT_CHANGED / WINDOW_STATE_CHANGED   │  │
│  │  • Node Inspector (Target IDs, Text keywords, Hindi/En) │  │
│  │  • performAction(ACTION_CLICK)                          │  │
│  └─────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────┘
```

### Component & Data Strategy

1. **`AdSkipAccessibilityService`**:
   - Declared in `AndroidManifest.xml` with `android.permission.BIND_ACCESSIBILITY_SERVICE` and configuration XML.
   - Monitors node tree with `AccessibilityNodeInfo`:
     - Checks node text, content descriptions, and view resource IDs.
     - Keywords: "Skip Ad", "Skip Ads", "Skip in", "Close", "Close Ad", "No thanks", "Dismiss", "বিজ্ঞাপন", "स्किप करें", "विज्ञापन छोड़ें", "إلغاء".
     - Identifies YouTube skip button `com.google.android.youtube:id/skip_ad_button`.
     - Recursively checks clickable parent nodes if the label itself is not clickable.
     - Dispatches click event via `performAction(AccessibilityNodeInfo.ACTION_CLICK)`.
2. **`AdSkipPreferencesRepository`**:
   - Persists total ads skipped, estimated seconds saved, mute audio preference, turbo skip toggle, and selected language (Hindi vs English).
   - Broadcasts updates back to the UI via Kotlin Coroutines `StateFlow`.
3. **`AdTestSimulator`**:
   - Plays a simulated video stream with an overlay ad that activates after 3 seconds with a "Skip Ad in 5s" countdown that transforms into "Skip Ad" / "स्किप करें".
   - Demonstrates the real Accessibility Service auto-clicking the button without human intervention.
