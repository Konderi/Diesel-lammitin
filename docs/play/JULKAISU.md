# Julkaisu Google Play -kauppaan: vaiheet

Tämä ohje täydentää muistiota [../PLAY-JULKAISU.md](../PLAY-JULKAISU.md). Tässä hakemistossa:

| Tiedosto | Sisältö |
|---|---|
| [KAUPPA-TEKSTIT.md](KAUPPA-TEKSTIT.md) | Nimi, lyhyt ja pitkä kuvaus englanniksi ja suomeksi |
| [LOMAKKEET.md](LOMAKKEET.md) | Data safety, SMS-lupien ilmoitus ja muut lomakkeet |
| [privacy.html](privacy.html) | Tietosuojaseloste (fi/en). Julkaistu: https://konderi.github.io/privacy/heatcall/ (repo [Konderi/privacy](https://github.com/Konderi/privacy)) |
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

1. **Luo sovellus:** nimi *HeatCall*, oletuskieli englanti (en-US), sovellus, **maksullinen**. Ks. [Hinnoittelu](#hinnoittelu-490-).
2. **Sovelluksen sisältö:** täytä lomakkeet tiedoston [LOMAKKEET.md](LOMAKKEET.md) mukaan. Tietosuojaselosteen URL: **https://konderi.github.io/privacy/heatcall/**
3. **Kaupan sivu:** tekstit tiedostosta [KAUPPA-TEKSTIT.md](KAUPPA-TEKSTIT.md) ja kuvat hakemistosta `assets/`. Lisää suomi käännökseksi.
4. **Testaus → Sisäinen testaus:** luo julkaisu, lataa `app-play.aab`, lisää itsesi testaajaksi ja asenna Playn kautta. Testaa tilakysely, käynnistys, vastaus ja luetuksi merkintä.
5. **Tekstiviestilupien ilmoitus:** lähetetään ensimmäisen SMS-lupia käyttävän julkaisun yhteydessä. Arviointi kestää yleensä muutamasta päivästä viikkoihin. Jos hakemus hylätään, katso varasuunnitelma tiedostosta [../PLAY-JULKAISU.md](../PLAY-JULKAISU.md#3-varasuunnitelma-jos-poikkeusta-ei-myönnetä).
6. **Suljettu testaus** (pakollinen henkilötileille, jotka on luotu marraskuun 2023 jälkeen): vähintään 12 testaajaa 14 päivän ajan. Sen jälkeen voi hakea tuotantoon.
7. **Tuotanto:** luo julkaisu samasta AAB:sta, maat (esim. Suomi + EU) ja julkaise.

## Hinnoittelu: 4,90 €

**Tärkeää:** ilmaista sovellusta ei voi myöhemmin muuttaa maksulliseksi. Maksullisen voi muuttaa ilmaiseksi. Valitse siis *maksullinen* jo sovellusta luotaessa.

1. **Maksuprofiili:** Play Console → *Asetukset → Maksuprofiili*. Kauppiastili ja pankkitili pitää olla vahvistettu ennen kuin hinnan voi asettaa.
2. **Hinta:** *Kaupallistaminen → Tuotteet → Sovelluksen hinnoittelu* → aseta hinnaksi **4,90 EUR**.
   - EU-maissa hinta sisältää arvonlisäveron. Google tilittää veron ja maksaa sinulle summan ilman veroa.
   - Muihin maihin Google laskee paikallisen hinnan automaattisesti, ja voit muuttaa sitä maakohtaisesti.
3. **Arvio tulosta (Suomi):** 4,90 € sisältää alv:n 25,5 % → 3,90 €. Googlen palvelumaksu on 15 % ensimmäisestä miljoonasta dollarista vuodessa → noin **3,32 € myyntiä kohden**.
4. **Testaajat:** maksullisen sovelluksen testaajat joutuvat ostamaan sovelluksen myös testiraiteella. Anna testaajille **promokoodit** (*Kaupallistaminen → Promokoodit*, enintään 500 koodia neljännesvuodessa), jolloin he saavat sovelluksen ilmaiseksi. Tämä koskee myös henkilötilin 12 testaajan suljettua testausta.
5. **Palautukset:** Google palauttaa rahat automaattisesti, jos ostaja poistaa sovelluksen kahden tunnin sisällä ostosta.

Kaupan kuvaukseen kannattaa lisätä maininta vaatimuksista (EasyStart Call -yksikkö ja SIM-kortti), jotta laitteettomat ostajat eivät osta sovellusta turhaan. Maininta on jo tiedostossa [KAUPPA-TEKSTIT.md](KAUPPA-TEKSTIT.md).

## 4. Tarkistuslista ennen lähetystä

- [ ] Sovellus luotu maksullisena, maksuprofiili vahvistettu ja hinta 4,90 € asetettu
- [ ] Promokoodit testaajille
- [x] Tietosuojaseloste julkaistu: https://konderi.github.io/privacy/heatcall/ (yhteysosoite heatcall@konderi.fi)
- [ ] Alias heatcall@konderi.fi toimii (lähetä testiviesti)
- [ ] Upload-avain ja `keystore.properties` varmuuskopioitu
- [ ] `versionCode` nostettu edellisestä Play-julkaisusta
- [ ] AAB testattu sisäisessä testauksessa oikealla puhelimella ja lämmittimellä
- [ ] Data safety, ikäluokitus, kohdeyleisö ja SMS-ilmoitus täytetty
- [ ] Kaupan sivu: kuvake, esittelykuva, vähintään 2 kuvakaappausta (en ja fi)
