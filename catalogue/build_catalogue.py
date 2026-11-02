#!/usr/bin/env python3
"""
Build the app's exercise catalogue from our hand-written source and the wger snapshot.

    python3 -I catalogue/build_catalogue.py           # writes the catalogue
    python3 -I catalogue/build_catalogue.py --check   # fails if it is out of date or invalid (CI)

Reads exercises.src.json (our exercises) and vocabulary.json (presets, muscles, equipment), takes
wger's English instructions where an exercise asks for them and their licence allows it, and
writes app/src/main/assets/exercises/exercises.json with each exercise's provenance. Fails on
anything that would ship wrong: an unknown muscle or preset, a duplicate slug or legacy id, or
wger text that is copied from elsewhere or has no author to credit.

Standard library only.
"""
import html
import json
import re
import sys
from html.parser import HTMLParser
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
SOURCE = HERE / "exercises.src.json"
VOCABULARY = HERE / "vocabulary.json"
WGER = HERE / "wger"
OUTPUT = ROOT / "app/src/main/assets/exercises/exercises.json"

SLUG = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")
# wger licence ids we can ship text under: CC BY-SA 3.0, CC BY-SA 4.0, CC0, CC BY 4.0
SHIPPABLE_LICENCES = {1, 2, 3, 4}
# Shorter wger text is a stub ("." or a single phrase), not instructions
MIN_INSTRUCTIONS = 40
# Flags in wger-flags.json that rule out copying an exercise's English text
TEXT_BLOCKERS = ("bodybuilding.com", "website as its author", "has no author", "no English text")


class _Steps(HTMLParser):
    """wger's HTML description as plain steps: one per list item or paragraph."""

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.steps, self.current = [], []

    def flush(self):
        text = re.sub(r"\s+", " ", "".join(self.current)).strip()
        if text:
            self.steps.append(text)
        self.current = []

    def handle_starttag(self, tag, attrs):
        if tag in ("li", "p", "br", "ol", "ul"):
            self.flush()

    def handle_endtag(self, tag):
        if tag in ("li", "p", "ol", "ul"):
            self.flush()

    def handle_data(self, data):
        self.current.append(data)


def steps(description: str) -> list:
    parser = _Steps()
    parser.feed(html.unescape(description).replace(" ", " "))
    parser.close()
    parser.flush()
    return parser.steps


def authors(item: dict) -> list:
    named = [a for a in item["author_history"] if a]
    return named or ([item["license_author"]] if item["license_author"] else [])


