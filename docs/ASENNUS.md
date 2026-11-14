# HeatCall – asennus

HeatCall ei ole Google Play -kaupassa, joten se asennetaan APK-tiedostosta (niin sanottu sivulataus). Sovellus toimii Android 10:ssä ja uudemmissa.

## APK-tiedosto

Buildin jälkeen APK löytyy kahdesta paikasta:

| Polku | Kuvaus |
|---|---|
| `app/build/outputs/apk/release/app-release.apk` | Gradlen tuottama release-APK |
| `dist/HeatCall-1.0.apk` | Sama tiedosto jakoa varten nimettynä (ei versionhallinnassa) |

Buildaus, ks. [KEHITYS.md](KEHITYS.md#buildaus).

## Tapa 1: USB-kaapeli (suositeltu)

1. Ota puhelimessa käyttöön kehittäjäasetukset: **Asetukset → Tietoja puhelimesta → Koontiversionumero**, napauta 7 kertaa.
2. **Asetukset → Järjestelmä → Kehittäjäasetukset → USB-virheenkorjaus** päälle.
3. Kytke puhelin Maciin ja hyväksy puhelimessa virheenkorjaus.
4. Joko Android Studiossa: avaa projekti, valitse puhelin ja paina ▶ **Run**, tai komentorivillä:

```bash
~/Library/Android/sdk/platform-tools/adb install -r dist/HeatCall-1.0.apk
```

Kaapeliasennuksessa Android ei käsittele sovellusta "tuntemattomasta lähteestä" tulevana, joten [rajoitetut asetukset](#rajoitetut-asetukset) eivät koske sitä.

## Tapa 2: Ilman kaapelia

APK:n voi siirtää puhelimeen myös ilman kaapelia.

### Sähköposti – ei toimi Gmaililla

**Gmail estää `.apk`-liitteet**, myös zip-pakattuina, joten sähköposti ei ole luotettava tapa. Muut sähköpostipalvelut voivat toimia, mutta pilvilinkki on varmempi.

### Toimivat tavat

| Tapa | Ohje |
|---|---|
| **Google Drive / iCloud Drive / OneDrive** | Lataa APK pilveen Macilta ja avaa se puhelimessa pilvipalvelun sovelluksesta tai selaimesta. |
| **GitHub Release** | Liitä APK repositorion Releaseen. Avaa julkaisu puhelimen selaimessa (kirjautuneena, jos repo on yksityinen) ja lataa APK. |
| **Quick Share / Lähijako** | Jaa tiedosto suoraan toisesta Android-laitteesta tai Windows-koneesta. |
| **Langaton virheenkorjaus** | Android 11+: Kehittäjäasetukset → Langaton virheenkorjaus → Muodosta pari koodilla. Sen jälkeen `adb pair` ja `adb connect`, minkä jälkeen asennus toimii kuten kaapelilla. |

### Asennus puhelimessa

1. Avaa ladattu `HeatCall-1.0.apk` (esim. Tiedostot → Lataukset).
2. Android kysyy luvan asentaa tuntemattomia sovelluksia siitä sovelluksesta, josta tiedosto avattiin (selain, Drive, Tiedostot). Salli lupa.
3. **Play Protect** voi varoittaa tuntemattomasta kehittäjästä. Valitse **Lisätietoja → Asenna silti**. Varoitus johtuu siitä, että sovellusta ei ole julkaistu Play-kaupassa.
4. Avaa HeatCall ja tee [ensimmäisen käynnistyksen](KAYTTOOHJE.md#1-ensimmäinen-käynnistys) asetukset.

## Rajoitetut asetukset

Android 13:sta alkaen tiedostosta tai selaimesta asennettu sovellus ei saa heti pyytää kaikkia arkaluonteisia oikeuksia. HeatCallin kohdalla tämä koskee:

- **ilmoitusten käyttöoikeutta**, jota luetuksi merkintä tarvitsee
- uusimmissa Android-versioissa mahdollisesti myös **tekstiviestioikeuksia**

Jos asetus näkyy harmaana tai Android ilmoittaa *"Rajoitettu asetus"*:

1. **Asetukset → Sovellukset → HeatCall**
2. Oikean yläkulman **⋮**-valikko → **Salli rajoitetut asetukset** (vahvista PIN-koodilla tai sormenjäljellä)
3. Palaa HeatCalliin ja anna oikeus uudelleen

## Päivittäminen

Uusi versio asennetaan vanhan päälle samalla tavalla. Tiedot (laitteet, ajastimet, historia) säilyvät.

**Allekirjoitus:** päivitys onnistuu vain, jos uusi APK on allekirjoitettu **samalla avaimella** kuin asennettu versio. Tällä hetkellä release-build allekirjoitetaan buildikoneen Android-debug-avaimella (`~/.android/debug.keystore`). Siksi:

- Buildaa päivitykset samalla Macilla, tai kopioi `debug.keystore` talteen.
- Jos avain vaihtuu, vanha versio pitää poistaa ennen uuden asennusta. Tällöin tiedot katoavat.

Jos sovellusta halutaan jakaa laajemmin, kannattaa luoda oma release-avain. Ohje on tiedostossa [KEHITYS.md](KEHITYS.md#allekirjoitus).

## Toiseen puhelimeen

Sama APK toimii missä tahansa Android 10+ -puhelimessa. Jokaiseen puhelimeen syötetään lämmittimen numero ja PIN erikseen. Jos haluat, että myös toinen puhelin saa lämmittimen virheilmoitukset, valtuuta se kohdasta **Info ja asetukset → Valtuuta tämä puhelin**. Huomaa, että EasyStart Callissa voi olla vain yksi valtuutettu numero kerrallaan.
