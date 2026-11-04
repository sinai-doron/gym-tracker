"""Makes the launcher icons and the Play Store icon from art/neonsheep-fitness-icon-source.jpg.

The source is the neon sheep lifting a barbell on a dark disc (generated with Grok from the
neonsheep logo, 2026-11-04). Run from the repository root: python3 -I scripts/make_app_icon.py
(needs Pillow).

- Adaptive icon (every launcher since Android 8): the disc goes on the foreground layer of the
  108-unit canvas at 70 units across. The farthest point of the drawing (a weight plate's
  corner, 663 of the source's 1408 px from the centre) then sits 33 units out: the edge of the
  66-unit circle every launcher mask keeps. The background layer is the disc's own edge colour,
  so no mask shape shows a seam.
- Legacy and round icons: the disc alone, on transparency.
- Play Store: 512 x 512, the disc on its edge colour (Play rounds the corners itself).
"""
from pathlib import Path

from PIL import Image, ImageDraw

SOURCE = Path("art/neonsheep-fitness-icon-source.jpg")
RES = Path("app/src/main/res")
EDGE = (0x11, 0x14, 0x1B)
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
DISC_UNITS = 70  # of the adaptive canvas's 108


def disc(size: int) -> Image.Image:
    """The source's disc at size x size, transparent outside it (anti-aliased edge)."""
    big = 4 * size
    src = Image.open(SOURCE).convert("RGB").resize((big, big), Image.LANCZOS)
    mask = Image.new("L", (big, big), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, big - 1, big - 1), fill=255)
    out = src.convert("RGBA")
    out.putalpha(mask)
    return out.resize((size, size), Image.LANCZOS)


def main() -> None:
    for name, scale in DENSITIES.items():
        folder = RES / f"mipmap-{name}"
        canvas = round(108 * scale)
        fg = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
        d = disc(round(DISC_UNITS * scale))
        fg.paste(d, ((canvas - d.width) // 2, (canvas - d.height) // 2), d)
        fg.save(folder / "ic_launcher_foreground.png", optimize=True)
        legacy = disc(round(48 * scale))
        legacy.save(folder / "ic_launcher.png", optimize=True)
        legacy.save(folder / "ic_launcher_round.png", optimize=True)

    store = Image.new("RGBA", (512, 512), EDGE + (255,))
    d = disc(512)
    store.paste(d, (0, 0), d)
    store.convert("RGB").save("app/src/main/ic_launcher-playstore.png", optimize=True)


if __name__ == "__main__":
    main()