def build() -> tuple:
    src = json.loads(SOURCE.read_text())
    vocab = json.loads(VOCABULARY.read_text())
    wger = {e["id"]: e for e in json.loads((WGER / "wger-exercises.json").read_text())["exercises"]}
    wger_meta = json.loads((WGER / "wger-exercises.json").read_text())["meta"]
    licences = {l["id"]: l for l in json.loads((WGER / "wger-reference.json").read_text())["licenses"]}
    flags = json.loads((WGER / "wger-flags.json").read_text())["flagged"]

    errors, out, slugs, legacy = [], [], set(), {}

    def err(slug, message):
        errors.append(f"{slug}: {message}")

    for x in src["exercises"]:
        s = x["slug"]
        if not SLUG.match(s):
            err(s, "slug must be lowercase words joined by hyphens")
        if s in slugs:
            err(s, "duplicate slug")
        slugs.add(s)
        for old in x.get("legacy", []):
            if old in legacy:
                err(s, f"legacy id {old} is already {legacy[old]}")
            legacy[old] = s
        for lang in ("en", "he"):
            if not x.get(lang, "").strip():
                err(s, f"no {lang} name")
        preset = vocab["presets"].get(x["preset"])
        if not preset:
            err(s, f"unknown preset {x['preset']}")
            continue
        if x["load"] not in preset["loads"]:
            err(s, f"load {x['load']} doesn't fit preset {x['preset']} (allowed: {', '.join(preset['loads'])})")
        if x["sides"] not in vocab["sides"]:
            err(s, f"unknown sides {x['sides']}")
        if x["mechanics"] not in vocab["mechanics"]:
            err(s, f"unknown mechanics {x['mechanics']}")
        if x["equipment"] not in vocab["equipment"]:
            err(s, f"unknown equipment {x['equipment']}")
        if not x["primary"]:
            err(s, "no primary muscle")
        for m in x["primary"] + x["secondary"]:
            if m not in vocab["muscles"]:
                err(s, f"unknown muscle {m}")
        if set(x["primary"]) & set(x["secondary"]):
            err(s, "a muscle is both primary and secondary")

        source, instructions = None, {}
        if x["wger"] is not None:
            e = wger.get(x["wger"])
            if not e:
                err(s, f"wger {x['wger']} is not in the snapshot")
                continue
            source = {
                "name": "wger",
                "url": f"https://wger.de/exercise/{e['id']}/view/",
                "exerciseId": e["id"],
                "exerciseUuid": e["uuid"],
                "exerciseLicense": licences[e["license"]]["short_name"],
                "exerciseAuthors": authors(e),
                "retrieved": wger_meta["retrieved_at"][:10],
            }
            if x["text"] == "wger":
                blockers = [f for f in flags.get(str(e["id"]), []) if any(b in f for b in TEXT_BLOCKERS)]
                english = [t for t in e["translations"] if t["language"] == "en"]
                if blockers:
                    err(s, f"wger {e['id']}'s text can't be shipped: {'; '.join(blockers)}")
                elif not english or len(" ".join(steps(english[0]["description"]))) < MIN_INSTRUCTIONS:
                    err(s, f"wger {e['id']} has no usable English instructions; set text to none")
                elif english[0]["license"] not in SHIPPABLE_LICENCES:
                    err(s, f"wger {e['id']}'s text licence {english[0]['license']} can't be shipped")
                else:
                    t = english[0]
                    instructions["en"] = steps(t["description"])
                    source.update({
                        "textId": t["id"],
                        "textUuid": t["uuid"],
                        "textLicense": licences[t["license"]]["short_name"],
                        "textAuthors": authors(t),
                    })
            elif x["text"] != "none":
                err(s, f"text must be wger or none, not {x['text']}")
        elif x["text"] != "none":
            err(s, "text can only come from wger when the exercise follows a wger entry")

        if x["preset"] == "time":
            defaults = vocab["defaults"]["time"]
        elif x["preset"] == "distance_load":
            defaults = vocab["defaults"]["distance_load"]
        elif x["preset"] in ("distance_time", "calories_time"):
            defaults = vocab["defaults"]["cardio"]
        else:
            defaults = vocab["defaults"][x["mechanics"]]

        out.append({
            "slug": s,
            "names": {"en": x["en"], "he": x["he"]},
            "aliases": {lang: x.get("aliases", {}).get(lang, []) for lang in ("en", "he")},
            "preset": x["preset"],
            "metrics": preset["metrics"],
            "record": {"metric": preset["record"], "better": preset["better"]},
            "load": x["load"],
            "sides": x["sides"],
            "equipment": x["equipment"],
            "mechanics": x["mechanics"],
            "muscles": {"primary": x["primary"], "secondary": x["secondary"]},
            "weightStepKg": x.get("weightStepKg", vocab["equipment"][x["equipment"]]["weightStepKg"]),
            "defaults": defaults,
            "instructions": instructions,
            "source": source,
            "legacy": x.get("legacy", []),
        })

    catalogue = {
        "version": vocab["catalogueVersion"],
        "license": "CC BY-SA 4.0 (https://creativecommons.org/licenses/by-sa/4.0/)",
        "attribution": "Exercise instructions and data from wger (https://wger.de), by the authors named in each "
                       "exercise's source, under the licence named there. Changes, Hebrew names and the rest of "
                       "this catalogue by the Gym Tracker authors.",
        "presets": {k: {kk: v[kk] for kk in ("en", "he", "metrics", "record", "better")} for k, v in vocab["presets"].items()},
        "muscles": vocab["muscles"],
        "equipment": vocab["equipment"],
        "exercises": out,
    }
    return catalogue, errors


def main() -> int:
    catalogue, errors = build()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        print(f"{len(errors)} problem(s); nothing written", file=sys.stderr)
        return 1
    text = json.dumps(catalogue, ensure_ascii=False, indent=1) + "\n"
    if "--check" in sys.argv:
        if not OUTPUT.exists() or OUTPUT.read_text() != text:
            print(f"{OUTPUT.relative_to(ROOT)} is out of date: run python3 -I catalogue/build_catalogue.py", file=sys.stderr)
            return 1
        print(f"catalogue up to date: {len(catalogue['exercises'])} exercises", file=sys.stderr)
        return 0
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(text)
    with_text = sum(1 for e in catalogue["exercises"] if e["instructions"])
    print(f"{len(catalogue['exercises'])} exercises ({with_text} with wger instructions) -> {OUTPUT.relative_to(ROOT)}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
