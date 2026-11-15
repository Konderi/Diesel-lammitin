# HeatCall – developer documentation

[Suomi](../KEHITYS.md) · **English**

## Environment

| Tool | Version |
|---|---|
| Android Studio | Rabbit 2 (2026.2.2) or newer |
| JDK | The JBR bundled with Android Studio: macOS `/Applications/Android Studio.app/Contents/jbr/Contents/Home`, Windows `C:\Program Files\Android\Android Studio\jbr` |
| Gradle | 9.6.0 (wrapper) |
| Android Gradle Plugin | 9.5.1 (built-in Kotlin) |
| Kotlin / Compose compiler | 2.4.21 |
| Compose BOM | 2026.09.00 (Material 3 1.4) |
| SDK | compileSdk 37, targetSdk 36, minSdk 29 |

The versions are in [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml). `core-ktx` 1.19 requires compileSdk 37.

## Building

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"   # macOS
# export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"                  # Windows (Git Bash)

./gradlew testDebugUnitTest     # unit tests
./gradlew assembleDebug         # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease       # app/build/outputs/apk/release/app-release.apk
./gradlew bundlePlay            # app/build/outputs/bundle/play/app-play.aab (Play, R8, upload key)
```

`local.properties` (`sdk.dir=…`) is created automatically by Android Studio and is not committed to version control.

### Signing

There are two builds, signed with different keys (`app/build.gradle.kts`):

| Build | Use | Key |
|---|---|---|
| `release` (`assembleRelease`) | Sideloaded APK, GitHub releases | **Debug key** (`~/.android/debug.keystore`). Intentional: updates install over previously shared versions. |
| `play` (`bundlePlay`) | Google Play (AAB) | **Upload key** (`signingConfigs.upload`), R8 minification and resource shrinking |

The upload key details are read from `keystore.properties` in the project root (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). The file and the keys (`*.jks`, `*.keystore`) are in `.gitignore` and never committed. If `keystore.properties` is missing, the `play` build is left unsigned. Creating and backing up the key: [play/RELEASE-STEPS.md](play/RELEASE-STEPS.md#1-upload-key).

The sideloaded and Play versions have different signatures, so users must uninstall the sideloaded version before installing from Play. If the debug key changes (e.g. a new build machine), a GitHub APK update will not install over the old version, so back up `debug.keystore`.

## Structure

```
app/src/main/java/fi/heatcall/
├── HeatCallApp.kt              Application: repository, in-memory state, notification channel, widget updates
├── MainActivity.kt             Compose root, navigation, permissions, sending, reply dialog
├── data/
│   └── DeviceRepository.kt     DataStore: devices (JSON), selected device, settings, message log
├── i18n/
│   └── I18n.kt                 Language choice and tr("Finnish", "English")
├── platform/
│   └── DeviceVendor.kt         Manufacturer detection and background activity settings screens
├── sms/
│   ├── Commands.kt             Building and validating SMS commands
│   ├── ReplyParser.kt          Interpreting replies in the app language
│   ├── DeviceClock.kt          Device clock: setting from phone time, offset, DST reminder
│   ├── SmsSender.kt            Sending with SmsManager + SentReceiver (send status)
│   ├── SmsReceiver.kt          SMS_RECEIVED: filtering by number, log, notification, reply dialog
│   ├── MarkReadListener.kt     NotificationListenerService: "Mark as read" action
│   ├── MarkReadDiagnostics.kt  Mark-as-read troubleshooting log
│   └── ReplyBus.kt             In-app state (sending, reply to show)
├── widget/
│   └── StatusWidget.kt         Home screen widget (RemoteViews)
└── ui/
    ├── components/             Chrome (background and buttons), WheelPicker, SubScreen (sub-screen
    │                           scaffold), LanguagePicker
    ├── screens/                MainScreen, DeviceEditScreen, TimersScreen, InfoScreens, ReplyDialog,
    │                           ClockDialog (device clock), PermissionGuide (permission status and
    │                           restricted settings guide), BackgroundGuide (background activity by brand)
    └── theme/                  Dark Material 3 theme

