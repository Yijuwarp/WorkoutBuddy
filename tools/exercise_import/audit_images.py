#!/usr/bin/env python3
"""One-off audit: flag ic_ex_*.jpg files that look like line-art illustrations
rather than grayscale photos, based on histogram shape (illustrations are
near-bimodal: almost all pixels near pure black or pure white, few midtones)."""
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
DRAWABLE = os.path.normpath(os.path.join(HERE, "..", "..", "app", "src", "main", "res", "drawable"))

def midtone_fraction(path):
    im = Image.open(path).convert("L")
    hist = im.histogram()
    total = sum(hist)
    midtone = sum(hist[40:215])  # pixels that are neither near-black nor near-white
    return midtone / total

results = []
for fn in sorted(os.listdir(DRAWABLE)):
    if fn.startswith("ic_ex_") and fn.endswith(".jpg"):
        frac = midtone_fraction(os.path.join(DRAWABLE, fn))
        results.append((frac, fn))

results.sort()
print("Lowest midtone fraction (most likely line-art/illustration) first:\n")
for frac, fn in results:
    tag = "ILLUSTRATION?" if frac < 0.25 else ""
    print(f"  {frac:6.3f}  {fn:48s} {tag}")
