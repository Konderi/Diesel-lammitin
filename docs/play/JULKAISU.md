# Julkaisu Google Play -kauppaan: vaiheet

**Suomi** · [English](../en/play/RELEASE-STEPS.md)

Tämä ohje täydentää muistiota [../PLAY-JULKAISU.md](../PLAY-JULKAISU.md). Tässä hakemistossa:

| Tiedosto | Sisältö |
|---|---|
| [KAUPPA-TEKSTIT.md](KAUPPA-TEKSTIT.md) | Nimi, lyhyt ja pitkä kuvaus englanniksi ja suomeksi |
| [LOMAKKEET.md](LOMAKKEET.md) | Data safety, SMS-lupien ilmoitus ja muut lomakkeet |
| [TESTAAJAT.md](TESTAAJAT.md) | Suljettu testaus (12 testaajaa / 14 päivää), kutsuviesti fi/en |
| [../privacy/index.html](../privacy/index.html) | Tietosuojaseloste (fi/en). Julkaistu GitHub Pagesissa tästä reposta (`main` → `/docs`): https://konderi.github.io/HeatCall/privacy/ |
| `assets/` | Kuvake, esittelykuvat ja kuvakaappaukset |

## 1. Upload-avain

Play-build allekirjoitetaan omalla upload-avaimella. Google Play allekirjoittaa sen jälkeen jaettavan sovelluksen omalla avaimellaan (Play App Signing).

| Tiedosto | Sijainti | Versionhallinnassa |
|---|---|---|
| Avain | `~/.android/heatcall-upload.jks` | Ei |
| Salasanat | `keystore.properties` (projektin juuressa) | Ei (`.gitignore`) |

**Varmuuskopioi molemmat heti salasananhallintaan (esim. 1Password).** Jos upload-avain katoaa, sen voi nollata Play Consolen tuen kautta, mutta se vie aikaa.

Uudella koneella kopioi avain ja `keystore.properties` paikoilleen ja tarkista, että `storeFile`-polku osoittaa avaimeen.

## 2. Buildaus

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest bundlePlay
```

Tulos: `app/build/outputs/bundle/play/app-play.aab`. Build käyttää R8-minifiointia, joten koko on noin 4 Mt.

Jokaista uutta Play-julkaisua varten nosta `versionCode` (ja `versionName`) tiedostossa `app/build.gradle.kts`. Sivuladattava APK (`assembleRelease`, debug-avain) ja Play-versio ovat eri allekirjoituksella. Käyttäjän pitää poistaa sivuladattu versio ennen Play-version asentamista.

## 3. Play Console

1. **Luo sovellus:** nimi *HeatCall*, oletuskieli englanti (en-US), sovellus, **ilmainen**. Ks. [Hinnoittelu](#hinnoittelu-ilmainen).
2. **Sovelluksen sisältö:** täytä lomakkeet tiedoston [LOMAKKEET.md](LOMAKKEET.md) mukaan. Tietosuojaselosteen URL: **https://konderi.github.io/HeatCall/privacy/**
3. **Kaupan sivu:** tekstit tiedostosta [KAUPPA-TEKSTIT.md](KAUPPA-TEKSTIT.md) ja kuvat hakemistosta `assets/`. Lisää suomi käännökseksi.
4. **Testaus → Sisäinen testaus:** luo julkaisu, lataa `app-play.aab`, lisää itsesi testaajaksi ja asenna Playn kautta. Testaa tilakysely, käynnistys, vastaus ja luetuksi merkintä.
5. **Tekstiviestilupien ilmoitus:** lähetetään ensimmäisen SMS-lupia käyttävän julkaisun yhteydessä. Arviointi kestää yleensä muutamasta päivästä viikkoihin. Jos hakemus hylätään, katso varasuunnitelma tiedostosta [../PLAY-JULKAISU.md](../PLAY-JULKAISU.md#3-varasuunnitelma-jos-poikkeusta-ei-myönnetä).
6. **Suljettu testaus** (pakollinen henkilötileille, jotka on luotu marraskuun 2023 jälkeen): vähintään 12 testaajaa 14 päivän ajan. Sen jälkeen voi hakea tuotantoon.
7. **Tuotanto:** luo julkaisu samasta AAB:sta, maat (esim. Suomi + EU) ja julkaise.

## Hinnoittelu: ilmainen

HeatCall on ilmainen ja avointa lähdekoodia (https://github.com/Konderi/HeatCall). Siinä ei ole mainoksia eikä sovelluksen sisäisiä ostoja.

- **Ei maksuprofiilia:** ilmaisen sovelluksen julkaisu ei vaadi kauppiastiliä eikä pankkitietoja.
- **Ei promokoodeja:** testaajat asentavat sovelluksen suoraan liittymislinkin kautta.
- **Lopullinen valinta:** ilmaista sovellusta ei voi myöhemmin muuttaa maksulliseksi.
- **Lomakkeet:** *Sisältää mainoksia* = Ei. Kaupan sivulla ei ole sovelluksen sisäisiä ostoja.

Kaupan kuvauksessa mainitaan vaatimukset (EasyStart Call -yksikkö ja SIM-kortti), jotta käyttäjät tietävät ennen asennusta, mitä sovellus tarvitsee. Maininta on jo tiedostossa [KAUPPA-TEKSTIT.md](KAUPPA-TEKSTIT.md).

## 4. Tarkistuslista ennen lähetystä

- [ ] Sovellus luotu ilmaisena
- [ ] Lähdekoodirepo julkinen: https://github.com/Konderi/HeatCall
- [x] Tietosuojaseloste julkaistu: https://konderi.github.io/HeatCall/privacy/ (yhteysosoite heatcall@konderi.fi)
- [ ] Alias heatcall@konderi.fi toimii (lähetä testiviesti)
- [ ] Upload-avain ja `keystore.properties` varmuuskopioitu
- [ ] `versionCode` nostettu edellisestä Play-julkaisusta
- [ ] AAB testattu sisäisessä testauksessa oikealla puhelimella ja lämmittimellä
- [ ] Data safety, ikäluokitus, kohdeyleisö ja SMS-ilmoitus täytetty
- [ ] Kaupan sivu: kuvake, esittelykuva, vähintään 2 kuvakaappausta (en ja fi)
