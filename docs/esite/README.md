# HeatCall-esite / brochure

Suomi + English

| Tiedosto / File | Sisältö / Contents |
|---|---|
| `HeatCall-brochure-en-fi.pdf` | Kaksikielinen esite, A4, 4 sivua: englanti (s. 1–2), suomi (s. 3–4) / Bilingual brochure, A4, 4 pages: English (pp. 1–2), Finnish (pp. 3–4) |
| `HeatCall-esite.pdf` | Suomenkielinen esite, A4, 2 sivua / Finnish brochure, A4, 2 pages |
| `esite-en-fi.html`, `esite.html`, `img/` | Lähteet / Sources |

Renderöinti / Rendering (macOS, WebKit):

```bash
cd docs/esite
swift render_pdf.swift "$PWD/esite-en-fi.html" "$PWD/HeatCall-brochure-en-fi.pdf" 4
swift render_pdf.swift "$PWD/esite.html" "$PWD/HeatCall-esite.pdf" 2
```

Windows (Chrome headless), tuottaa saman A4-asettelun / produces the same A4 layout:

```bash
chrome --headless=new --no-pdf-header-footer --print-to-pdf=HeatCall-brochure-en-fi.pdf esite-en-fi.html
chrome --headless=new --no-pdf-header-footer --print-to-pdf=HeatCall-esite.pdf esite.html
```
