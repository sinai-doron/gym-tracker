# wger snapshot

A pinned copy of the exercise data from [wger](https://wger.de), the upstream our exercise
catalogue is built from. Why wger, and what was rejected: the plan doc, "Exercise data source:
recommendation". The snapshot is not shipped in the app as it is; the catalogue the app ships
takes the exercises we choose from it, with our own ids.

| File | What it holds |
|---|---|
| `wger-exercises.json` | All 904 exercises: category, primary and secondary muscles, equipment, variation group, the English and Hebrew text (name, description, aliases, notes), image and video links, and the licence and authors of each |
| `wger-reference.json` | wger's licences, muscles, equipment, categories and languages, by id |
| `wger-flags.json` | Exercises that can't be shipped as they are, and why |
| `fetch_wger.py` | Fetches all of the above again |

Retrieved 2026-11-02 from wger 2.8.0-dev.2 (the date and version are also in each file's `meta`).
Only English and Hebrew text is kept; wger has 33 languages.

## Refreshing

```sh
python3 -I catalogue/wger/fetch_wger.py
git diff --stat catalogue/wger
```

The files are sorted and indented, so the diff shows what wger changed. Read it before
committing: a changed licence or author matters as much as changed text.

## Licences: what we owe

wger's data is licensed entry by entry, not as a whole. Every exercise, text, image and video
has its own `license` (an id in `wger-reference.json`: CC BY-SA 3.0, CC BY-SA 4.0, CC BY 4.0 or
CC0) and its own authors (`license_author`, `author_history`). In this snapshot the exercises are
746 CC BY-SA 4.0, 132 CC BY-SA 3.0 and 26 CC0.

Anything we copy from here into the catalogue:

- keeps its wger id, licence and authors next to it, so the app can credit them;
- is credited in the app's Open-source licences screen ("Exercise data from wger.de, by its
  authors, under CC BY-SA"), before the catalogue ships;
- makes our catalogue file CC BY-SA 4.0 too, including our own changes and Hebrew translations of
  wger text. This covers the data only, not the app's code.

wger's code is AGPL-3.0; we use none of it, only the data.

## Flags

`wger-flags.json` lists why an exercise can't be used as it is:

- **Text copied from bodybuilding.com** (83, 112, 254, 310, 562, 580, 630, 655): wger labels it
  CC BY-SA, but the words are bodybuilding.com's. Leave the text out or write our own; the
  exercise itself (name, muscles, equipment) is fine.
- **A website named as the author** (202, 260, 580, 655, 688: exrx.net, realsimple.com,
  bodybuilding.com, carinatum.com): the text likely came from that site. Treat as above.
- **No author on the English text** (51 exercises): the licence can't be credited; write our own
  text, or take the exercise without its description.
- **No primary muscle** (144 exercises): fill in the muscles ourselves before using it.

Images are not used yet. If they are later: about a quarter have no author and some are marked
`is_ai_generated`; take only those with a named author.