app/src/test/java/fi/heatcall/
├── sms/ProtocolTest.kt, DeviceClockTest.kt, EnglishTest.kt
└── platform/VendorTest.kt
```

## Key design decisions

### Sending in the background

`SmsSender` sends the message directly with `SmsManager.sendTextMessage`, without a messaging app. The carrier's send confirmation goes to `SentReceiver`, which updates the state (`WaitingReply` / `Failed`). If no reply arrives within 3 minutes, `MainActivity` and the widget show a timeout.

### Receiving and marking as read

Android only allows marking messages as read (writing to the SMS database) for the **default SMS app**. We did not want to make HeatCall the default messaging app, so:

1. `SmsReceiver` receives every incoming message (`RECEIVE_SMS`) and only handles those from the numbers of saved heaters. Numbers are compared by their last 9 digits, so `+358…`, `00358…` and `040…` are treated as the same number.
2. The message is saved to the log, the device state is updated and the reply dialog is shown (or a notification, if the app is in the background).
3. `MarkReadListener` (NotificationListenerService) looks for the **default messaging app's** (`Telephony.Sms.getDefaultSmsPackage`) notification that matches the message (start of the text or sender number) and triggers its `SEMANTIC_ACTION_MARK_AS_READ` action. As a fallback, the action is identified by its title ("Merkitse luetuksi", "Mark as read" …).

The matching is designed to cope with differences between manufacturers:

- **Package:** the default messaging app or a known messaging app (`KNOWN_SMS_PACKAGES`: Google, Samsung, AOSP, Oppo/ColorOS, OnePlus, Xiaomi).
- **Message:** the message just received, *or* the EasyStart Call protocol format (`PROTOCOL` regex, e.g. `Heater:`, `T1:`, `ErD:` at the start of a line), *or* the number in the title, *or* the MessagingStyle sender's `tel:` URI. Matching works even if the number is saved in contacts under a name or the process has restarted.
- **Action:** `SEMANTIC_ACTION_MARK_AS_READ` or by title (`MARK_READ_LABELS`). Reply, delete and block actions are skipped.
- **Listener disconnects:** `requestRebind` after a disconnect and when the app starts (ColorOS disconnects listeners in the background).
- **Diagnostics:** `MarkReadDiagnostics` logs every decision. The log is shown in the Info and settings screen and can be shared.

Tested on the emulator (Google Messages): heater reply → read, protocol-format message from an unknown number → read, ordinary message → stays unread.

### State and storage

`DeviceRepository` stores everything in DataStore Preferences. Devices are stored as a JSON array, the log is limited to the 150 most recent messages, and the PIN code is masked in the log. `HeatCallApp.cachedState` keeps the latest state in memory for the notification listener. `SmsReceiver` reads the state from disk, because the process may be started just for the message.

### Widget

`StatusWidget` is a classic `AppWidgetProvider` + `RemoteViews`, so no Glance dependency is needed. The Check status button sends a broadcast that sends the `heater?` query and marks the wait as started (`SharedPreferences`). `HeatCallApp` redraws the widget whenever the selected car's state changes. `AlarmManager` wakes the widget after the timeout.

### Permissions and restricted settings

On Android 13+, an app installed from a file only gets the SMS and notification listener permissions after the user allows restricted settings in the app info. `PermissionGuide.kt`:

- `readPermissionStatus` / `rememberPermissionStatus`: permission status. The status refreshes whenever the user returns from the system settings (`repeatOnLifecycle(RESUMED)`).
- `MainActivity.permissionLauncher` opens `PermissionHelpDialog` automatically if the SMS permission is denied.
- `openAppDetails` opens the `ACTION_APPLICATION_DETAILS_SETTINGS` page, which has ⋮ → *Allow restricted settings*, permissions and battery.

On the emulator you can simulate a blocked permission:

```bash
for p in SEND_SMS RECEIVE_SMS; do
  $ADB shell pm revoke fi.heatcall android.permission.$p
  $ADB shell pm set-permission-flags fi.heatcall android.permission.$p user-fixed
