# HeatCall – käyttöohje

HeatCall ohjaa Eberspächer-seisontalämmitintä tekstiviesteillä EasyStart Call -yksikön kautta. Jokainen komento on tekstiviesti, josta liittymäsi voi veloittaa.

## 1. Ensimmäinen käynnistys

<img src="images/01-tervetuloa.png" width="240" align="right" alt="Tervetuloa-näkymä">

Sovellus kysyy ensimmäisellä käynnistyskerralla:

| Kenttä | Selitys |
|---|---|
| **Kieli / Language** | Suomi tai English. Oletus on puhelimen kieli. Kielen voi vaihtaa myöhemmin kohdasta **Info ja asetukset → Kieli**. |
| **Lämmittimen puhelinnumero** | EasyStart Callin SIM-kortin numero, esim. `+358 40 123 4567` tai `040 123 4567` |
| **PIN-koodi** | EasyStart Callin nelinumeroinen PIN (tehdasasetus `1234`) |
| **Nimi** | Auton nimi, joka näkyy kortissa ja widgetissä |
| **Lämmittimen tyyppi** | *Vesilämmitin* (Hydronic) tai *Ilmalämmitin* (Airtronic). Kestokäyttö (∞) on vain ilmalämmittimissä. |
| **Auton kuva** | Valinnainen kuva puhelimen galleriasta |

Kun tallennat tiedot, Android kysyy luvat:

- **Tekstiviestien lähetys ja vastaanotto:** pakollinen, koska ilman sitä sovellus ei voi ohjata lämmitintä
- **Ilmoitukset:** vastaus näytetään ilmoituksena, kun sovellus on taustalla

