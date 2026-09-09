#!/usr/bin/env python3
"""Build the clinician handout, A4 and US Letter, from store-assets/clinician-handout.md.

ADDENDUM-03 Part 19 asks for one printable page in two sizes. The two files are
generated from the one document so that the copy cannot drift between them, which
is the only reason this script exists rather than two hand-written files.

Output is HTML with a print stylesheet rather than a PDF. Every machine has a
browser that prints to PDF, no machine reliably has a PDF toolchain, and an HTML
page can be proof-read in the same browser it prints from. Black ink only, no grey
fills, nothing under 9 point, because it has to survive a low toner office laser.

The QR code is left as a marked box. It cannot be generated until the Play listing
exists, which is on the BLOCKED list, and a QR code pointing at a URL that does not
resolve yet is worse than an empty box that says what goes in it.
"""

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
SOURCE = ROOT / "store-assets" / "clinician-handout.md"

SIZES = {
    "a4": ("A4", "20mm"),
    "letter": ("Letter", "0.75in"),
}

CSS = """
@page {{ size: {size}; margin: {margin}; }}
* {{ box-sizing: border-box; }}
body {{
  font-family: Georgia, "Times New Roman", serif;
  font-size: 11pt;
  line-height: 1.45;
  color: #000;
  background: #fff;
  margin: 0;
}}
h1 {{ font-size: 20pt; line-height: 1.2; margin: 0 0 14pt; }}
p {{ margin: 0 0 10pt; }}
.strip {{
  font-size: 10pt;
  border-top: 1.5pt solid #000;
  padding-top: 8pt;
  margin-top: 16pt;
  display: flex;
  gap: 10mm;
  align-items: flex-start;
}}
.qr {{
  width: 25mm; height: 25mm; flex: 0 0 25mm;
  border: 1pt solid #000;
  display: flex; align-items: center; justify-content: center;
  text-align: center; font-size: 7pt; line-height: 1.2;
}}
.start {{ margin-top: 14pt; }}
.start b {{ font-size: 12pt; }}
.rule {{ border-bottom: 1pt solid #000; height: 26pt; margin-top: 10pt; }}
"""


def between(text, start, end):
    body = text.split(start, 1)[1]
    return body.split(end, 1)[0].strip()


def paragraphs(text):
    return [p.strip() for p in re.split(r"\n\s*\n", text) if p.strip()]


def build(size_key):
    text = SOURCE.read_text()
    heading = between(text, "### Heading\n", "### The four lines")
    heading = heading.strip().strip("*")
    lines = paragraphs(between(text, "### The four lines\n", "### The strip"))
    strip = paragraphs(between(text, "### The strip along the bottom\n", "### The three ruled"))
    strip = [s for s in strip if not s.startswith("[")]

    size, margin = SIZES[size_key]
    parts = [
        "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">",
        "<title>Steady Health, one page to hand over</title>",
        f"<style>{CSS.format(size=size, margin=margin)}</style>",
        "</head><body>",
        f"<h1>{heading}</h1>",
    ]
    parts += [f"<p>{line}</p>" for line in lines]
    parts += [
        '<div class="strip">',
        '<div class="qr">QR code<br>25 mm<br>see BLOCKED</div>',
        "<div>" + "<br>".join(strip) + "</div>",
        "</div>",
        '<div class="start"><b>Start with:</b>',
        '<div class="rule"></div><div class="rule"></div><div class="rule"></div>',
        "</div>",
        "</body></html>",
    ]
    return "\n".join(parts) + "\n"


def main():
    if not SOURCE.exists():
        print(f"missing {SOURCE}", file=sys.stderr)
        return 1
    for key in SIZES:
        out = ROOT / "store-assets" / f"clinician-handout-{key}.html"
        out.write_text(build(key))
        print(f"wrote {out.relative_to(ROOT)}")
    print("Print to PDF from a browser. Check the ruled lines survive draft quality.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