done
```

### Manufacturer-specific settings

`platform/DeviceVendor.kt` detects the manufacturer (`Build.MANUFACTURER` / `BRAND`): Samsung, Google, OPPO (also OnePlus and realme), Xiaomi or other. Each manufacturer has guide steps and targets (`SettingsTarget`): app info, battery and auto-start. For each target, a list of the manufacturer's internal screens (`ComponentName`) is tried in order, and the fallback is always `ACTION_APPLICATION_DETAILS_SETTINGS`. `ActivityNotFoundException` and `SecurityException` are ignored. The UI is `BackgroundGuideDialog` (Info and settings → Permissions → Background activity settings).

The manufacturers' screens are not a public API. They have been collected from the manufacturers' known paths and may change with system updates. Detection and the fallback have been tested on the emulator (Pixel). The Samsung, OPPO and Xiaomi deep links still need to be verified on real devices. `VendorTest.kt` tests the detection.

### Languages (Finnish / English)

The app has its own language setting that does not depend on the phone language (`i18n/I18n.kt`):

- Texts are written in code as pairs: `tr("Suomeksi", "In English")`. `I18n.language` is Compose state, so screens, notifications and the widget switch language immediately without a restart.
- The choice is stored in SharedPreferences. If the user has not chosen a language, the phone language is used: Finnish if the phone is in Finnish, otherwise English.
- Enum names (`Commands.Day`, `HeaterType`, `Mode`, `DeviceAction`, `BottomItem`) are getters (`label`), so the language is not fixed at creation time.
- Texts shown by the system (the notification listener name, the widget description and preview) are in resources `values/` (English) and `values-fi/`, and they follow the phone language.
- SMS commands are language-independent. `EnglishTest.kt` verifies this and the English interpretations.

## Testing

### Unit tests

[`ProtocolTest.kt`](../../app/src/test/java/fi/heatcall/sms/ProtocolTest.kt) covers building commands, number comparison and interpreting replies. [`DeviceClockTest.kt`](../../app/src/test/java/fi/heatcall/sms/DeviceClockTest.kt) covers the clock set command, offset calculation (including wrapping around the week) and the DST reminder. [`EnglishTest.kt`](../../app/src/test/java/fi/heatcall/sms/EnglishTest.kt) checks that commands do not depend on the language and tests the English interpretations. [`VendorTest.kt`](../../app/src/test/java/fi/heatcall/platform/VendorTest.kt) tests manufacturer detection.

### Emulator

You can send heater replies to the emulator as simulated text messages:

```bash
ADB=~/Library/Android/sdk/platform-tools/adb                    # macOS
# ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"        # Windows (Git Bash)
$ADB install -r app/build/outputs/apk/debug/app-debug.apk
$ADB emu sms send +358401234567 "Heater:on,mode:heater,run:30"
$ADB emu sms send +358401234567 "ErD:HeaterCom:lost,Heater:off"
```

Notification access can be granted without the UI:

```bash
$ADB shell cmd notification allow_listener fi.heatcall/fi.heatcall.sms.MarkReadListener
```

You can check that messages were marked as read (Google Messages must have been launched once first):

```bash
$ADB shell content query --uri content://sms/inbox --projection read:body
$ADB logcat -s HeatCallMarkRead
```

## Known limitations

- The widget only shows the car currently selected in the app.
- Changing the device PIN (`config:set,pin`) is implemented in the `Commands` object, but it is not yet available in the UI.
- Marking as read depends on the messaging app's notification having a Mark as read action.
- "Updated X min ago" only refreshes when the screen is redrawn.
