# Publishing on Google Play

[Suomi](../PLAY-JULKAISU.md) · **English**

This memo lists the requirements for the Play release and their status. The release is in progress: the developer account is verified, and testers are being recruited for the closed test. The practical steps are in [play/RELEASE-STEPS.md](play/RELEASE-STEPS.md). The sideloaded APK from GitHub (see [INSTALLATION.md](INSTALLATION.md)) keeps working alongside the Play version.

## Conclusion

The technical preparation is done. The deciding question is **Google's policy on SMS permissions**: `SEND_SMS` and `RECEIVE_SMS` are allowed only for the default messaging app or for an approved exception, and Google reviews exceptions manually. If the exception is not granted, the app would have to be changed so that it does not send or receive messages in the background.

## 1. Basic requirements

| Item | Status / action |
|---|---|
| Play Console developer account and identity verification | Done |
| Price | Free and open source, no payments profile (see [play/RELEASE-STEPS.md](play/RELEASE-STEPS.md#pricing-free)) |
| Closed testing: new personal accounts need at least 12 testers for 14 days | Required: the account is personal and was created in 11/2026. See [TESTAAJAT.md](../play/TESTAAJAT.md) (Finnish + English). |
| **Android App Bundle (AAB)** Play accepts only AAB, not APK. | Done: `./gradlew bundlePlay` → `app-play.aab` |
| **Upload key** + Play App Signing | Done: the `play` build is signed with the upload key (see [DEVELOPMENT.md](DEVELOPMENT.md#signing)) |
| `applicationId` | `fi.heatcall`. The ID cannot be changed after publishing. |
| targetSdk | 36 meets the current requirements |
| Privacy policy (public URL) | Published: https://konderi.github.io/privacy/heatcall/ |
| Data safety form | Text messages and the phone number are processed only on the device and are not sent anywhere |
| Content rating, store description, 512 px icon, 1024×500 px feature graphic, screenshots | Done: icon, feature graphics and screenshots (en, fi) in `docs/play/assets/`, texts in [KAUPPA-TEKSTIT.md](../play/KAUPPA-TEKSTIT.md) |
| Trademarks | "Compatible with Eberspächer EasyStart Call" is allowed. The Eberspächer logo must not be used, and the app must not look like the manufacturer's official app. |

## 2. SMS permissions: Permissions Declaration Form

Items on the exception list that HeatCall could fit:

| Exception | Allowed SMS permissions | Suitability |
|---|---|---|
| **Connected device companion apps** | READ/RECEIVE/SEND/WRITE_SMS etc. | Best basis: EasyStart Call is a device installed in the car and controlled with the phone |
| Device automation | READ/RECEIVE/SEND/WRITE_SMS etc. | Weak: refers to user-defined automations across different parts of the operating system |
| In-vehicle hands-free | RECEIVE/SEND_SMS etc. | Does not fit: intended for use while driving, e.g. navigation |

The form needs:

- a description of the core function: remote control over the SMS protocol, which is the only remote connection the device supports
- a justification of why there is no alternative: the device has no internet connection, only GSM and SMS
- a video showing a command being sent and the reply being received

The declaration is submitted with the first release that uses SMS permissions (see [play/RELEASE-STEPS.md](play/RELEASE-STEPS.md#3-play-console), step 5). Prepared answers: [play/FORMS.md](play/FORMS.md).

## 3. Fallback plan if the exemption is not granted

| Function | Current | Without SMS permissions |
|---|---|---|
| Sending a command | `SmsManager`, in the background | **SMS intent**: the messaging app opens with the text prefilled, and the user taps Send |
| Receiving the reply | `SMS_RECEIVED` | **SMS User Consent API**: the user approves reading one message. The API is designed for verification codes, so whether it suits heater replies has to be tested. |
| Widget's Check status | In the background | Not possible without user action |
| Marking as read | NotificationListenerService | Allowed, as long as its use is clearly explained before the permission is requested |

## 4. Other permissions

- **Notification access** (`BIND_NOTIFICATION_LISTENER_SERVICE`): allowed, but the app must clearly state what the permission is used for. The explanation in the Info and settings screen is a good basis.
- **POST_NOTIFICATIONS**, widget and AlarmManager: no restrictions.

## 5. Code changes

- [x] Upload key: `signingConfigs.upload`, details from `keystore.properties`, which is not under version control
- [x] `play` build: `bundlePlay`, R8 minification (`isMinifyEnabled = true`), resource shrinking and ProGuard rules
- [x] An explanation screen before requesting notification access (prominent disclosure): *Grant access* first opens a dialog that explains what the permission is used for and what it is not used for. Android's settings open only from *Continue to settings*.
- [x] A link to the privacy policy in the app: Info and settings → About → *Privacy policy* (plus a *Source code* link)

Source: [Google Play – Use of SMS or Call Log permission groups](https://support.google.com/googleplay/android-developer/answer/10208820)
