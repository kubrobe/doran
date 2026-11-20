#!/usr/bin/env python3
"""Builds app/src/main/assets/course/course.json and tools/audio_texts.json from tools/course.txt.

Run from the repository root:  python3 tools/build_course.py
Fails loudly on anything that would make an exercise unanswerable.
"""
import hashlib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "tools" / "course.txt"
OUT = ROOT / "app" / "src" / "main" / "assets" / "course" / "course.json"
AUDIO_LIST = ROOT / "tools" / "audio_texts.json"

HANGUL = re.compile(r"[가-힣]")
PUNCT_KO = re.compile(r"[.,?!~…·\"“”]")


def audio_key(text: str) -> str:
    return hashlib.sha1(text.strip().encode("utf-8")).hexdigest()[:12]


def ko_tiles(ko: str):
    return [t for t in (PUNCT_KO.sub("", w) for w in ko.split()) if t]


def en_tiles(en: str):
    out = []
    for w in en.split():
        w = re.sub(r"^[\"“(]+|[.,?!;:\"”)]+$", "", w)
        if w:
            out.append(w)
    return out


errors = []


def err(msg):
    errors.append(msg)


units = []
cur = None
for n, raw in enumerate(SRC.read_text(encoding="utf-8").splitlines(), 1):
    line = raw.strip()
    if not line or line.startswith("#"):
        continue
    kind, _, rest = line.partition(" ")
    rest = rest.strip()
    if kind == "@unit":
        parts = [p.strip() for p in rest.split("|")]
        if len(parts) != 4:
            err(f"line {n}: unit needs 4 fields")
            continue
        cur = {"id": parts[0], "title": parts[1], "section": parts[2], "blurb": parts[3],
               "tips": [], "letters": [], "words": [], "sentences": [], "fills": []}
        units.append(cur)
        continue
    if cur is None:
        err(f"line {n}: content before first @unit")
        continue
    uid = cur["id"]
    if kind == "t":
        cur["tips"].append(rest)
    elif kind == "l":
        parts = [p.strip() for p in rest.split("|")]
        if len(parts) < 4:
            err(f"line {n}: letter needs 4-5 fields")
            continue
        cur["letters"].append({"id": f"{uid}.l{len(cur['letters']) + 1}", "ch": parts[0], "rom": parts[1],
                               "say": parts[2], "hint": parts[3], "group": parts[4] if len(parts) > 4 else ""})
    elif kind == "w":
        ko, sep, en = rest.partition(" = ")
        if not sep:
            err(f"line {n}: word needs ' = '")
            continue
        cur["words"].append({"id": f"{uid}.w{len(cur['words']) + 1}", "ko": ko.strip(), "en": en.strip()})
    elif kind == "s":
        speak = True
        if "{nospeak}" in rest:
            speak = False
            rest = rest.replace("{nospeak}", "").strip()
        ko, sep, en = rest.partition(" = ")
        if not sep:
            err(f"line {n}: sentence needs ' = '")
            continue
        ens = [e.strip() for e in en.split("|")]
        cur["sentences"].append({"id": f"{uid}.s{len(cur['sentences']) + 1}", "ko": ko.strip(), "en": ens[0],
                                 "enAlt": [en_tiles(e) for e in ens[1:]],
                                 "koTiles": ko_tiles(ko), "enTiles": en_tiles(ens[0]), "speak": speak})
    elif kind == "f":
        body, sep, opts = rest.partition(" ; ")
        ko, sep2, en = body.partition(" = ")
        m = re.match(r"^(.*)\[(.+?)\](.*)$", ko.strip())
        if not (sep and sep2 and m):
            err(f"line {n}: fill needs 'before[answer]after = english ; a, b'")
            continue
        options = [o.strip() for o in opts.split(",") if o.strip()]
        answer = m.group(2)
        if answer in options or len(set(options)) != len(options) or len(options) < 2:
            err(f"line {n}: bad fill options")
        full = (m.group(1) + answer + m.group(3)).strip()
        if not full.endswith(("?", ".", "!")):
            full += "."
        cur["fills"].append({"id": f"{uid}.f{len(cur['fills']) + 1}", "before": m.group(1), "answer": answer,
                             "after": m.group(3), "en": en.strip(), "options": options, "full": full})
    else:
        err(f"line {n}: unknown line type {kind!r}")

# ---- validation ----
ids = set()
meaning = {}
for u in units:
    if u["id"] in ids:
        err(f"duplicate unit {u['id']}")
    ids.add(u["id"])
    is_hangul = bool(u["letters"])
    if is_hangul and len(u["letters"]) < 4:
        err(f"{u['id']}: need at least 4 letters")
    if not is_hangul and (len(u["words"]) < 6 or len(u["sentences"]) < 6):
        err(f"{u['id']}: need at least 6 words and 6 sentences")
    if len(u["words"]) < 3:
        err(f"{u['id']}: need at least 3 words")
    for w in u["words"]:
        if not HANGUL.search(w["ko"]):
            err(f"{w['id']}: no hangul")
        prev = meaning.get(w["ko"])
        if prev and prev != w["en"]:
            err(f"{w['id']}: {w['ko']} means {w['en']!r} here but {prev!r} elsewhere")
        meaning[w["ko"]] = w["en"]
    ens = [w["en"].lower() for w in u["words"]]
    if len(set(ens)) != len(ens):
        err(f"{u['id']}: two words share an English meaning")
    kos = [w["ko"] for w in u["words"]]
    if len(set(kos)) != len(kos):
        err(f"{u['id']}: duplicate Korean word")
    roms = [l["rom"] for l in u["letters"]]
    if len(set(roms)) != len(roms):
        err(f"{u['id']}: duplicate romanization")
    seen_en = set()
    for s in u["sentences"]:
        if len(s["koTiles"]) < 1 or len(s["enTiles"]) < 1:
            err(f"{s['id']}: empty tiles")
        key = " ".join(s["enTiles"]).lower()
        if key in seen_en:
            err(f"{s['id']}: duplicate English sentence in unit")
        seen_en.add(key)
        canon = sorted(t.lower() for t in s["enTiles"])
        for alt in s["enAlt"]:
            if sorted(t.lower() for t in alt) != canon:
                # An alternative that needs other tiles is still fine (never constructible), but warn.
                print(f"note: {s['id']} alternative {' '.join(alt)!r} uses different words", file=sys.stderr)

if errors:
    print("\n".join(errors), file=sys.stderr)
    sys.exit(1)

# ---- audio list ----
texts = {}


def need(text, slow):
    t = text.strip()
    k = audio_key(t)
    if k in texts:
        texts[k]["slow"] = texts[k]["slow"] or slow
    else:
        texts[k] = {"key": k, "text": t, "slow": slow}


for u in units:
    for l in u["letters"]:
        need(l["say"], False)
    for w in u["words"]:
        need(w["ko"], False)
    for s in u["sentences"]:
        need(s["ko"], True)
    for f in u["fills"]:
        need(f["full"], True)

course = {"version": 1, "units": units}
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(course, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
AUDIO_LIST.write_text(json.dumps(sorted(texts.values(), key=lambda x: x["key"]), ensure_ascii=False, indent=1) + "\n",
                      encoding="utf-8")
nw = sum(len(u["words"]) for u in units)
ns = sum(len(u["sentences"]) for u in units)
nl = sum(len(u["letters"]) for u in units)
print(f"{len(units)} units, {nl} letters, {nw} words, {ns} sentences, {len(texts)} audio clips")
