# EasyStart Call – SMS-protokolla

Lähde: Eberspächer EasyStart Call -käyttöohje (FI, 08.2014). Toteutus: [`Commands.kt`](../app/src/main/java/fi/heatcall/sms/Commands.kt) ja [`ReplyParser.kt`](../app/src/main/java/fi/heatcall/sms/ReplyParser.kt).

## Yleistä

- Muoto: `pin:<PIN> <komento>`. PIN-koodin ja komennon välissä on **aina välilyönti**.
- Laite hyväksyy sekä isot että pienet kirjaimet.
- Komennon parametrit erotetaan pilkulla, esim. `heater:on,run:30`.
- Jos lähettäjän numero on valtuutettu, PIN-koodia ei tarvita. HeatCall lähettää sen silti aina.
- Laite lähettää vastauksen vain, jos **kuittausilmoitus** on päällä. Kyselyihin (`?`) se vastaa aina.

## Lämmitys

| Toiminto | Komento | Vastaus (esimerkki) |
|---|---|---|
| Lämmitys päälle (viimeisimmät asetukset) | `pin:1234 heater:on` | `Heater:on,mode:heater,run:45` |
| Lämmitys päälle, käyntiaika | `pin:1234 heater:on,run:30` | `Heater:on,mode:heater,run:30` |
| Kestokäyttö (vain ilmalämmitin) | `pin:1234 heater:on,run:perm,sp:24` | `Heater:on,mode:heater,run:perm,SP:24` |
| Tuuletus päälle | `pin:1234 heater:on,mode:fan,run:30` | `Heater:on,mode:fan,run:30` |
| Pois | `pin:1234 heater:off` | `Heater:off` |
| Tilakysely | `pin:1234 heater?` | `Heater:on,mode:heater,run:45` |
| Sisälämpötila | `pin:1234 temp?` | `Temp:ok,value:28` |

- `run`: 10–120 minuuttia tai `perm` (kestokäyttö, vain ilmalämmittimet).
- `sp`: asetuslämpötila 8–36 °C (vain ilmalämmittimet). Yksinumeroiset arvot ilman etunollaa (`9`, ei `09`).

## Ajastimet

| Toiminto | Komento | Vastaus |
|---|---|---|
| Ohjelmointi | `pin:1234 t1:on,time:MO-FR.06.30,run:30` | `T1:on,time:MO-FR.06.30,mode:heater,run:30` |
| Ohjelmointi, tuuletus | `pin:1234 t2:on,time:FR.16.30,mode:fan,run:40` | `T2:on,time:FR.16.30,mode:fan,run:40` |
| Aktivointi (tallennetuilla arvoilla) | `pin:1234 t1:on` | `T1:on,…` |
| Poisto käytöstä | `pin:1234 t1:off` | `T1:off` |
| Yhden ajastimen tila | `pin:1234 t1?` | `T1:on,time:…` tai `T1:off` |
| Kaikkien tila | `pin:1234 tall?` | `T1:…,T2:…,T3:…` |

- Ajastin: `t1`, `t2` tai `t3`.
- `time`: `<PÄIVÄ>.<HH>.<MM>`. Päivä on `MO TU WE TH FR SA SU` tai alue `MO-FR`, `SA-SU`, `MO-SU`.
- Oletuksena aika on **lähtöaika** (end): lämmitin käynnistyy käyntiajan verran ennen ja sammuu asetettuna aikana. Aloitusaikatila (start) asetetaan asennuksen yhteydessä.
- Kestokäyttö ei ole mahdollinen ajastimella.

## Laite

| Toiminto | Komento | Vastaus |
|---|---|---|
| Kellon kysely | `pin:1234 devicetime?` | `DeviceTime:ok,time:TU.14.20` |
| Kellon asetus | `pin:1234 devicetime:set,time:FR.06.30` | `DeviceTime:ok,time:FR.06.30` |
| Valtuutettu numero | `pin:1234 mainphone?` | `MainPhone:ok,number:+358…` |
| Valtuuta lähettäjä | `pin:1234 mainphone:set,number:mine` | `MainPhone:ok,number:+358…` |
| Valtuuta numero | `pin:1234 mainphone:set,number:+358401234567` | `MainPhone:ok,number:…` |
| PIN-koodin vaihto | `pin:1234 config:set,pin:9876` | – |

Laitteen kello ja viikonpäivä pitää asettaa uudelleen jokaisen virtakatkon jälkeen. Myös kesä- ja talviaika vaihdetaan käsin.

## Virheilmoitukset

Virheilmoitukset lähetetään vain, jos `ErrorFeedback:on` on asetettu asennuksessa.

| Vastaus | Merkitys |
|---|---|
| `ErD:HeaterCom:lost,Heater:off` | Tiedonsiirto lämmittimeen katkesi |
| `ErD:HeaterCom:no_heater_found` | Lämmitintä ei tunnistettu käyttöönotossa |
| `ErL:DatCom:lost` | Johdotusvika |
| `ErH:Heater:Error` / `ErH:HeaterError` | Lämmittimessä on häiriö |
| `ErH:Heater:mode_not_supported` | Lämmitin ei tue pyydettyä toimintoa |
| `ErC:Temp:missing` | Lämpötila-anturia ei ole asennettu |
| `ErC:Temp:short_circuit` | Lämpötila-anturi on viallinen |
| `ErC:DeviceTime:time_not_set` | Laitteen kelloa ei ole asetettu |
| `ErR:DeviceTime:time_not_set,T2:off` | Ajastinta ei aktivoitu, koska kelloa ei ole asetettu |

## Vastausten tulkinta HeatCallissa

`ReplyParser` pilkkoo vastauksen pilkuista `avain:arvo`-pareiksi ja muodostaa niistä suomenkielisen otsikon ja rivit. Tunnistamattomat osat näytetään sellaisenaan, joten tietoa ei katoa. Lämmittimen tila (`Heater:on/off`) tallennetaan laitteen viimeisimmäksi tunnetuksi tilaksi, joka näkyy kortissa ja widgetissä.
