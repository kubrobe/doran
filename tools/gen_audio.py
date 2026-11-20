#!/usr/bin/env python3
"""Generates the bundled Korean audio for every entry in tools/audio_texts.json.

Normal speed: <key>.mp3   Slow: <key>_s.mp3   (in app/src/main/assets/audio/)
Each clip is checked with the app's own offline recognizer. If a clip is not recognized
well, it is regenerated with the other voice and the better take is kept.
Writes tools/audio_report.tsv.
"""
import asyncio, json, subprocess, sys, unicodedata, wave
from pathlib import Path

import edge_tts
import numpy as np
import sherpa_onnx

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app/src/main/assets/audio"
ASR = ROOT / "app/src/main/assets/asr"
VOICES = ["ko-KR-SunHiNeural", "ko-KR-InJoonNeural"]
NORMAL, SLOW = "-5%", "-35%"

def hangul_only(s):
    return "".join(c for c in s if "가" <= c <= "힣")

def jamo(s):
    return [c for c in unicodedata.normalize("NFD", hangul_only(s))]

def lev(a, b):
    prev = list(range(len(b) + 1))
    for i, x in enumerate(a, 1):
        cur = [i]
        for j, y in enumerate(b, 1):
            cur.append(min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (x != y)))
        prev = cur
    return prev[-1]

def similarity(target, heard):
    t, h = jamo(target), jamo(heard)
    if not t:
        return 1.0
    return max(0.0, 1 - lev(t, h) / max(len(t), len(h)))

rec = sherpa_onnx.OfflineRecognizer.from_transducer(
    encoder=str(ASR / "encoder.int8.onnx"), decoder=str(ASR / "decoder.int8.onnx"),
    joiner=str(ASR / "joiner.int8.onnx"), tokens=str(ASR / "tokens.txt"), num_threads=4,
    decoding_method="greedy_search")

def recognize(mp3):
    pcm = subprocess.run(["ffmpeg", "-loglevel", "quiet", "-i", str(mp3), "-ar", "16000", "-ac", "1", "-f", "s16le", "-"],
                         capture_output=True, check=True).stdout
    a = np.frombuffer(pcm, dtype=np.int16).astype(np.float32) / 32768
    # Pad with silence like the app does, so very short clips decode.
    a = np.concatenate([np.zeros(4000, np.float32), a, np.zeros(8000, np.float32)])
    s = rec.create_stream(); s.accept_waveform(16000, a); rec.decode_stream(s)
    return s.result.text, len(a) / 16000

async def tts(text, voice, rate, path):
    for attempt in range(5):
        try:
            await edge_tts.Communicate(text, voice, rate=rate).save(str(path))
            if path.stat().st_size > 1000:
                return
        except Exception as e:  # network hiccups
            print("retry", text, e, file=sys.stderr)
        await asyncio.sleep(2 + attempt * 3)
    raise RuntimeError(f"TTS failed for {text!r}")

def default_voice(item):
    # Single letters and words: one consistent voice. Sentences: alternate two voices like two characters.
    return 1 if (item["slow"] and int(item["key"][:2], 16) % 3 == 0) else 0

async def make(item, sem, report):
    async with sem:
        key, text = item["key"], item["text"]
        for suffix, rate, wanted in (("", NORMAL, True), ("_s", SLOW, item["slow"])):
            if not wanted:
                continue
            path = OUT / f"{key}{suffix}.mp3"
            if path.exists() and path.stat().st_size > 1000:
                heard, dur = recognize(path)
                report.append((key + suffix, text, heard, round(similarity(text, heard), 2), round(dur, 1), "kept"))
                continue
            v = default_voice(item)
            await tts(text, VOICES[v], rate, path)
            heard, dur = recognize(path)
            sim = similarity(text, heard)
            if sim < 0.75 and len(hangul_only(text)) > 1:
                alt = path.with_suffix(".alt.mp3")
                await tts(text, VOICES[1 - v], rate, alt)
                heard2, dur2 = recognize(alt)
                sim2 = similarity(text, heard2)
                if sim2 > sim:
                    alt.replace(path); heard, dur, sim, v = heard2, dur2, sim2, 1 - v
                else:
                    alt.unlink()
            report.append((key + suffix, text, heard, round(sim, 2), round(dur, 1), VOICES[v]))

async def main():
    items = json.loads((ROOT / "tools/audio_texts.json").read_text(encoding="utf-8"))
    OUT.mkdir(parents=True, exist_ok=True)
    wanted = {f"{i['key']}.mp3" for i in items} | {f"{i['key']}_s.mp3" for i in items if i["slow"]}
    for f in OUT.glob("*.mp3"):
        if f.name not in wanted:
            f.unlink()
    report = []
    sem = asyncio.Semaphore(6)
    await asyncio.gather(*(make(i, sem, report) for i in items))
    report.sort()
    with open(ROOT / "tools/audio_report.tsv", "w", encoding="utf-8") as fh:
        fh.write("file\ttext\trecognized\tsimilarity\tseconds\tvoice\n")
        for r in report:
            fh.write("\t".join(map(str, r)) + "\n")
    sims = [r[3] for r in report]
    print(f"{len(report)} clips, mean similarity {sum(sims)/len(sims):.3f}, below 0.6: {sum(s < 0.6 for s in sims)}")
    missing = [w for w in wanted if not (OUT / w).exists()]
    if missing:
        sys.exit(f"missing audio: {missing[:5]}")

asyncio.run(main())
