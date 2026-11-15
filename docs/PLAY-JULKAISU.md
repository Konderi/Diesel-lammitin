# Julkaisu Google Play -kauppaan

**Suomi** · [English](en/PLAY-RELEASE.md)

Tämä muistio kokoaa Play-julkaisun vaatimukset ja niiden tilan. Julkaisu on käynnissä: kehittäjätili on vahvistettu, ja suljettuun testaukseen haetaan testaajia. Käytännön vaiheet ovat tiedostossa [play/JULKAISU.md](play/JULKAISU.md). Sivuladattava APK GitHubista (ks. [ASENNUS.md](ASENNUS.md)) toimii edelleen Play-version rinnalla.

## Päätelmä

Tekninen valmistelu on tehty. Ratkaiseva kysymys on **Googlen tekstiviestilupia koskeva käytäntö**: `SEND_SMS` ja `RECEIVE_SMS` sallitaan vain oletusviestisovellukselle tai hyväksytylle poikkeukselle, ja Google tarkistaa poikkeukset käsin. Jos poikkeusta ei myönnetä, sovellus pitäisi muuttaa niin, ettei se lähetä tai vastaanota viestejä taustalla.

## 1. Perusvaatimukset

| Asia | Tila / toimenpide |
|---|---|
| Play Console -kehittäjätili ja henkilöllisyyden varmennus | Kunnossa |
| Hinta | Ilmainen ja avointa lähdekoodia, ei maksuprofiilia (ks. [play/JULKAISU.md](play/JULKAISU.md#hinnoittelu-ilmainen)) |
| Suljettu testaus: uusilla henkilökohtaisilla tileillä vähintään 12 testaajaa 14 päivän ajan | Tarvitaan: tili on henkilökohtainen ja luotu 11/2026. Ks. [play/TESTAAJAT.md](play/TESTAAJAT.md). |
| **Android App Bundle (AAB)** Play ottaa vastaan vain AAB:n, ei APK:ta. | Valmis: `./gradlew bundlePlay` → `app-play.aab` |
| **Upload-avain** + Play App Signing | Valmis: `play`-build allekirjoitetaan upload-avaimella (ks. [KEHITYS.md](KEHITYS.md#allekirjoitus)) |
| `applicationId` | `fi.heatcall`. Tunnusta ei voi vaihtaa julkaisun jälkeen. |
| targetSdk | 36 täyttää nykyiset vaatimukset |
| Tietosuojaseloste (julkinen URL) | Julkaistu: https://konderi.github.io/privacy/heatcall/ |
| Tietoturvalomake (Data safety) | Tekstiviestit ja puhelinnumero käsitellään vain laitteella, eikä niitä lähetetä minnekään |
| Ikäluokitus, kaupan kuvaus, kuvake 512 px, mainoskuva 1024×500 px, kuvakaappaukset | Valmis: kuvake, esittelykuvat ja kuvakaappaukset (en, fi) hakemistossa `docs/play/assets/`, tekstit tiedostossa [play/KAUPPA-TEKSTIT.md](play/KAUPPA-TEKSTIT.md) |
| Tavaramerkit | "Yhteensopiva Eberspächer EasyStart Callin kanssa" on sallittu. Eberspächerin logoa ei saa käyttää, eikä sovellus saa näyttää valmistajan viralliselta sovellukselta. |

## 2. Tekstiviestiluvat: Permissions Declaration Form

Poikkeuslistan kohdat, joihin HeatCall voisi sopia:

| Poikkeus | Sallitut SMS-luvat | Soveltuvuus |
|---|---|---|
| **Connected device companion apps** | READ/RECEIVE/SEND/WRITE_SMS ym. | Paras peruste: EasyStart Call on autoon asennettu laite, jota ohjataan puhelimella |
| Device automation | READ/RECEIVE/SEND/WRITE_SMS ym. | Heikko: tarkoittaa käyttäjän määrittämiä automaatioita käyttöjärjestelmän eri osissa |
| In-vehicle hands-free | RECEIVE/SEND_SMS ym. | Ei sovi: tarkoitettu ajon aikaiseen käyttöön, esim. navigointiin |

Lomakkeeseen tarvitaan:

- ydintoiminnon kuvaus: kauko-ohjaus SMS-protokollalla, jota laite tukee ainoana etäyhteytenä
- perustelu, miksi vaihtoehtoista tapaa ei ole: laitteessa ei ole internet-yhteyttä, vain GSM ja SMS
- video, jossa näkyy komennon lähetys ja vastauksen vastaanotto

Ilmoitus lähetetään ensimmäisen SMS-lupia käyttävän julkaisun yhteydessä (ks. [play/JULKAISU.md](play/JULKAISU.md#3-play-console), vaihe 5). Valmiit vastaukset: [play/LOMAKKEET.md](play/LOMAKKEET.md).

## 3. Varasuunnitelma, jos poikkeusta ei myönnetä

| Toiminto | Nykyinen | Ilman SMS-lupia |
|---|---|---|
| Komennon lähetys | `SmsManager`, taustalla | **SMS-intent**: viestisovellus avautuu valmiilla tekstillä, ja käyttäjä painaa Lähetä |
| Vastauksen vastaanotto | `SMS_RECEIVED` | **SMS User Consent API**: käyttäjä hyväksyy yhden viestin lukemisen. API on tehty vahvistuskoodeille, joten sopivuus lämmittimen vastauksiin pitää testata. |
| Widgetin Tarkista tila | Taustalla | Ei onnistu ilman käyttäjän toimia |
| Luetuksi merkintä | NotificationListenerService | Sallittu, kunhan käyttö kerrotaan selvästi ennen oikeuden pyytämistä |

## 4. Muut luvat

- **Ilmoitusten käyttöoikeus** (`BIND_NOTIFICATION_LISTENER_SERVICE`): sallittu, mutta sovelluksessa pitää olla selkeä ilmoitus siitä, mihin oikeutta käytetään. Info ja asetukset -näkymän selitys on hyvä pohja.
- **POST_NOTIFICATIONS**, widget ja AlarmManager: ei rajoituksia.

## 5. Muutokset koodiin

- [x] Upload-avain: `signingConfigs.upload`, tiedot `keystore.properties`-tiedostosta, joka ei ole versionhallinnassa
- [x] `play`-build: `bundlePlay`, R8-minifiointi (`isMinifyEnabled = true`), resurssien karsinta ja ProGuard-säännöt
- [x] Selitysnäyttö ennen ilmoitusten käyttöoikeuden pyytämistä (prominent disclosure): *Anna käyttöoikeus* avaa ensin dialogin, joka kertoo mihin oikeutta käytetään ja mitä sillä ei tehdä. Androidin asetus avautuu vasta *Jatka asetuksiin* -painikkeesta.
- [x] Tietosuojaselosteen linkki sovellukseen: Info ja asetukset → Tietoja → *Tietosuojaseloste* (myös *Lähdekoodi*-linkki)

Lähde: [Google Play – SMS- ja puhelulokilupien käyttö](https://support.google.com/googleplay/android-developer/answer/10208820)