Jos tekstiviestilupa evätään (esim. Android näyttää *"Sovelluksen lupa kielletty"*), HeatCall avaa automaattisesti ohjeen **Lupien salliminen**. Ks. [Asennus → Rajoitetut asetukset](ASENNUS.md#rajoitetut-asetukset).

Numeroa ja PIN-koodia voi muuttaa myöhemmin: **Info ja asetukset → Laitteet → Muokkaa** tai rataskuvake → **Muokkaa laitetta**.

<br clear="right">

## 2. Päänäkymä

<img src="images/02-paanakyma.png" width="240" align="right" alt="Päänäkymä">

Jokaisella autolla on oma kortti:

- **Minuutit-rulla:** käyntiaika 10–120 minuuttia viiden minuutin välein. Kun valitset **Pois**, painike muuttuu SAMMUTA-painikkeeksi. Ilmalämmittimellä rullassa on lisäksi **∞** (kestokäyttö).
- **Tila-rulla:** **Lämmitys** tai **Tuuletus**. Tuuletus käyttää auton puhallinta ilman lämmitystä. Kaikki lämmittimet eivät tue sitä.
- **Tilarivi:** viimeisin lämmittimen ilmoittama tila ja milloin se päivittyi.
- **Tarkista tila:** kysyy lämmittimen tilan heti.
- **Rataskuvake ▾:** avaa valikon, josta löytyvät Kysy tila, Sisälämpötila, Ajastimet, **Laitteen kello**, Muokkaa laitetta ja Poista laite.
- **Kesä-/talviaikamuistutus:** kolme viikkoa aikasiirtymän jälkeen kortissa näkyy *"Talviaika alkoi – päivitä lämmittimen kello"*, kunnes kello on asetettu.

Pyöritä rullat haluamiisi arvoihin ja paina keskellä olevaa **KÄYNNISTÄ**-painiketta. Valinnat tallentuvat, joten seuraavalla kerralla riittää yksi painallus.

Jos autoja on useampi, valitse ohjattava auto koskettamalla sen korttia. Valittu kortti näkyy sinisellä reunuksella.

<br clear="right">

## 3. Vastausikkuna ja luetuksi merkintä

<img src="images/03-vastaus.png" width="240" align="right" alt="Vastausikkuna">

Kun lämmitin vastaa, vastaus avautuu omaan ikkunaansa suomeksi tulkittuna. Alkuperäinen viesti näkyy ikkunan alareunassa.

- Jos sovellus on auki, ikkuna aukeaa heti.
- Jos sovellus on taustalla, saat HeatCall-ilmoituksen, josta ikkuna aukeaa.
- Sama viesti merkitään automaattisesti **luetuksi** puhelimen viestisovelluksessa, joten se ei jää lukemattomaksi.

Luetuksi merkintä vaatii **ilmoitusten käyttöoikeuden**: **Info ja asetukset → Vastausviestit → Anna käyttöoikeus**. Asetuksista näet myös, mikä viestisovellus on puhelimen oletus ja onko käyttöoikeus annettu. Toiminnon voi kytkeä pois samasta kohdasta.

> **Huom!** Lämmitin vastaa käynnistykseen ja sammutukseen vain, jos EasyStart Callin **kuittausilmoitus** on päällä (tehdasasetus on pois). Asennusliike voi kytkeä sen päälle. Tilakyselyyn lämmitin vastaa aina.

<br clear="right">

## 4. Tilan tarkistus

<img src="images/04-tarkista-tila.png" width="240" align="right" alt="Tarkista tila">

Paina kortin **Tarkista tila** -painiketta. Sovellus lähettää komennon `heater?`, ja rivillä näkyy "Tarkistetaan…".

- Vastaus tulee yleensä muutamassa sekunnissa tai minuutissa riippuen verkosta.
- Jos vastausta ei tule 3 minuutissa, sovellus kertoo siitä. Tarkista silloin, että autossa on kenttää (esim. ettei auto ole maanalaisessa parkkihallissa).

<br clear="right">

## 5. Kotinäytön widget

<img src="images/08-widget.png" width="240" align="right" alt="Widget">

Widget näyttää valitun auton lämmittimen tilan ja sisältää **Tarkista tila** -painikkeen, joten tilan voi tarkistaa avaamatta sovellusta.

**Lisääminen:**
- **Info ja asetukset → Kotinäytön widget → Lisää widget kotinäytölle**, tai
- paina kotinäyttöä pitkään → **Widgetit** → **HeatCall**

**Toiminta:**
- Tilapallo on oranssi, kun lämmitin on päällä, ja harmaa, kun se on pois. Läpinäkyvä pallo tarkoittaa, että tilaa ei tiedetä.
- Kun painat Tarkista tila, painikkeessa näkyy "Odotetaan vastausta", kunnes lämmitin vastaa.
- Kun kosketat widgetin muuta aluetta, sovellus aukeaa.
- Jos autoja on useampi, widget näyttää sovelluksessa valittuna olevan auton.

Widgetistä ei voi käynnistää lämmitintä, jotta lämmitin ei käynnisty vahingossa.

<br clear="right">

## 6. Ajastimet

<img src="images/06-ajastimet.png" width="240" align="right" alt="Ajastimet">

Rataskuvake → **Ajastimet**. Laitteessa on kolme ajastinta (T1–T3).

- **Päivä:** yksittäinen päivä tai alue *Ma–pe*, *La–su* tai *Joka päivä*.
- **Aika on lähtöaika:** lämmitin käynnistyy käyntiajan verran ennen asetettua aikaa ja sammuu asetettuna aikana. Esimerkiksi lähtöaika 7.30 ja käyntiaika 30 min tarkoittavat, että lämmitys on päällä klo 7.00–7.30.
- **Ohjelmoi ja päälle:** lähettää ajastimen asetukset ja aktivoi ajastimen.
- **Pois:** poistaa ajastimen käytöstä.
- **Tila / Kysy kaikkien ajastinten tila:** näyttää laitteeseen tallennetut ajastimet.

Kestokäyttö ei ole mahdollinen ajastimella.

> **Tärkeää:** ajastin toimii vain, jos **laitteen kello** on oikeassa ajassa. Ks. [Laitteen kello](#7-laitteen-kello).

<br clear="right">

## 7. Laitteen kello

<img src="images/13-laitteen-kello.png" width="240" align="right" alt="Laitteen kello">

EasyStart Call tietää vain viikonpäivän ja kellonajan. **Se ei vaihda kesä- ja talviaikaa itse**, joten aikasiirtymän jälkeen ajastimet laukeavat tunnin väärään aikaan. Kello pitää asettaa uudelleen myös jokaisen virtakatkon jälkeen (esim. akun vaihto).

Avaa **rataskuvake → Laitteen kello**, tai napauta kortin muistutusta *"Talviaika alkoi – päivitä lämmittimen kello"*.

| Painike | Toiminto |
|---|---|
| **Aseta puhelimen ajasta** | Lähettää puhelimen viikonpäivän ja kellonajan (esim. `devicetime:set,time:SA.16.09`). Puhelimen aika on aina oikeassa kesä- tai talviajassa. |
| **Kysy laitteen kello** | Kysyy laitteen ajan. Vastausikkuna vertaa sitä puhelimen aikaan. |
| **Aseta käsin…** | Valitse viikonpäivä ja kellonaika itse. |

<br clear="right">

<img src="images/14-kellon-poikkeama.png" width="240" align="right" alt="Kellon poikkeama">

Kun laite vastaa kellokyselyyn, vastausikkuna näyttää poikkeaman, esim. *"Laitteen kello on 1 h edellä puhelimen ajasta"*, ja painikkeen **Aseta oikea aika**. Alle 10 minuutin ero johtuu tekstiviestin viiveestä, ja silloin kello näytetään oikeana.

Kun kello asetetaan HeatCallista, aika tallentuu ja kesä-/talviaikamuistutus poistuu.

<br clear="right">

## 8. Info ja asetukset

<img src="images/07-asetukset.png" width="240" align="right" alt="Asetukset">

| Kohta | Toiminto |
|---|---|
| Kieli | Suomi / English. Vaihto näkyy heti koko sovelluksessa, ilmoituksissa ja widgetissä. |
| Käyttöoikeudet | Lupien tila (tekstiviestit, ilmoitukset, ilmoitusten käyttöoikeus, akun optimointi), *Pyydä luvat*, *Taustatoiminnan asetukset* (ohje puhelimen merkin mukaan: Samsung, Pixel, OPPO/OnePlus/realme, Xiaomi) ja *Luvat estetty? Näytä ohje* |
| Laitteet | Autojen numero, PIN, tyyppi ja kuva |
| Vastausviestit | Luetuksi merkintä päälle tai pois, oletusviestisovellus ja ilmoitusten käyttöoikeus |
| Laitetoiminnot | Aseta laitteen kello puhelimen ajasta, Kysy laitteen kello, Valtuuta tämä puhelin, Kysy valtuutettu numero |
| Kotinäytön widget | Widgetin lisäys |

**Valtuuta tämä puhelin** (`mainphone:set,number:mine`) tallentaa puhelimesi numeron EasyStart Calliin valtuutetuksi numeroksi. Valtuutettuun numeroon lähetetään virheilmoitukset, eikä tästä numerosta soitettaessa kysytä PIN-koodia.

<br clear="right">

## 9. Alapalkki

| Painike | Sisältö |
|---|---|
| **Info ja asetukset** | Asetukset, ks. yllä |
| **Lisää laite** | Uuden auton tai lämmittimen lisäys |
| **Ajankohtaista** | Viestihistoria (150 viimeisintä). PIN-koodi näkyy muodossa `pin:****`. |
| **Ohjeet** | Lyhyt käyttöohje, virhekoodit ja komennot |

## 10. Virheilmoitukset

| Koodi | Merkitys | Toimenpide |
|---|---|---|
| `ErD:HeaterCom:lost` | Yhteys EasyStart Callin ja lämmittimen välillä katkesi | Yritä uudelleen; jos toistuu, tarkastuta asennus |
| `ErD:HeaterCom:no_heater_found` | Lämmitintä ei tunnistettu | Irrota sulake hetkeksi ja yritä uudelleen |
| `ErH:Heater:Error` | Lämmittimessä on häiriö | Yritä uudelleen; jos toistuu, vie huoltoon |
| `ErH:Heater:mode_not_supported` | Lämmitin ei tue toimintoa (esim. tuuletus) | Valitse toinen tila |
| `ErC:Temp:missing` | Lämpötila-anturia ei ole asennettu | – |
| `ErC:Temp:short_circuit` | Lämpötila-anturi on viallinen | Huolto |
| `ErC/ErR:DeviceTime:time_not_set` | Laitteen kelloa ei ole asetettu | Aseta laitteen kello |
| `ErL:DatCom:lost` | Johdotusvika | Huolto |

## 11. Vianetsintä

| Ongelma | Ratkaisu |
|---|---|
| Päänäkymässä oranssi varoitus **Tekstiviestilupa puuttuu** | Napauta varoitusta ja seuraa ohjetta, ks. [Rajoitetut asetukset](ASENNUS.md#rajoitetut-asetukset). |
| Lupaikkunassa **Salli** harmaana / *"Sovelluksen lupa kielletty"* | Sovellustiedot → ⋮ → **Salli rajoitetut asetukset**, ks. [Asennus](ASENNUS.md#rajoitetut-asetukset). |
| Vastaukset tulevat vain, kun sovellus on auki (Oppo, Xiaomi ym.) | Salli taustatoiminta ja automaattinen käynnistys, ks. [ColorOS](ASENNUS.md#oppo--oneplus--realme-coloros). |
| Lämmitin ei vastaa mihinkään | Tarkista, että autossa on kenttää, EasyStart Callin SIM-kortti on voimassa ja numero on oikein. Kokeile **Tarkista tila**. |
| Tilakysely toimii, mutta käynnistykseen ei tule vastausta | Kuittausilmoitus on pois päältä (tehdasasetus). Lämmitin todennäköisesti käynnistyi; varmista asia tilakyselyllä. |
| "Laitteen numero tai PIN puuttuu" | Täydennä tiedot kohdassa Muokkaa laitetta. |
| Vastaus ei merkitty luetuksi | 1) Tarkista **Info ja asetukset → Käyttöoikeudet**: *Ilmoitusten käyttöoikeus* = OK. Jos Android ilmoittaa "rajoitettu asetus", katso [Asennus](ASENNUS.md#rajoitetut-asetukset). 2) Oppo/Xiaomi: salli taustatoiminta ja automaattinen käynnistys. 3) Lähetä tilakysely ja katso **Info ja asetukset → Vianmääritys: luetuksi merkintä**, ks. alla. |
| Ajastin ei käynnistä lämmitintä tai käynnistää tunnin väärään aikaan | Laitteen kello on väärässä ajassa, usein kesä-/talviajan vaihdon jälkeen. **Rataskuvake → Laitteen kello → Aseta puhelimen ajasta**, ja tarkista sen jälkeen ajastimen tila. |
| "Ei verkkoyhteyttä" tai "Lentokonetila on päällä" | Viestiä ei lähetetty puhelimesta. Tarkista puhelimen oma yhteys. |

## 12. Luetuksi merkinnän vianmääritys

**Info ja asetukset → Vianmääritys: luetuksi merkintä** näyttää 30 viimeisintä tapahtumaa. Lokiin tallennetaan vain lähettäjä ja ilmoituksen toimintojen nimet, ei viestien sisältöä. **Jaa**-painikkeella lokin voi lähettää eteenpäin.

| Lokissa näkyy | Merkitys ja korjaus |
|---|---|
| *Kuuntelija: ei yhdistetty* | Ilmoitusten käyttöoikeus puuttuu, tai järjestelmä on pysäyttänyt kuuntelijan. Anna käyttöoikeus ja salli taustatoiminta (Oppo: Akun käyttö → taustatoiminta ja automaattinen käynnistys). |
| Ei riviä *Lämmittimen viesti vastaanotettu* | HeatCall ei saanut tekstiviestiä. Tarkista tekstiviestilupa ja taustatoiminta. Tarkista myös, että lämmittimen numero on asetuksissa oikein. |
| *vastaanotettu*, mutta ei *merkitty luetuksi* -riviä | Viestisovelluksen ilmoitusta ei tunnistettu. Lähetä loki kehittäjälle. |
| *Merkitse luetuksi -toimintoa ei löytynyt. Toiminnot: [...]* | Viestisovellus ei tarjoa ilmoituksessaan Merkitse luetuksi -toimintoa, tai sen nimi on tuntematon. Lähetä loki: hakasulkeissa näkyvien nimien perusteella tunnistus voidaan lisätä. |
| *merkitty luetuksi (…)* | Toimii. Suluissa lukee, miten viesti tunnistettiin. |

Viesti tunnistetaan kolmella tavalla: juuri vastaanotettu viesti, lämmittimen viestimuoto (`Heater:…`, `T1:…`, `ErD:…` ym.) tai lähettäjän numero. Tunnistus toimii, vaikka lämmittimen numero olisi tallennettu yhteystietoihin nimellä.
