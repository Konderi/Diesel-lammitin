# HeatCall – kehittäjän dokumentaatio

## Ympäristö

| Työkalu | Versio |
|---|---|
| Android Studio | Rabbit 2 (2026.2.2) tai uudempi |
| JDK | Android Studion mukana tuleva JBR (`/Applications/Android Studio.app/Contents/jbr/Contents/Home`) |
| Gradle | 9.6.0 (wrapper) |
| Android Gradle Plugin | 9.5.1 (sisäänrakennettu Kotlin) |
| Kotlin / Compose-kääntäjä | 2.4.21 |
| Compose BOM | 2026.09.00 (Material 3 1.4) |
| SDK | compileSdk 37, targetSdk 36, minSdk 29 |

Versiot ovat tiedostossa [`gradle/libs.versions.toml`](../gradle/libs.versions.toml). `core-ktx` 1.19 vaatii compileSdk 37:n.

## Buildaus

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew testDebugUnitTest     # yksikkötestit
./gradlew assembleDebug         # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease       # app/build/outputs/apk/release/app-release.apk
```

`local.properties` (`sdk.dir=…`) syntyy Android Studiossa automaattisesti, eikä sitä viedä versionhallintaan.

### Allekirjoitus

Release-build allekirjoitetaan tällä hetkellä **debug-avaimella** (`app/build.gradle.kts` → `signingConfig = signingConfigs.getByName("debug")`), jotta APK on suoraan asennettavissa omaan käyttöön.

Oma release-avain otetaan käyttöön näin:

```bash
keytool -genkeypair -v -keystore ~/heatcall-release.jks -alias heatcall \
  -keyalg RSA -keysize 4096 -validity 10000
```

Luo sen jälkeen `signingConfigs { create("release") { … } }` ja lue salasanat esimerkiksi `keystore.properties`-tiedostosta, jota **ei** viedä repoon (`*.jks` ja `*.keystore` ovat jo `.gitignore`ssa). Huomaa, että avaimen vaihto estää päivitysasennuksen vanhan version päälle.

## Rakenne

```
app/src/main/java/fi/heatcall/
├── HeatCallApp.kt              Application: repository, tila muistissa, ilmoituskanava, widgetin päivitys
├── MainActivity.kt             Compose-juuri, navigointi, luvat, lähetys, vastausikkuna
├── data/
│   └── DeviceRepository.kt     DataStore: laitteet (JSON), valittu laite, asetukset, viestiloki
├── sms/
│   ├── Commands.kt             SMS-komentojen muodostus ja validointi
│   ├── ReplyParser.kt          Vastausten tulkinta suomeksi
│   ├── SmsSender.kt            Lähetys SmsManagerilla + SentReceiver (lähetyksen tila)
│   ├── SmsReceiver.kt          SMS_RECEIVED: suodatus numeron mukaan, loki, ilmoitus, vastausikkuna
│   ├── MarkReadListener.kt     NotificationListenerService: "Merkitse luetuksi" -toiminto
│   └── ReplyBus.kt             Sovelluksen sisäinen tila (lähetys, näytettävä vastaus)
├── widget/
│   └── StatusWidget.kt         Kotinäytön widget (RemoteViews)
└── ui/
    ├── components/             Tausta, kromipainike, rullavalitsin, alinäkymän runko
    ├── screens/                Päänäkymä, laitteen muokkaus, ajastimet, info, ohjeet, vastausikkuna,
    │                           PermissionGuide (lupien tila ja rajoitettujen asetusten ohje)
    └── theme/                  Tumma Material 3 -teema
