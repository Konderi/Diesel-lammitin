# Play Consolen lomakkeet: valmiit vastaukset

## 1. Data safety (Tietoturva)

**Sovelluksen sisältö → Data safety**

| Kysymys | Vastaus |
|---|---|
| Kerääkö tai jakaako sovellus käyttäjätietoja? | **Ei.** Sovelluksella ei ole internet-oikeutta, eikä mitään tietoa lähetetä kehittäjälle tai kolmansille osapuolille. |
| Salataanko tiedot siirron aikana? | Ei sovellu (tietoja ei siirretä). |
| Voiko käyttäjä pyytää tietojen poistoa? | Tiedot ovat vain laitteella. Ne poistetaan sovelluksesta tai poistamalla sovellus. |

Perustelu Googlen määritelmän mukaan: laitteella käsiteltävät tiedot, joita ei lähetetä laitteelta pois, eivät ole "kerättyjä". Käyttäjän itse käynnistämä tekstiviesti hänen omaan lämmittimeensä on käyttäjän toiminto, ei kehittäjän tiedonkeruuta.

## 2. Tekstiviestilupien ilmoitus (Permissions Declaration Form)

**Sovelluksen sisältö → Sensitive app permissions → SMS and Call Log permissions**

- **Core functionality:** *Connected device companion app*
- **Permissions:** `SEND_SMS`, `RECEIVE_SMS`

**Kuvaus (englanniksi, lomakkeeseen):**

> HeatCall is a companion app for the Eberspächer EasyStart Call, a GSM remote control unit installed in a vehicle to operate its fuel-operated parking heater. The unit has no internet connectivity; its only remote interface is SMS. The app's core function is to send control commands to the unit (e.g. "pin:1234 heater:on,run:30") and to receive and display the unit's status replies (e.g. "Heater:on,mode:heater,run:30").
>
> SEND_SMS is needed to send commands to the user-configured unit number in the background when the user presses Start, Stop or Check status (also from the home screen widget). RECEIVE_SMS is needed to receive the unit's replies and show them in the app. Only messages from the user's configured unit numbers are processed; all other messages are ignored. The app has no internet permission, and no SMS data leaves the device.
>
> Alternatives do not work: the SMS Retriever and User Consent APIs are designed for one-time verification codes, and the unit's status replies do not fit that format. An SMS intent would require the user to send every command manually, which breaks the core remote-control function and the home screen widget.

**Video (tarvitaan yleensä, 30–90 s, julkaise YouTubessa "piilotettu"-tilassa):**

1. Avaa HeatCall, ja näytä auton kortti ja lämmittimen numero (Info ja asetukset → Laitteet).
2. Paina **Check status**. Näytä, että tekstiviesti lähtee taustalla.
3. Lämmittimen vastaus saapuu ja avautuu omaan ikkunaansa ("Heater on").
4. Paina **START** ja näytä vastaus.
5. Näytä kotinäytön widgetin Check status.
6. Näytä, että toisesta numerosta tullut viesti ei näy sovelluksessa.

## 3. Muut lomakkeet

| Lomake | Vastaus |
|---|---|
| Sovelluksen käyttöoikeus (App access) | Kaikki toiminnot ovat käytettävissä ilman kirjautumista. Huomautus arvioijalle: toimintojen testaamiseen tarvitaan EasyStart Call -yksikkö. Ilman sitä komento lähtee, mutta vastausta ei tule. |
| Mainokset | Ei mainoksia |
| Sisällön ikäluokitus | Työkalut / apuohjelma. Ei väkivaltaa, seksuaalisuutta, rahapelejä eikä käyttäjien välistä viestintää → kaikille (PEGI 3) |
| Kohdeyleisö | 18+ (ajoneuvon omistaja) |
| Uutissovellus | Ei |
| Terveys, rahoitus, VPN | Ei |
| Hallituksen sovellus | Ei |
| Tietosuojaseloste | URL, josta `docs/play/privacy.html` löytyy julkisena |

## 4. Ilmoitusten käyttöoikeus (NotificationListenerService)

Playssa erillistä lomaketta ei ole, mutta käyttö pitää kertoa selvästi. Sovelluksessa tämä on tehty: *Info ja asetukset → Vastausviestit* selittää ennen *Anna käyttöoikeus* -painiketta, että oikeutta käytetään vain lämmittimen vastausten merkitsemiseen luetuiksi. Sama asia mainitaan tietosuojaselosteessa ja kaupan kuvauksessa.
