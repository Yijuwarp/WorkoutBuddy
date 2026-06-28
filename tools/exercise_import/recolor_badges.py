#!/usr/bin/env python3
"""One-off: the rank badge PNGs (drawable-nodpi/badge_*.png) were generated with an opaque
white background, which reads as a bright box against the app's new dark theme - including
white pockets between artwork details (wings, crown gaps) that aren't connected to the image
border, so a corner flood-fill alone misses them. Since the artwork itself has no intentional
white content, this does a global near-white -> dark-surface-color replace, blending
proportionally near the artwork edges (by "whiteness") so anti-aliased boundaries don't leave
a hard ring.
"""
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
DRAWABLE_NODPI = os.path.normpath(os.path.join(HERE, "..", "..", "app", "src", "main", "res", "drawable-nodpi"))
TARGET = (0x16, 0x1B, 0x26)
WHITE_FLOOR = 225   # channel value below which a pixel is considered "not background" at all
WHITE_CEIL = 250    # channel value above which a pixel is fully background

def whiteness(r, g, b):
    m = min(r, g, b)
    if m >= WHITE_CEIL:
        return 1.0
    if m <= WHITE_FLOOR:
        return 0.0
    return (m - WHITE_FLOOR) / (WHITE_CEIL - WHITE_FLOOR)

badges = [f for f in os.listdir(DRAWABLE_NODPI) if f.startswith("badge_") and f.endswith(".png")]
for fn in sorted(badges):
    path = os.path.join(DRAWABLE_NODPI, fn)
    im = Image.open(path).convert("RGB")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b = px[x, y]
            a = whiteness(r, g, b)
            if a > 0:
                nr = round(r * (1 - a) + TARGET[0] * a)
                ng = round(g * (1 - a) + TARGET[1] * a)
                nb = round(b * (1 - a) + TARGET[2] * a)
                px[x, y] = (nr, ng, nb)
    im.save(path)
    print(f"  {fn}")
