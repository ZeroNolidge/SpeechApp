#!/usr/bin/env python3
"""Builds the web version's emoji font: Noto Color Emoji trimmed to the emoji the app uses.

Browsers running Compose draw text with their own fonts, which have no emoji, so the web
build ships a small emoji font. CI runs this before every web build so new emoji are covered.

Usage: tools/make_emoji_font.py <full NotoColorEmoji.ttf> <output.ttf>
Needs: pip install fonttools
"""
import glob
import subprocess
import sys
import tempfile

src, out = sys.argv[1], sys.argv[2]
codepoints = set()
for path in glob.glob("app/src/**/*.kt", recursive=True):
    for ch in open(path, encoding="utf-8").read():
        o = ord(ch)
        # Symbols and emoji, skipping ordinary punctuation like dashes and curly quotes.
        if o >= 0x2000 and not (0x2010 <= o <= 0x206F):
            codepoints.add(o)
# Joiners, variation selectors and keycap pieces used inside emoji sequences.
codepoints |= {0xFE0F, 0x200D, 0x20E3} | set(range(0x30, 0x3A))

with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False) as f:
    f.write("\n".join("U+%04X" % c for c in sorted(codepoints)))
    listing = f.name

subprocess.run(
    ["pyftsubset", src, f"--unicodes-file={listing}", f"--output-file={out}", "--layout-features=*", "--no-hinting"],
    check=True,
)
print(f"{len(codepoints)} codepoints -> {out}")