```

## Keskeiset ratkaisut

### Lähetys taustalla

`SmsSender` lähettää viestin `SmsManager.sendTextMessage`illa suoraan, ilman viestisovellusta. Operaattorin lähetyskuittaus tulee `SentReceiver`ille, joka päivittää tilan (`WaitingReply` / `Failed`). Jos vastausta ei tule 3 minuutissa, `MainActivity` ja widget näyttävät aikakatkaisun.

### Vastaanotto ja luetuksi merkintä

Android sallii viestien merkitsemisen luetuksi (SMS-tietokantaan kirjoittamisen) vain **oletus-SMS-sovellukselle**. HeatCallia ei haluttu tehdä oletusviestisovellukseksi, joten:

1. `SmsReceiver` saa jokaisen saapuvan viestin (`RECEIVE_SMS`) ja käsittelee vain tallennettujen lämmittimien numeroista tulevat. Numeroita verrataan viimeisten 9 numeron perusteella, joten `+358…`, `00358…` ja `040…` tulkitaan samaksi numeroksi.
2. Viesti tallennetaan lokiin, laitteen tila päivitetään ja vastausikkuna näytetään (tai ilmoitus, jos sovellus on taustalla).
3. `MarkReadListener` (NotificationListenerService) etsii **oletusviestisovelluksen** (`Telephony.Sms.getDefaultSmsPackage`) ilmoituksen, joka vastaa viestiä (tekstin alku tai lähettäjän numero), ja laukaisee sen toiminnon `SEMANTIC_ACTION_MARK_AS_READ`. Varalla toiminto tunnistetaan otsikosta ("Merkitse luetuksi", "Mark as read" …).

Tämä toimii Google Messagesilla ja Samsung Messagesilla, jotka tarjoavat Merkitse luetuksi -toiminnon ilmoituksessaan. Sovellusta voi vaihtaa ilman muutoksia, koska oletusviestisovellus luetaan aina järjestelmältä.

### Tila ja tallennus

`DeviceRepository` tallentaa kaiken DataStore Preferencesiin. Laitteet ovat JSON-taulukkona, loki on rajattu 150 viimeisimpään viestiin, ja PIN-koodi peitetään lokissa. `HeatCallApp.cachedState` pitää viimeisimmän tilan muistissa ilmoituskuuntelijaa varten. `SmsReceiver` lukee tilan levyltä, koska prosessi voi käynnistyä juuri viestiä varten.

### Widget

`StatusWidget` on perinteinen `AppWidgetProvider` + `RemoteViews`, joten Glance-riippuvuutta ei tarvita. Tarkista tila -painike lähettää broadcastin, joka lähettää `heater?`-kyselyn ja merkitsee odotuksen alkaneeksi (`SharedPreferences`). `HeatCallApp` piirtää widgetin uudelleen aina, kun valitun auton tila muuttuu. `AlarmManager` herättää widgetin aikakatkaisun jälkeen.

### Luvat ja rajoitetut asetukset

Tiedostosta asennettu sovellus saa Android 13+:ssa tekstiviesti- ja ilmoituskuuntelijaluvat vasta, kun käyttäjä sallii rajoitetut asetukset sovellustiedoista. `PermissionGuide.kt`:

- `readPermissionStatus` / `rememberPermissionStatus`: lupien tila. Tila päivittyy aina, kun käyttäjä palaa järjestelmän asetuksista (`repeatOnLifecycle(RESUMED)`).
- `MainActivity.permissionLauncher` avaa `PermissionHelpDialog`in automaattisesti, jos tekstiviestilupa evätään.
- `openAppDetails` avaa `ACTION_APPLICATION_DETAILS_SETTINGS`-sivun, jossa ovat ⋮ → *Salli rajoitetut asetukset*, luvat ja akku.

Emulaattorissa estetyn luvan voi simuloida:

```bash
for p in SEND_SMS RECEIVE_SMS; do
  $ADB shell pm revoke fi.heatcall android.permission.$p
  $ADB shell pm set-permission-flags fi.heatcall android.permission.$p user-fixed
done
```

## Testaus

### Yksikkötestit

[`ProtocolTest.kt`](../app/src/test/java/fi/heatcall/sms/ProtocolTest.kt) kattaa komentojen muodostuksen, numerovertailun ja vastausten tulkinnan.

### Emulaattori

Emulaattorille voi lähettää lämmittimen vastauksia simuloituina tekstiviesteinä:

```bash
ADB=~/Library/Android/sdk/platform-tools/adb
$ADB install -r app/build/outputs/apk/debug/app-debug.apk
$ADB emu sms send +358401234567 "Heater:on,mode:heater,run:30"
$ADB emu sms send +358401234567 "ErD:HeaterCom:lost,Heater:off"
```

Ilmoitusten käyttöoikeuden voi antaa ilman käyttöliittymää:

```bash
$ADB shell cmd notification allow_listener fi.heatcall/fi.heatcall.sms.MarkReadListener
```

Luetuksi merkinnän voi tarkistaa (Google Messagesin ensimmäinen käynnistys pitää olla tehty):

```bash
$ADB shell content query --uri content://sms/inbox --projection read:body
$ADB logcat -s HeatCallMarkRead
```

## Tunnetut rajoitukset

- Widget näyttää vain sovelluksessa valittuna olevan auton.
- PIN-koodin vaihto laitteeseen (`config:set,pin`) on toteutettu `Commands`-oliossa, mutta käyttöliittymässä sitä ei vielä ole.
- Luetuksi merkintä riippuu siitä, että viestisovelluksen ilmoituksessa on Merkitse luetuksi -toiminto.
- "Päivitetty X min sitten" päivittyy vain, kun näkymä piirretään uudelleen.
