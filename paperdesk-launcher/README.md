# Paperdesk Launcher

Paperdesk Launcher is a focused Android home screen by Human Centric Works. It provides a clean, distraction-light workspace and gives priority to the Madhvanidhi, Architect Calculator, and Pomodoro apps while still allowing access to every installed launchable application.

## Current capabilities

- Registers as an Android HOME launcher
- Can request the default Home role from inside the app
- Adaptive portrait and landscape layouts
- Live clock and date
- Clean text-row application list without decorative trailing characters
- Paperdesk apps pinned at the top in a consistent order
- Dedicated shortcuts for Madhvanidhi, Architect Calculator, and Pomodoro
- Home returns to the top of the app list; Back remains inside the launcher
- No network permission, analytics, advertising, or accounts

## Build

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Make it the launcher

Install the APK, open **Paperdesk**, tap **Make Paperdesk default**, and select Paperdesk as the Home app. Keep another launcher installed until Paperdesk has been tested on the target device.
