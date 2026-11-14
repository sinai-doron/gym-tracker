"""Makes the place badges and the outlines the app finds a run's place in.

    python3 -I scripts/make_place_badges.py <us-states.geojson> <ne_50m_admin_0_countries.geojson>

(needs Pillow). Run from the repository root.

Sources:
- art/badges/: the medal in the style of the app's other badges, by Grok Imagine from badge_5k
  (2026-11-14): badge_blank.jpg has an empty face; badge_usa/israel/uk.jpg carry a landmark.
- US states: https://raw.githubusercontent.com/PublicaMundi/MappingAPI/master/data/geojson/us-states.json
  (US Census outlines, public domain).
- Countries: https://raw.githubusercontent.com/nvkelso/natural-earth-vector/v5.1.2/geojson/ne_50m_admin_0_countries.geojson
  (Natural Earth, public domain).

Writes:
- app/src/main/res/drawable-nodpi/badge_place_<id>.webp, 512 px like the other badges: a
  country's landmark, or a state's outline raised in white on the empty medal. No text: the app
  writes the place's name on the ribbon, in its own language (ui/components/PlaceBadges.kt).
- app/src/main/assets/places/places.json: each place's id, names (en, he) and outline, simplified.
"""
import json
import math
import statistics
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "art/badges"
DRAWABLES = ROOT / "app/src/main/res/drawable-nodpi"
ASSET = ROOT / "app/src/main/assets/places/places.json"
SIZE = 512
# Outlines to about 300 m: plenty to tell which state a run is in, and a small file
SIMPLIFY_DEG = 0.003

# The state's outline on the 1024 px empty medal: the face above the ribbon
FACE_CENTRE = (517, 445)
FACE_BOX = (380, 330)
ICON = (238, 234, 246)

COUNTRIES = [
    # id, Natural Earth ADM0_A3, English, Hebrew
    ("usa", "USA", "USA", "ארה״ב"),
    ("israel", "ISR", "Israel", "ישראל"),
    ("uk", "GBR", "UK", "בריטניה"),
]

STATES = {
    "Alabama": ("al", "אלבמה"), "Alaska": ("ak", "אלסקה"), "Arizona": ("az", "אריזונה"),
    "Arkansas": ("ar", "ארקנסו"), "California": ("ca", "קליפורניה"), "Colorado": ("co", "קולורדו"),
    "Connecticut": ("ct", "קונטיקט"), "Delaware": ("de", "דלאוור"), "Florida": ("fl", "פלורידה"),
    "Georgia": ("ga", "ג׳ורג׳יה"), "Hawaii": ("hi", "הוואי"), "Idaho": ("id", "איידהו"),
    "Illinois": ("il", "אילינוי"), "Indiana": ("in", "אינדיאנה"), "Iowa": ("ia", "איובה"),
    "Kansas": ("ks", "קנזס"), "Kentucky": ("ky", "קנטקי"), "Louisiana": ("la", "לואיזיאנה"),
    "Maine": ("me", "מיין"), "Maryland": ("md", "מרילנד"), "Massachusetts": ("ma", "מסצ׳וסטס"),
    "Michigan": ("mi", "מישיגן"), "Minnesota": ("mn", "מינסוטה"), "Mississippi": ("ms", "מיסיסיפי"),
    "Missouri": ("mo", "מיזורי"), "Montana": ("mt", "מונטנה"), "Nebraska": ("ne", "נברסקה"),
    "Nevada": ("nv", "נבדה"), "New Hampshire": ("nh", "ניו המפשייר"), "New Jersey": ("nj", "ניו ג׳רזי"),
    "New Mexico": ("nm", "ניו מקסיקו"), "New York": ("ny", "ניו יורק"),
    "North Carolina": ("nc", "צפון קרוליינה"), "North Dakota": ("nd", "צפון דקוטה"), "Ohio": ("oh", "אוהיו"),
    "Oklahoma": ("ok", "אוקלהומה"), "Oregon": ("or", "אורגון"), "Pennsylvania": ("pa", "פנסילבניה"),
    "Rhode Island": ("ri", "רוד איילנד"), "South Carolina": ("sc", "דרום קרוליינה"),
    "South Dakota": ("sd", "דרום דקוטה"), "Tennessee": ("tn", "טנסי"), "Texas": ("tx", "טקסס"),
    "Utah": ("ut", "יוטה"), "Vermont": ("vt", "ורמונט"), "Virginia": ("va", "וירג׳יניה"),
    "Washington": ("wa", "וושינגטון"), "West Virginia": ("wv", "מערב וירג׳יניה"),
    "Wisconsin": ("wi", "ויסקונסין"), "Wyoming": ("wy", "ויומינג"),
}


def rings(geometry):
    polys = geometry["coordinates"] if geometry["type"] == "MultiPolygon" else [geometry["coordinates"]]
    return [poly[0] for poly in polys]


