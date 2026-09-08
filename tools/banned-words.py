#!/usr/bin/env python3
"""Prove that no user-facing string uses a banned word.

The list is DESIGN.md section 6, including the words ADDENDUM-03 added. It reads the
text of every string resource and every string literal in the movement library, which
holds its copy in Kotlin until the translations land.

    tools/banned-words.py        prints every hit and exits 1 if there are any

XML comments are skipped: a comment is not copy. Resource names are skipped for the
same reason, which is why session_target is not a hit while "Aiming for" is the string
somebody actually reads.

Two cards are exempt, each for a written reason, and nothing else ever is.
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

WORDS = """
rung tier trail trend postcard story check-in streak score goal target calories burn
earn cheat fail should must senior elderly frail frailty sarcopenia diagnosis
prescribe assessment evaluation findings recommendations workout routine complete
level unlock achievement hazard risk decline patient report results test missed
""".split()

# Every exemption is one card, one reason, and no wildcards.
EXEMPT = {
    "card_no_calories_title": "the card is about there being no calories; CONTENT.md ships it as written",
    "card_no_calories_summary": "same card",
    "card_no_calories_body": "same card",
    "card_no_calories_source": "a citation, printed as published",
}

PATTERN = {word: re.compile(r"(?<![a-z-])" + re.escape(word) + r"(?![a-z-])", re.I) for word in WORDS}


def strings_from_xml(path):
    """Every piece of text a person can read in one resource file."""
    root = ET.parse(path).getroot()
    for element in root.iter():
        if element.tag not in ("string", "item"):
            continue
        name = element.get("name")
        text = "".join(element.itertext())
        yield name, text


def strings_from_kotlin(path):
    for number, line in enumerate(path.read_text().splitlines(), start=1):
        for literal in re.findall(r'"((?:[^"\\]|\\.)*)"', line):
            yield f"line {number}", literal


def main():
    files = sorted((ROOT / "app/src/main/res").rglob("values*/*.xml"))
    if not files:
        print("Refused: found no string resources to check.", file=sys.stderr)
        return 2

    hits = []
    for path in files:
        for name, text in strings_from_xml(path):
            for word, pattern in PATTERN.items():
                if pattern.search(text) and name not in EXEMPT:
                    hits.append((word, path, name, text))

    library = ROOT / "app/src/main/java/com/kamsiob/steadyhealth/session/Movements.kt"
    for where, text in strings_from_kotlin(library):
        for word, pattern in PATTERN.items():
            if pattern.search(text):
                hits.append((word, library, where, text))

    for word, path, where, text in hits:
        short = text if len(text) < 90 else text[:87] + "..."
        print(f"BANNED {word}: {path.relative_to(ROOT)} [{where}] {short}")

    if hits:
        print(f"\n{len(hits)} banned word(s) in user-facing copy.")
        return 1

    checked = len(files) + 1
    print(f"Checked {checked} files against {len(WORDS)} words. No banned word appears.")
    print(f"{len(EXEMPT)} exemptions, each with a reason in this script.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
