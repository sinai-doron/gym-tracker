#!/usr/bin/env python3
"""
Fetch a snapshot of wger's exercise data (https://wger.de) into this folder.

    python3 -I catalogue/wger/fetch_wger.py

Writes, sorted and indented so a refresh shows up as a readable diff:
  wger-exercises.json   every exercise, with its English and Hebrew text only
  wger-reference.json   wger's licences, muscles, equipment, categories and languages
  wger-flags.json       entries we must not ship as they are, and why

The snapshot is the pinned upstream for our own catalogue: it is not shipped in the app as is.
Every exercise, translation, image and video carries its own licence and authors; keep them
with anything copied from here (see README.md).

Downloads go through curl, which uses the system's certificates (python.org's Python on macOS
has none of its own). Standard library only otherwise.
"""
import json
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

API = "https://wger.de/api/v2"
HERE = Path(__file__).resolve().parent
# wger language ids
LANGUAGES = {2: "en", 21: "he"}
# Exercise text copied from bodybuilding.com reads like this; wger has a handful of them
COPIED_PHRASE = re.compile(r"this (will be|is) your starting position")
# English text found word for word in bodybuilding.com's instructions (checked 2026-11-02), by
# exercise id, for those the phrase alone doesn't catch
KNOWN_COPIED = {83, 112, 254, 310, 562, 630, 655}


def get(url: str):
    out = subprocess.run(
        ["curl", "-sfL", "--retry", "3", "-H", "Accept: application/json", "-A", "gym-tracker-catalogue-import", url],
        check=True, capture_output=True,
    ).stdout
    return json.loads(out)


def get_all(path: str) -> list:
    results, url = [], f"{API}/{path}/?limit=200"
    while url:
        page = get(url)
        results += page["results"]
        url = page["next"]
    return results


def licence_fields(item: dict) -> dict:
    """The licence and authorship wger records on an exercise, translation, image or video."""
    return {
        "license": item["license"]["id"] if isinstance(item["license"], dict) else item["license"],
        "license_title": item.get("license_title") or "",
        "license_author": item.get("license_author") or "",
        "license_author_url": item.get("license_author_url") or "",
        "license_object_url": item.get("license_object_url") or "",
        "license_derivative_source_url": item.get("license_derivative_source_url") or "",
        "author_history": sorted(item.get("author_history") or []),
    }


def muscle(m: dict) -> dict:
    return {"id": m["id"], "name": m["name"], "name_en": m["name_en"], "is_front": m["is_front"]}


def exercise(e: dict) -> dict:
    return {
        "id": e["id"],
        "uuid": e["uuid"],
        "created": e["created"],
        "last_update": e["last_update"],
        "category": e["category"],
        "muscles": [muscle(m) for m in e["muscles"]],
        "muscles_secondary": [muscle(m) for m in e["muscles_secondary"]],
        "equipment": e["equipment"],
        "variation_group": e["variation_group"],
        **licence_fields(e),
        "translations": sorted(
            (
                {
                    "id": t["id"],
                    "uuid": t["uuid"],
                    "language": LANGUAGES[t["language"]],
                    "name": t["name"],
                    "description": t["description"],
                    "aliases": sorted(a["alias"] for a in t.get("aliases") or []),
                    "notes": [n["comment"] for n in t.get("notes") or []],
                    "created": t["created"],
                    **licence_fields(t),
                }
                for t in e["translations"]
                if t["language"] in LANGUAGES
            ),
            key=lambda t: (t["language"], t["id"]),
        ),
        "images": sorted(
            (
                {
                    "id": i["id"],
                    "uuid": i["uuid"],
                    "image": i["image"],
                    "is_main": i["is_main"],
                    "is_ai_generated": i.get("is_ai_generated", False),
                    "style": i.get("style"),
                    **licence_fields(i),
                }
                for i in e["images"]
            ),
            key=lambda i: i["id"],
        ),
        "videos": sorted(
            ({"id": v["id"], "uuid": v["uuid"], "video": v["video"], "is_main": v["is_main"], **licence_fields(v)}
             for v in e["videos"]),
            key=lambda v: v["id"],
        ),
    }


def looks_like_url(text: str) -> bool:
    return bool(re.match(r"^\s*(https?://|www\.)", text))


def flags(ex: dict) -> list:
    """Why an exercise can't be shipped as it is; empty when nothing stands out."""
    out = []
    english = [t for t in ex["translations"] if t["language"] == "en"]
    if not english:
        out.append("no English text")
    if not ex["muscles"]:
        out.append("no primary muscle")
    for t in english:
        author = t["license_author"]
        if not author and not t["author_history"]:
            out.append(f"English text {t['id']} has no author")
        if looks_like_url(author):
            out.append(f"English text {t['id']} names a website as its author: {author}")
        if COPIED_PHRASE.search(re.sub(r"<[^>]+>", " ", t["description"]).lower()):
            out.append(f"English text {t['id']} reads like bodybuilding.com's (\"your starting position\")")
    if ex["id"] in KNOWN_COPIED:
        out.append("English text matches bodybuilding.com's word for word (checked 2026-11-02)")
    return out


def main() -> int:
    raw = get_all("exerciseinfo")
    exercises = sorted((exercise(e) for e in raw), key=lambda e: e["id"])
    reference = {
        name: sorted(get_all(path), key=lambda r: r["id"])
        for name, path in [
            ("licenses", "license"),
            ("muscles", "muscle"),
            ("equipment", "equipment"),
            ("categories", "exercisecategory"),
            ("languages", "language"),
        ]
    }
    try:
        version = get(f"{API}/version/")
    except subprocess.CalledProcessError:
        version = None

    meta = {
        "source": "wger (https://wger.de), exercise API v2",
        "wger_version": version,
        "retrieved_at": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "languages": sorted(LANGUAGES.values()),
        "exercises": len(exercises),
        "licence_note": "Each exercise, translation, image and video has its own licence and authors "
                        "(fields license*, author_history; license ids in wger-reference.json).",
    }
    flagged = {str(e["id"]): f for e in exercises if (f := flags(e))}

    def write(name: str, data) -> None:
        (HERE / name).write_text(json.dumps(data, ensure_ascii=False, indent=1, sort_keys=True) + "\n")

    write("wger-exercises.json", {"meta": meta, "exercises": exercises})
    write("wger-reference.json", {"meta": meta, **reference})
    write("wger-flags.json", {"meta": meta, "flagged": flagged})
    print(f"{len(exercises)} exercises, {len(flagged)} flagged, wger {version}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
