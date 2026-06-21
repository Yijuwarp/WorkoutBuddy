#!/usr/bin/env python3
"""
wger_import.py - helper for adding new exercises to WorkoutBuddy.

This is the exact pipeline used to source the app's exercise thumbnails: it pulls
openly-licensed images from wger.de (https://wger.de/api/v2/), converts them to the
app's house style (200x200 grayscale baseline JPEG, ~6-9 KB), and drops them into
app/src/main/res/drawable/ with the ic_ex_<sanitized_name>.jpg naming the app resolves.

It does NOT touch DatabaseInitializer.kt or YouTube links - those are done by hand
(see docs/ADDING_EXERCISES.md). This script only handles the fetch + image conversion,
which is the tedious part.

Requires: Python 3 + Pillow (`pip install pillow`). No other dependencies.

Usage:
  # 1. Cache the wger catalog locally (run once; re-run to refresh):
  python wger_import.py fetch

  # 2. Find the wger exercise id + check an image exists, by keyword:
  python wger_import.py search "upright row"
  python wger_import.py search "good morning"

  # 3. Convert chosen images into the drawable folder. Pass id=AppName pairs
  #    (AppName must match the `name` you'll use in DatabaseInitializer.kt):
  python wger_import.py convert 694="Dumbbell Upright Row" 268="Good Mornings"

The English language id on wger is 2. Images are 200x200 thumbnails already; we only
recolor to grayscale and recompress to land in the existing size band.
"""
import sys, os, json, io, re, time, urllib.request

UA = {"User-Agent": "WorkoutBuddy/1.0 (exercise importer)"}
HERE = os.path.dirname(os.path.abspath(__file__))
CACHE_TRANS = os.path.join(HERE, "wger_trans.json")
CACHE_IMGS = os.path.join(HERE, "wger_imgs.json")
# drawable dir, relative to this script (tools/exercise_import -> app/.../drawable)
DRAWABLE = os.path.normpath(os.path.join(HERE, "..", "..", "app", "src", "main", "res", "drawable"))


def _get(url):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def _page_all(base):
    out, url = [], base
    while url:
        d = _get(url)
        out.extend(d["results"])
        url = d.get("next")
        time.sleep(0.2)
    return out


def cmd_fetch():
    print("Fetching English exercise translations (language=2)...")
    trans = _page_all("https://wger.de/api/v2/exercise-translation/?language=2&format=json&limit=500")
    print(f"  {len(trans)} translations")
    print("Fetching exercise images...")
    imgs = _page_all("https://wger.de/api/v2/exerciseimage/?format=json&limit=500")
    print(f"  {len(imgs)} images")
    json.dump(trans, open(CACHE_TRANS, "w"))
    json.dump(imgs, open(CACHE_IMGS, "w"))
    print(f"Cached to {CACHE_TRANS} and {CACHE_IMGS}")


def _load():
    if not (os.path.exists(CACHE_TRANS) and os.path.exists(CACHE_IMGS)):
        sys.exit("No cache found. Run `python wger_import.py fetch` first.")
    return json.load(open(CACHE_TRANS)), json.load(open(CACHE_IMGS))


def _img_by_ex(imgs):
    """base exercise id -> main thumbnail url (prefers is_main)."""
    out = {}
    for im in imgs:
        ex = im["exercise"]
        tn = im.get("thumbnails") or {}
        url = tn.get("small") or im.get("image")
        if url and (im.get("is_main") or ex not in out):
            out[ex] = url
    return out


def cmd_search(term):
    trans, imgs = _load()
    have_img = _img_by_ex(imgs)
    kws = term.lower().split()
    hits = []
    seen = set()
    for t in trans:
        ex = t["exercise"]
        name = t["name"]
        if ex in have_img and all(k in name.lower() for k in kws) and (ex, name) not in seen:
            seen.add((ex, name))
            hits.append((ex, name))
    if not hits:
        print(f"No image-backed matches for '{term}'.")
        return
    print(f"Matches for '{term}' (id -> wger name, all have images):")
    for ex, name in sorted(set(hits)):
        print(f"  {ex:5d}  {name}")


def resname(name):
    clean = re.sub(r"[^a-z0-9]", "_", name.lower())
    clean = re.sub(r"_+", "_", clean).strip("_")
    return f"ic_ex_{clean}.jpg"


def cmd_convert(pairs):
    from PIL import Image
    _, imgs = _load()
    have_img = _img_by_ex(imgs)
    for raw_pair in pairs:
        if "=" not in raw_pair:
            print(f"  SKIP (bad pair, want id=Name): {raw_pair}")
            continue
        ex_s, appname = raw_pair.split("=", 1)
        ex = int(ex_s)
        appname = appname.strip().strip('"')
        url = have_img.get(ex)
        if not url:
            print(f"  NO IMAGE for wger id {ex} ({appname})")
            continue
        data = urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=30).read()
        im = Image.open(io.BytesIO(data))
        if im.mode in ("RGBA", "LA", "P"):  # flatten transparency on white
            rgba = im.convert("RGBA")
            bg = Image.new("RGBA", im.size, (255, 255, 255, 255))
            bg.paste(rgba, mask=rgba.split()[-1])
            im = bg.convert("RGB")
        im = im.convert("L")  # grayscale = the app's house style
        if im.size != (200, 200):
            im = im.resize((200, 200), Image.LANCZOS)
        # tune quality to land in the existing ~6-9 KB band
        q = 80
        while q >= 40:
            buf = io.BytesIO()
            im.save(buf, "JPEG", quality=q, optimize=True)
            if buf.tell() <= 9000 or q == 40:
                break
            q -= 5
        path = os.path.join(DRAWABLE, resname(appname))
        open(path, "wb").write(buf.getvalue())
        print(f"  {resname(appname):44s} {buf.tell():5d}B q{q}")


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return
    cmd = sys.argv[1]
    if cmd == "fetch":
        cmd_fetch()
    elif cmd == "search":
        cmd_search(" ".join(sys.argv[2:]))
    elif cmd == "convert":
        cmd_convert(sys.argv[2:])
    else:
        print(__doc__)


if __name__ == "__main__":
    main()