def unwrap(ring):
    """Alaska's Aleutians cross the date line: keep them west of it, so the outline stays whole."""
    return [[lon - 360 if lon > 0 else lon, lat] for lon, lat in ring]


def simplify(ring, tolerance):
    """Douglas-Peucker, in degrees."""
    if len(ring) < 4:
        return ring
    def dist(p, a, b):
        (x, y), (x1, y1), (x2, y2) = p, a, b
        dx, dy = x2 - x1, y2 - y1
        if dx == dy == 0:
            return math.hypot(x - x1, y - y1)
        t = max(0, min(1, ((x - x1) * dx + (y - y1) * dy) / (dx * dx + dy * dy)))
        return math.hypot(x - x1 - t * dx, y - y1 - t * dy)
    keep = [False] * len(ring)
    keep[0] = keep[-1] = True
    stack = [(0, len(ring) - 1)]
    while stack:
        i, j = stack.pop()
        best, k = 0, None
        for m in range(i + 1, j):
            d = dist(ring[m], ring[i], ring[j])
            if d > best:
                best, k = d, m
        if k is not None and best > tolerance:
            keep[k] = True
            stack += [(i, k), (k, j)]
    return [[round(x, 4), round(y, 4)] for p, (x, y) in zip(keep, ring) if p]


def state_badge(blank, outline):
    """The state's outline, raised in white on the empty medal, as the landmarks are."""
    lat0 = math.radians(statistics.mean(lat for r in outline for _, lat in r))
    pts = [[(lon * math.cos(lat0), -lat) for lon, lat in r] for r in outline]
    xs = [x for r in pts for x, _ in r]
    ys = [y for r in pts for _, y in r]
    w, h = max(xs) - min(xs), max(ys) - min(ys)
    s = min(FACE_BOX[0] / w, FACE_BOX[1] / h)
    ox = FACE_CENTRE[0] - w * s / 2 - min(xs) * s
    oy = FACE_CENTRE[1] - h * s / 2 - min(ys) * s
    up = 4  # drawn large, then shrunk: a smooth edge
    mask = Image.new("L", (blank.width * up, blank.height * up), 0)
    d = ImageDraw.Draw(mask)
    for r in pts:
        if len(r) > 2:
            d.polygon([((x * s + ox) * up, (y * s + oy) * up) for x, y in r], fill=255)
    mask = mask.resize(blank.size, Image.LANCZOS)
    img = blank.convert("RGBA")
    shadow = Image.new("RGBA", img.size, (40, 16, 92, 150))
    shadow.putalpha(mask.filter(ImageFilter.GaussianBlur(6)).point(lambda v: v * 150 // 255))
    img.alpha_composite(shadow, (5, 7))
    edge = Image.new("RGBA", img.size, (196, 188, 220, 255))
    edge.putalpha(mask)
    img.alpha_composite(edge, (0, 3))
    face = Image.new("RGBA", img.size, ICON + (255,))
    face.putalpha(mask)
    img.alpha_composite(face)
    return img.convert("RGB")


def save(img, place_id):
    path = DRAWABLES / f"badge_place_{place_id.replace('-', '_')}.webp"
    img.resize((SIZE, SIZE), Image.LANCZOS).save(path, "WEBP", quality=82, method=6)
    return path


def main() -> int:
    states_path, countries_path = sys.argv[1:3]
    states = {f["properties"]["name"]: f["geometry"] for f in json.load(open(states_path))["features"]}
    countries = {f["properties"]["ADM0_A3"]: f["geometry"] for f in json.load(open(countries_path))["features"]}
    places, total = [], 0

    for place_id, a3, en, he in COUNTRIES:
        outline = [simplify(r, SIMPLIFY_DEG) for r in rings(countries[a3])]
        places.append({"id": place_id, "kind": "country", "names": {"en": en, "he": he}, "outline": outline})
        total += save(Image.open(ART / f"badge_{place_id}.jpg").convert("RGB"), place_id).stat().st_size

    blank = Image.open(ART / "badge_blank.jpg").convert("RGB")
    for name, (code, he) in sorted(STATES.items()):
        raw = [unwrap(r) if code == "ak" else r for r in rings(states[name])]
        place_id = f"us-{code}"
        places.append({
            "id": place_id, "kind": "state", "country": "usa", "names": {"en": name, "he": he},
            "outline": [simplify(r, SIMPLIFY_DEG) for r in raw],
        })
        total += save(state_badge(blank, raw), place_id).stat().st_size

    ASSET.parent.mkdir(parents=True, exist_ok=True)
    ASSET.write_text(json.dumps({"version": 1, "places": places}, ensure_ascii=False, separators=(",", ":")) + "\n")
    print(f"{len(places)} places; badges {total // 1024} KB; outlines {ASSET.stat().st_size // 1024} KB", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
