# HeatCall – kehittäjän dokumentaatio

**Suomi** · [English](en/DEVELOPMENT.md)

## Ympäristö

| Työkalu | Versio |
|---|---|
| Android Studio | Rabbit 2 (2026.2.2) tai uudempi |
| JDK | Android Studion mukana tuleva JBR: macOS `/Applications/Android Studio.app/Contents/jbr/Contents/Home`, Windows `C:\Program Files\Android\Android Studio\jbr` |
| Gradle | 9.6.0 (wrapper) |
| Android Gradle Plugin | 9.5.1 (sisäänrakennettu Kotlin) |
| Kotlin / Compose-kääntäjä | 2.4.21 |
| Compose BOM | 2026.09.00 (Material 3 1.4) |
| SDK | compileSdk 37, targetSdk 36, minSdk 29 |

Versiot ovat tiedostossa [`gradle/libs.versions.toml`](../gradle/libs.versions.toml). `core-ktx` 1.19 vaatii compileSdk 37:n.

## Buildaus

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"   # macOS
# export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"                  # Windows (Git Bash)

./gradlew testDebugUnitTest     # yksikkötestit
./gradlew assembleDebug         # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease       # app/build/outputs/apk/release/app-release.apk
./gradlew bundlePlay            # app/build/outputs/bundle/play/app-play.aab (Play, R8, upload-avain)
```

`local.properties` (`sdk.dir=…`) syntyy Android Studiossa automaattisesti, eikä sitä viedä versionhallintaan.

### Allekirjoitus

Buildeja on kaksi, ja ne allekirjoitetaan eri avaimilla (`app/build.gradle.kts`):

| Build | Käyttö | Avain |
|---|---|---|
| `release` (`assembleRelease`) | Sivuladattava APK, GitHub-julkaisut | **Debug-avain** (`~/.android/debug.keystore`). Tarkoituksellinen: päivitykset asentuvat aiemmin jaettujen versioiden päälle. |
| `play` (`bundlePlay`) | Google Play (AAB) | **Upload-avain** (`signingConfigs.upload`), R8-minifiointi ja resurssien karsinta |

Upload-avaimen tiedot luetaan projektin juuren `keystore.properties`-tiedostosta (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). Tiedosto ja avaimet (`*.jks`, `*.keystore`) ovat `.gitignore`ssa, eikä niitä viedä repoon. Jos `keystore.properties` puuttuu, `play`-build jää allekirjoittamatta. Avaimen luonti ja varmuuskopiointi: [play/JULKAISU.md](play/JULKAISU.md#1-upload-avain).

Sivuladattu versio ja Play-versio ovat eri allekirjoituksella, joten käyttäjän pitää poistaa sivuladattu versio ennen Play-version asentamista. Jos debug-avain vaihtuu (esim. uusi buildikone), GitHub-APK:n päivitys ei asennu vanhan päälle, joten `debug.keystore` kannattaa varmuuskopioida.

## Rakenne

```
app/src/main/java/fi/heatcall/
├── HeatCallApp.kt              Application: repository, tila muistissa, ilmoituskanava, widgetin päivitys
├── MainActivity.kt             Compose-juuri, navigointi, luvat, lähetys, vastausikkuna
├── data/
│   └── DeviceRepository.kt     DataStore: laitteet (JSON), valittu laite, asetukset, viestiloki
├── i18n/
│   └── I18n.kt                 Kielivalinta ja tr("suomi", "englanti")
├── platform/
│   └── DeviceVendor.kt         Valmistajan tunnistus ja taustatoiminnan asetusnäkymät
├── sms/
│   ├── Commands.kt             SMS-komentojen muodostus ja validointi
│   ├── ReplyParser.kt          Vastausten tulkinta sovelluksen kielellä
│   ├── DeviceClock.kt          Laitteen kello: asetus puhelimen ajasta, poikkeama, kesä-/talviaikamuistutus
│   ├── SmsSender.kt            Lähetys SmsManagerilla + SentReceiver (lähetyksen tila)
│   ├── SmsReceiver.kt          SMS_RECEIVED: suodatus numeron mukaan, loki, ilmoitus, vastausikkuna
│   ├── MarkReadListener.kt     NotificationListenerService: "Merkitse luetuksi" -toiminto
│   ├── MarkReadDiagnostics.kt  Luetuksi merkinnän vianmääritysloki
│   └── ReplyBus.kt             Sovelluksen sisäinen tila (lähetys, näytettävä vastaus)
├── widget/
│   └── StatusWidget.kt         Kotinäytön widget (RemoteViews)
└── ui/
    ├── components/             Chrome (tausta ja painikkeet), WheelPicker (rullavalitsin),
    │                           SubScreen (alinäkymän runko), LanguagePicker (kielivalinta)
    ├── screens/                MainScreen, DeviceEditScreen, TimersScreen, InfoScreens, ReplyDialog,
    │                           ClockDialog (laitteen kello), PermissionGuide (lupien tila ja
    │                           rajoitettujen asetusten ohje), BackgroundGuide (taustatoiminta merkeittäin)
    └── theme/                  Tumma Material 3 -teema

