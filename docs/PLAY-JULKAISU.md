# Julkaisu Google Play -kauppaan

**Suomi** · [English](en/PLAY-RELEASE.md)

Tämä on muistio siitä, mitä Play-julkaisu vaatisi. HeatCall toimii sivuladattuna (ks. [ASENNUS.md](ASENNUS.md)), joten julkaisu ei ole välttämätön.

## Päätelmä

Teknisesti julkaisu on pieni työ. Ratkaiseva kysymys on **Googlen tekstiviestilupia koskeva käytäntö**: `SEND_SMS` ja `RECEIVE_SMS` sallitaan vain oletusviestisovellukselle tai hyväksytylle poikkeukselle, ja Google tarkistaa poikkeukset käsin. Jos poikkeusta ei myönnetä, sovellus pitäisi muuttaa niin, ettei se lähetä tai vastaanota viestejä taustalla.

## 1. Perusvaatimukset

| Asia | Tila / toimenpide |
|---|---|
| Play Console -kehittäjätili ja henkilöllisyyden varmennus | Kunnossa |
| Hinta | Ilmainen ja avointa lähdekoodia, ei maksuprofiilia (ks. [play/JULKAISU.md](play/JULKAISU.md#hinnoittelu-ilmainen)) |
| Suljettu testaus: uusilla henkilökohtaisilla tileillä vähintään 12 testaajaa 14 päivän ajan | Tarvitaan, jos tili on henkilökohtainen ja luotu marraskuun 2023 jälkeen |
| **Android App Bundle (AAB)** Play ottaa vastaan vain AAB:n, ei APK:ta. | `./gradlew bundleRelease` |
| **Upload-avain** + Play App Signing | Nykyinen debug-allekirjoitus ei käy, joten luodaan oma avain (ks. [KEHITYS.md](KEHITYS.md#allekirjoitus)) |
| `applicationId` | `fi.heatcall`. Tunnusta ei voi vaihtaa julkaisun jälkeen. |
| targetSdk | 36 täyttää nykyiset vaatimukset |
| Tietosuojaseloste (julkinen URL) | Julkaistu: https://konderi.github.io/privacy/heatcall/ |
| Tietoturvalomake (Data safety) | Tekstiviestit ja puhelinnumero käsitellään vain laitteella, eikä niitä lähetetä minnekään |
| Ikäluokitus, kaupan kuvaus, kuvake 512 px, mainoskuva 1024×500 px, kuvakaappaukset | Kuvakaappaukset löytyvät hakemistosta `docs/images` |
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

Kannattaa hakea ensin suljetussa testissä, ennen kuin muuhun käytetään aikaa.

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

## 5. Tarvittavat muutokset koodiin

1. `signingConfigs.release` omalla upload-avaimella. Salasanat luetaan `keystore.properties`-tiedostosta, joka ei ole versionhallinnassa.
2. `bundleRelease` ja tarvittaessa R8-minifiointi (`isMinifyEnabled = true`) sekä ProGuard-säännöt.
3. Selitysnäyttö ennen ilmoitusten käyttöoikeuden pyytämistä (prominent disclosure).
4. Tietosuojaselosteen linkki sovellukseen (Info ja asetukset → Tietoja).

Lähde: [Google Play – SMS- ja puhelulokilupien käyttö](https://support.google.com/googleplay/android-developer/answer/10208820)
