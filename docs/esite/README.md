# HeatCall-esite

- `HeatCall-esite.pdf` – kaksisivuinen A4-esite (suomi): esittely, toiminta, vaatimukset ja testaajahaku
- `esite.html` + `img/` – lähde. Muokkaa HTML:ää ja renderöi uudelleen:

```bash
cd docs/esite && swift render_pdf.swift "$PWD/esite.html" "$PWD/HeatCall-esite.pdf" 2
```
