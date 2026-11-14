# Suljettu testaus Google Playssa

Uusi henkilökohtainen kehittäjätili (luotu 11/2026) vaatii ennen tuotantojulkaisua suljetun testauksen:

- vähintään **12 testaajaa**, jotka ovat liittyneet testiin ja pysyvät mukana **14 päivää yhtäjaksoisesti**
- sen jälkeen Play Consolessa haetaan pääsyä tuotantoon (lyhyt kysely testauksesta)

## Valmistelu

1. **Testaajalista:** kerää vähintään 12, mieluummin 14–15, Google-tilin sähköpostiosoitetta. Sama osoite, jolla testaaja käyttää Play Kauppaa puhelimessaan. Ylimääräiset testaajat varmistavat, että 12 täyttyy, vaikka joku jättäytyisi pois.
2. **Play Console → Testaus → Suljettu testaus → Testaajat:** luo sähköpostilista ja lisää osoitteet. Kopioi **liittymislinkki** (opt-in URL).
3. **Promokoodit** (maksullinen sovellus): *Kaupallistaminen → Promokoodit*, yksi koodi kullekin testaajalle.
4. **Testaajat poistavat ensin sivuladatun HeatCallin.** Play-versio on allekirjoitettu eri avaimella, joten se ei asennu sivuladatun päälle.

## Kutsuviesti testaajille

Muokkaa kohdat `[LINKKI]` ja `[KOODI]` ennen lähettämistä.

### Suomeksi

> Hei!
>
> HeatCall on tulossa Google Play -kauppaan. Google vaatii ennen julkaisua kahden viikon testijakson vähintään 12 testaajalla, ja toivoisin, että lähtisit mukaan.
>
> 1. Poista nykyinen HeatCall puhelimesta. Lämmittimen numero ja PIN syötetään uudelleen.
> 2. Avaa linkki ja valitse "Ryhdy testaajaksi": [LINKKI]
> 3. Lunasta promokoodi Play Kaupassa (Profiili → Maksut ja tilaukset → Lunasta koodi): [KOODI]
> 4. Asenna HeatCall Play Kaupasta ja pidä se puhelimessa vähintään 14 päivää.
>
> Palautetta voi lähettää osoitteeseen heatcall@konderi.fi. Kiitos!

### In English

> Hi!
>
> HeatCall is coming to Google Play. Before release, Google requires a two-week test with at least 12 testers, and I'd be grateful if you could join.
>
> 1. Uninstall the current HeatCall from your phone. You will need to enter the heater number and PIN again.
> 2. Open the link and choose "Become a tester": [LINK]
> 3. Redeem the promo code in Play Store (Profile → Payments & subscriptions → Redeem code): [CODE]
> 4. Install HeatCall from Play Store and keep it on your phone for at least 14 days.
>
> Feedback: heatcall@konderi.fi. Thank you!

## Aikataulu

| Vaihe | Kesto |
|---|---|
| Henkilöllisyyden vahvistus (Google) | muutamasta päivästä viikkoon |
| Suljetun testauksen julkaisu ja SMS-lupailmoituksen tarkistus | muutamasta päivästä 1–2 viikkoon |
| Testijakso | 14 päivää |
| Tuotantopääsyn hakemus ja tarkistus | muutama päivä |
