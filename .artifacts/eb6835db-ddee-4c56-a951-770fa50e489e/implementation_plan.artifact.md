# Implementation Plan - Top Right Action Buttons (Sales & Finance, Notifications, Settings)

## User Review Required

> [!IMPORTANT]
> The user requested 3 action icons at the top right of the screen before any screen title:
> 1. **Ventes et Finance** (Sales & Finance) -> Navigates to Health & Finance screen.
> 2. **Notifications** (Notifications / Tâches) -> Navigates to Tasks & Notifications screen.
> 3. **Paramètres** (Settings) -> Opens a new Settings Dialog/Fragment with language toggle, app info, and options.

## Proposed Changes

### UI Components & Layouts
#### [NEW] [SettingsDialogFragment.kt](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/java/com/example/cunipro/ui/settings/SettingsDialogFragment.kt)
- Dialog or BottomSheet for App Settings (Language switch FR/EN, App info, Version, Logout).

#### [NEW] [dialog_settings.xml](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/res/layout/dialog_settings.xml)
- Layout for the Settings dialog.

#### [NEW] [include_top_actions.xml](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/res/layout/include_top_actions.xml)
- Reusable top right action container containing 3 styled buttons/cards:
  1. Finance / Ventes (`btnTopFinance`)
  2. Notifications (`btnTopNotifications`)
  3. Settings (`btnTopSettings`)

#### [MODIFY] Fragment Layouts
- Integrate `include_top_actions.xml` or top right action buttons into the headers of:
  - `fragment_dashboard.xml`
  - `fragment_animals.xml`
  - `fragment_reproduction.xml`
  - `fragment_feed.xml`
  - `fragment_health_finance.xml`
  - `fragment_tasks.xml`

#### [MODIFY] Fragment Kotlin Files
- Wire up the click listeners in each fragment to navigate/open:
  - Finance -> `healthFinanceFragment`
  - Notifications -> `tasksFragment`
  - Settings -> `SettingsDialogFragment`

## Verification Plan

### Automated Tests
- Build project using Gradle build (`app:assembleDebug`) to ensure no compilation errors.

### Manual Verification
- Deploy app to emulator/device, verify that all 3 action buttons appear at the top right of every screen before screen titles, and test clicking each one (Sales/Finance, Notifications, Settings).
