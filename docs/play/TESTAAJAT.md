# Suljettu testaus Google Playssa / Closed testing on Google Play

Suomi + English

Uusi henkilökohtainen kehittäjätili (luotu 11/2026) vaatii ennen tuotantojulkaisua suljetun testauksen:

- vähintään **12 testaajaa**, jotka ovat liittyneet testiin ja pysyvät mukana **14 päivää yhtäjaksoisesti**
- sen jälkeen Play Consolessa haetaan pääsyä tuotantoon (lyhyt kysely testauksesta)

*English: A new personal developer account (created 11/2026) requires closed testing before a production release: at least **12 testers** who have opted in and stay in the test for **14 consecutive days**. After that, you apply for production access in Play Console (a short questionnaire about the test).*

## Valmistelu / Preparation

1. **Testaajalista:** kerää vähintään 12, mieluummin 14–15, Google-tilin sähköpostiosoitetta. Sama osoite, jolla testaaja käyttää Play Kauppaa puhelimessaan. Ylimääräiset testaajat varmistavat, että 12 täyttyy, vaikka joku jättäytyisi pois.
2. **Play Console → Testaus → Suljettu testaus → Testaajat:** luo sähköpostilista ja lisää osoitteet. Kopioi **liittymislinkki** (opt-in URL).
3. **Testaajat poistavat ensin sivuladatun HeatCallin.** Play-versio on allekirjoitettu eri avaimella, joten se ei asennu sivuladatun päälle.

*English: (1) Collect at least 12, preferably 14–15, Google account email addresses (the account the tester uses for Play Store on their phone); the extra testers make sure 12 is reached even if someone drops out. (2) In **Play Console → Testing → Closed testing → Testers**, create an email list, add the addresses and copy the **opt-in URL**. (3) Testers must first uninstall the sideloaded HeatCall: the Play version is signed with a different key, so it will not install over the sideloaded one.*

## Kutsuviesti testaajille / Invitation message for testers

Korvaa kohta `[LINKKI]` liittymislinkillä ennen lähettämistä. Sovellus on ilmainen, joten promokoodeja ei tarvita.

*English: Replace `[LINK]` with the opt-in URL before sending. The app is free, so no promo codes are needed.*

### Suomeksi / In Finnish

> Hei!
>
> HeatCall on tulossa Google Play -kauppaan. Google vaatii ennen julkaisua kahden viikon testijakson vähintään 12 testaajalla, ja toivoisin, että lähtisit mukaan.
>
> 1. Poista nykyinen HeatCall puhelimesta. Lämmittimen numero ja PIN syötetään uudelleen.
> 2. Avaa linkki ja valitse "Ryhdy testaajaksi": [LINKKI]
> 3. Asenna HeatCall Play Kaupasta ja pidä se puhelimessa vähintään 14 päivää.
>
> Palautetta voi lähettää osoitteeseen heatcall@konderi.fi. Kiitos!

### In English

> Hi!
>
> HeatCall is coming to Google Play. Before release, Google requires a two-week test with at least 12 testers, and I'd be grateful if you could join.
>
> 1. Uninstall the current HeatCall from your phone. You will need to enter the heater number and PIN again.
> 2. Open the link and choose "Become a tester": [LINK]
> 3. Install HeatCall from Play Store and keep it on your phone for at least 14 days.
>
> Feedback: heatcall@konderi.fi. Thank you!

## Aikataulu / Timeline

| Vaihe / Step | Kesto / Duration |
|---|---|
| Henkilöllisyyden vahvistus (Google) / Identity verification (Google) | muutamasta päivästä viikkoon / a few days to a week |
| Suljetun testauksen julkaisu ja SMS-lupailmoituksen tarkistus / Closed testing release and SMS declaration review | muutamasta päivästä 1–2 viikkoon / a few days to 1–2 weeks |
| Testijakso / Test period | 14 päivää / 14 days |
| Tuotantopääsyn hakemus ja tarkistus / Production access application and review | muutama päivä / a few days |
