# Exercise catalogue

The built-in exercises the app ships, written here and built into
`app/src/main/assets/exercises/exercises.json`. The model behind it is
[ADR-0006](../docs/adr/0006-exercise-model.md); the plan doc's "The exercise database" says how the
app seeds it (new exercises inserted, built-ins updated, a user's own exercises never touched).

| File | What it is |
|---|---|
| `exercises.src.json` | Our exercises, one per line, written by hand |
| `vocabulary.json` | Presets and load conventions, the 18 muscles (with body-map regions), equipment and its weight step, default sets |
| `build_catalogue.py` | Checks the source and writes the app's catalogue |
| `wger/` | The pinned wger snapshot we take instructions from ([its README](wger/README.md)) |

## Adding or changing an exercise

1. Add or edit its line in `exercises.src.json`:
   - `slug`: lowercase words joined by hyphens; never changed once shipped, since history hangs on it.
   - `en`, `he`: the names shown. English is the main language.
   - `preset`: what a set records (`reps_load`, `reps`, `reps_assisted`, `time`, `distance_load`,
     `distance_time`, `calories_time`); `load`: how the weight is meant (`total`, `per_hand`,
     `added`, `assistance`, `none`); `sides`: `both` or `per_side`.
   - `equipment`, `mechanics`, and `primary` / `secondary` muscles from `vocabulary.json`. Write
     the muscles yourself: wger's are a starting point and are sometimes wrong (it lists lats for
     a cable lateral raise).
   - `wger`: the wger exercise it follows, or `null`. `text`: `wger` copies its English
     instructions; `none` copies nothing. Use `none` when wger's text describes a different
     variation (seated for standing, a band for a cable) or isn't instructions.
   - `legacy`: the owner's old ids (`d1_e1`…) for exercises their history already uses.
2. `python3 -I catalogue/build_catalogue.py`, and read what it changed in the output.
3. Bump `catalogueVersion` in `vocabulary.json` when the change should reach installed apps.

The build refuses to write anything when a rule breaks: an unknown muscle, equipment or preset, a
load that doesn't fit the preset, a duplicate slug or legacy id, or wger text that is copied from
another site, has no author, or is a stub. CI runs `build_catalogue.py --check`, which also fails
when the output wasn't rebuilt after a change.

## Licence

The catalogue is CC BY-SA 4.0, because the instructions come from wger under CC BY-SA. Each
exercise's `source` names the wger exercise and text it follows, with their licence and authors;
the app's Open-source licences screen (Settings → About) lists them, read from the built catalogue. The Hebrew
names, muscles and everything else not from wger are ours.

## Before release

- Read the instructions: wger's text passes the licence rules but varies in quality (the bench
  press entry has a dubious line about grip width and "inner chest").
- Hebrew names for the 158 exercises beyond the owner's 34 are a first draft.
- 21 exercises have no instructions yet (`text: none`).
