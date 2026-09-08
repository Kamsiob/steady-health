#!/usr/bin/env python3
"""Sort a Kotlin file's imports and drop the ones nothing uses.

ktlint's order is lexicographic with java, javax and kotlin last, in that order, and
kotlinx is not kotlin. Getting that wrong by hand twice is why this exists.

An import is dropped when its simple name never appears in the body as a whole word.
Aliased imports are always kept, because the alias is the name and the check would be
about the wrong word.

    tools/tidy-imports.py FILE [FILE...]
"""
import re
import sys
from pathlib import Path


def rank(line):
    name = line[len("import "):]
    if name.startswith("java."):
        return 1
    if name.startswith("javax."):
        return 2
    if name.startswith("kotlin."):
        return 3
    return 0


# Operators and delegate providers are used by syntax rather than by name, so the
# body never mentions them. Dropping androidx.compose.runtime.getValue is how a whole
# file of "by remember" stops compiling.
BY_SYNTAX = {
    "getValue", "setValue", "provideDelegate", "invoke", "iterator",
    "plus", "minus", "times", "div", "rem", "unaryMinus", "unaryPlus",
    "inc", "dec", "not", "compareTo", "contains", "rangeTo", "get", "set",
}


def tidy(path):
    lines = Path(path).read_text().split("\n")
    numbers = [i for i, line in enumerate(lines) if line.startswith("import ")]
    if not numbers:
        return False
    first, last = numbers[0], numbers[-1]
    body = "\n".join(lines[:first] + lines[last + 1:])

    kept = []
    for line in lines[first:last + 1]:
        if not line.startswith("import "):
            continue
        name = line[len("import "):].strip()
        if " as " in name:
            kept.append(line)
            continue
        simple = name.split(".")[-1]
        if simple == "*" or simple in BY_SYNTAX or re.search(r"(?<![A-Za-z0-9_])" + re.escape(simple) + r"(?![A-Za-z0-9_])", body):
            kept.append(line)

    kept.sort(key=lambda line: (rank(line), line))
    after = lines[:first] + kept + lines[last + 1:]
    if after == lines:
        return False
    Path(path).write_text("\n".join(after))
    return True


if __name__ == "__main__":
    changed = [p for p in sys.argv[1:] if tidy(p)]
    for p in changed:
        print(f"tidied {p}")
