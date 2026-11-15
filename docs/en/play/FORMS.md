# Play Console forms: prepared answers

[Suomi](../../play/LOMAKKEET.md) · **English**

## 1. Data safety

**App content → Data safety**

| Question | Answer |
|---|---|
| Does the app collect or share user data? | **No.** The app has no internet permission, and no data is sent to the developer or to third parties. |
| Is data encrypted in transit? | Not applicable (no data is transferred). |
| Can users request that data be deleted? | Data is stored only on the device. It is deleted in the app or by uninstalling the app. |

Justification under Google's definition: data processed on the device that is not sent off the device is not "collected". A text message that the user sends to their own heater is the user's own action, not data collection by the developer.

## 2. SMS permissions declaration (Permissions Declaration Form)

**App content → Sensitive app permissions → SMS and Call Log permissions**

- **Core functionality:** *Connected device companion app*
- **Permissions:** `SEND_SMS`, `RECEIVE_SMS`

**Description (in English, for the form):**

> HeatCall is a companion app for the Eberspächer EasyStart Call, a GSM remote control unit installed in a vehicle to operate its fuel-operated parking heater. The unit has no internet connectivity; its only remote interface is SMS. The app's core function is to send control commands to the unit (e.g. "pin:1234 heater:on,run:30") and to receive and display the unit's status replies (e.g. "Heater:on,mode:heater,run:30").
>
> SEND_SMS is needed to send commands to the user-configured unit number in the background when the user presses Start, Stop or Check status (also from the home screen widget). RECEIVE_SMS is needed to receive the unit's replies and show them in the app. Only messages from the user's configured unit numbers are processed; all other messages are ignored. The app has no internet permission, and no SMS data leaves the device.
>
> Alternatives do not work: the SMS Retriever and User Consent APIs are designed for one-time verification codes, and the unit's status replies do not fit that format. An SMS intent would require the user to send every command manually, which breaks the core remote-control function and the home screen widget.

**Video (usually required, 30–90 s, publish on YouTube as "Unlisted"):**

1. Open HeatCall and show the car card and the heater number (Info and settings → Devices).
2. Press **Check status**. Show that the text message is sent in the background.
3. The heater's reply arrives and opens in its own window ("Heater on").
4. Press **START** and show the reply.
5. Show Check status from the home screen widget.
6. Show that a message from another number does not appear in the app.

## 3. Other forms

| Form | Answer |
|---|---|
| App access | All functions are available without signing in. Note for the reviewer: testing the functions requires an EasyStart Call unit. Without one, the command is sent but no reply arrives. |
| Ads | No ads |
| Content rating | Tools / utility. No violence, sexuality, gambling or user-to-user communication → everyone (PEGI 3) |
| Target audience | 18+ (vehicle owner) |
| News app | No |
| Health, financial, VPN | No |
| Government app | No |
| Privacy policy | https://konderi.github.io/HeatCall/privacy/ |

## 4. Notification access (NotificationListenerService)

Play has no separate form for this, but the use must be clearly disclosed. This has been done in the app: *Info and settings → Reply messages → Grant access* first opens an explanation dialog. It states that the permission is used only to mark heater replies as read, that notifications from other apps are ignored and message contents are not stored, and that the permission can be revoked. Android's settings open only from *Continue to settings*. The same is stated in the privacy policy and in the store description.
