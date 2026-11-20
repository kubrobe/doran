import sys, json, subprocess, wave, os
from vosk import Model, KaldiRecognizer, SetLogLevel
SetLogLevel(-1)
model = Model(sys.argv[1]); d = sys.argv[2]
lines = [l.strip() for l in open(sys.argv[3], encoding='utf-8') if l.strip()]
def pcm(path):
    return subprocess.run(["ffmpeg","-loglevel","quiet","-i",path,"-ar","16000","-ac","1","-f","s16le","-"],capture_output=True).stdout
for i, line in enumerate(lines, 1):
    for p in ("e","g"):
        f = f"{d}/{p}_{i}.mp3"
        if not os.path.exists(f) or os.path.getsize(f) == 0: print(p, i, "MISSING"); continue
        data = pcm(f)
        r = KaldiRecognizer(model, 16000); r.SetWords(True)
        r.AcceptWaveform(data); res = json.loads(r.FinalResult())
        print(p, i, repr(line), "->", repr(res.get("text")), [round(w["conf"],2) for w in res.get("result",[])])