app/src/test/java/fi/heatcall/
├── sms/ProtocolTest.kt, DeviceClockTest.kt, EnglishTest.kt
└── platform/VendorTest.kt
```

## Keskeiset ratkaisut

### Lähetys taustalla

`SmsSender` lähettää viestin `SmsManager.sendTextMessage`illa suoraan, ilman viestisovellusta. Operaattorin lähetyskuittaus tulee `SentReceiver`ille, joka päivittää tilan (`WaitingReply` / `Failed`). Jos vastausta ei tule 3 minuutissa, `MainActivity` ja widget näyttävät aikakatkaisun.

### Vastaanotto ja luetuksi merkintä

Android sallii viestien merkitsemisen luetuksi (SMS-tietokantaan kirjoittamisen) vain **oletus-SMS-sovellukselle**. HeatCallia ei haluttu tehdä oletusviestisovellukseksi, joten:

1. `SmsReceiver` saa jokaisen saapuvan viestin (`RECEIVE_SMS`) ja käsittelee vain tallennettujen lämmittimien numeroista tulevat. Numeroita verrataan viimeisten 9 numeron perusteella, joten `+358…`, `00358…` ja `040…` tulkitaan samaksi numeroksi.
2. Viesti tallennetaan lokiin, laitteen tila päivitetään ja vastausikkuna näytetään (tai ilmoitus, jos sovellus on taustalla).
3. `MarkReadListener` (NotificationListenerService) etsii **oletusviestisovelluksen** (`Telephony.Sms.getDefaultSmsPackage`) ilmoituksen, joka vastaa viestiä (tekstin alku tai lähettäjän numero), ja laukaisee sen toiminnon `SEMANTIC_ACTION_MARK_AS_READ`. Varalla toiminto tunnistetaan otsikosta ("Merkitse luetuksi", "Mark as read" …).

Tunnistus on tehty kestämään valmistajien eroja:

- **Paketti:** oletusviestisovellus tai tunnettu viestisovellus (`KNOWN_SMS_PACKAGES`: Google, Samsung, AOSP, Oppo/ColorOS, OnePlus, Xiaomi).
- **Viesti:** juuri vastaanotettu viesti, *tai* EasyStart Call -protokollan muoto (`PROTOCOL`-regex, esim. `Heater:`, `T1:`, `ErD:` rivin alussa), *tai* otsikon numero, *tai* MessagingStyle-lähettäjän `tel:`-URI. Tunnistus toimii, vaikka numero olisi tallennettu yhteystietoihin nimellä tai prosessi olisi käynnistynyt uudelleen.
- **Toiminto:** `SEMANTIC_ACTION_MARK_AS_READ` tai otsikon perusteella (`MARK_READ_LABELS`). Vastaa-, poisto- ja estotoiminnot ohitetaan.
- **Kuuntelijan katkeaminen:** `requestRebind` katkeamisen jälkeen ja sovelluksen käynnistyessä (ColorOS katkaisee kuuntelijoita taustalla).
- **Vianmääritys:** `MarkReadDiagnostics` kirjaa jokaisen päätöksen. Loki näkyy Info ja asetukset -näkymässä ja sen voi jakaa.

Emulaattorissa (Google Messages) testattu: lämmittimen vastaus → luettu, protokollamuotoinen viesti tuntemattomasta numerosta → luettu, tavallinen viesti → jää lukemattomaksi.

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

### Valmistajakohtaiset asetukset

`platform/DeviceVendor.kt` tunnistaa valmistajan (`Build.MANUFACTURER` / `BRAND`): Samsung, Google, OPPO (myös OnePlus ja realme), Xiaomi tai muu. Jokaiselle valmistajalle on ohjeen vaiheet ja kohteet (`SettingsTarget`): sovelluksen tiedot, akku ja automaattinen käynnistys. Kullekin kohteelle kokeillaan listaa valmistajan sisäisiä näkymiä (`ComponentName`) järjestyksessä, ja varapolkuna on aina `ACTION_APPLICATION_DETAILS_SETTINGS`. `ActivityNotFoundException` ja `SecurityException` ohitetaan. Käyttöliittymä on `BackgroundGuideDialog` (Info ja asetukset → Käyttöoikeudet → Taustatoiminnan asetukset).

Valmistajien näkymät eivät ole julkista API:a. Ne on koottu valmistajien tunnetuista poluista ja voivat muuttua järjestelmäpäivityksissä. Emulaattorissa (Pixel) on testattu tunnistus ja varapolku. Samsungin, OPPOn ja Xiaomin syvälinkit pitää varmistaa oikeilla laitteilla. `VendorTest.kt` testaa tunnistuksen.

### Kielet (suomi / englanti)

Sovelluksella on oma kielivalinta, joka ei riipu puhelimen kielestä (`i18n/I18n.kt`):

- Tekstit kirjoitetaan koodiin parina: `tr("Suomeksi", "In English")`. `I18n.language` on Compose-tila, joten näkymät, ilmoitukset ja widget vaihtavat kieltä heti ilman uudelleenkäynnistystä.
- Valinta tallennetaan SharedPreferencesiin. Jos käyttäjä ei ole valinnut kieltä, käytetään puhelimen kieltä: suomi, jos puhelin on suomeksi, muuten englanti.
- Enumien nimet (`Commands.Day`, `HeaterType`, `Mode`, `DeviceAction`, `BottomItem`) ovat gettereitä (`label`), jotta kieli ei jää kiinni luontihetkeen.
- Järjestelmän näyttämät tekstit (ilmoitusten kuuntelijan nimi, widgetin kuvaus ja esikatselu) ovat resursseissa `values/` (englanti) ja `values-fi/`, ja ne seuraavat puhelimen kieltä.
- SMS-komennot ovat kielestä riippumattomia. `EnglishTest.kt` varmistaa tämän ja englanninkieliset tulkinnat.

## Testaus

### Yksikkötestit

[`ProtocolTest.kt`](../app/src/test/java/fi/heatcall/sms/ProtocolTest.kt) kattaa komentojen muodostuksen, numerovertailun ja vastausten tulkinnan. [`DeviceClockTest.kt`](../app/src/test/java/fi/heatcall/sms/DeviceClockTest.kt) kattaa kellon asetuskomennon, poikkeaman laskennan (myös viikon ympäri) ja kesä-/talviaikamuistutuksen. [`EnglishTest.kt`](../app/src/test/java/fi/heatcall/sms/EnglishTest.kt) varmistaa, että komennot eivät riipu kielestä, ja testaa englanninkieliset tulkinnat. [`VendorTest.kt`](../app/src/test/java/fi/heatcall/platform/VendorTest.kt) testaa valmistajan tunnistuksen.

### Emulaattori

Emulaattorille voi lähettää lämmittimen vastauksia simuloituina tekstiviesteinä:

```bash
ADB=~/Library/Android/sdk/platform-tools/adb                    # macOS
# ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"        # Windows (Git Bash)
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
